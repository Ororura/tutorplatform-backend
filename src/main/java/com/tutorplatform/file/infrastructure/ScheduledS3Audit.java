package com.tutorplatform.file.infrastructure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Read-only comparison of S3 materials/ objects against file_assets in THIS environment.
 *
 * <p>No S3 DELETE, DB UPDATE, or DB DELETE is ever issued here. A recent unregistered object may be
 * an upload in progress; only older objects are flagged as eligible for investigation. The audit
 * deliberately scans only the application files bucket, not restic backups.
 */
@Component
@ConditionalOnProperty(prefix = "app.file-storage", name = "provider", havingValue = "S3")
@ConditionalOnProperty(
        prefix = "app.file-storage.s3-reconciliation",
        name = "enabled",
        havingValue = "true")
public class ScheduledS3Audit {
    private static final Logger log = LoggerFactory.getLogger(ScheduledS3Audit.class);
    private static final String KEY_PATTERN =
            "materials/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final String PREFIX = "materials/";
    private static final int PAGE_SIZE = 1000;

    private final S3Client s3;
    private final NamedParameterJdbcTemplate jdbc;
    private final String bucket;
    private final long graceSeconds;
    private final int maxPages;
    private final int maxLogEntries;
    private final Clock clock;
    private final AtomicLong scanned = new AtomicLong();
    private final AtomicLong unregistered = new AtomicLong();
    private final AtomicLong eligible = new AtomicLong();
    private final AtomicLong unexpected = new AtomicLong();
    private final AtomicLong lastSuccess = new AtomicLong();
    private final Counter failures;

    @Autowired
    public ScheduledS3Audit(
            S3Client s3,
            NamedParameterJdbcTemplate jdbc,
            MeterRegistry registry,
            @Value("${app.file-storage.s3.bucket}") String bucket,
            @Value("${app.file-storage.s3-reconciliation.grace-seconds:86400}") long graceSeconds,
            @Value("${app.file-storage.s3-reconciliation.max-pages:100}") int maxPages,
            @Value("${app.file-storage.s3-reconciliation.max-log-entries:20}") int maxLogEntries) {
        this(s3, jdbc, registry, bucket, graceSeconds, maxPages, maxLogEntries, Clock.systemUTC());
    }

    // Package-private constructor permits deterministic tests without hitting a real S3 bucket.
    ScheduledS3Audit(
            S3Client s3,
            NamedParameterJdbcTemplate jdbc,
            MeterRegistry registry,
            String bucket,
            long graceSeconds,
            int maxPages,
            int maxLogEntries,
            Clock clock) {
        if (bucket == null || bucket.isBlank() || bucket.toLowerCase().contains("backup")) {
            throw new IllegalArgumentException("An application-files S3 bucket is required");
        }
        if (graceSeconds < 3600
                || maxPages < 1
                || maxPages > 10000
                || maxLogEntries < 0
                || maxLogEntries > 100) {
            throw new IllegalArgumentException("Invalid S3 audit settings");
        }
        this.s3 = s3;
        this.jdbc = jdbc;
        this.bucket = bucket;
        this.graceSeconds = graceSeconds;
        this.maxPages = maxPages;
        this.maxLogEntries = maxLogEntries;
        this.clock = clock;
        this.failures =
                Counter.builder("tutor.file.s3.audit.failures")
                        .description("Failed read-only S3 audit runs")
                        .register(registry);
        Gauge.builder("tutor.file.s3.audit.scanned", scanned, AtomicLong::get)
                .description("UUID-shaped application objects scanned in last complete audit")
                .register(registry);
        Gauge.builder("tutor.file.s3.audit.unregistered", unregistered, AtomicLong::get)
                .description(
                        "Objects absent from file_assets in last complete audit, including recent")
                .register(registry);
        Gauge.builder("tutor.file.s3.audit.eligible", eligible, AtomicLong::get)
                .description(
                        "Unregistered objects older than grace period; candidates for review only")
                .register(registry);
        Gauge.builder("tutor.file.s3.audit.unexpected", unexpected, AtomicLong::get)
                .description(
                        "Keys under materials/ that do not match the application's UUID key format")
                .register(registry);
        Gauge.builder(
                        "tutor.file.s3.audit.last.success.epoch.seconds",
                        lastSuccess,
                        AtomicLong::get)
                .description("Unix timestamp of last fully completed S3 audit")
                .register(registry);
    }

    @Scheduled(
            fixedDelayString = "${app.file-storage.s3-reconciliation.interval-ms:3600000}",
            initialDelayString = "${app.file-storage.s3-reconciliation.initial-delay-ms:60000}")
    public void audit() {
        // Commit gauges only after every S3 page and DB query completed successfully.
        long totalScanned = 0;
        long totalUnregistered = 0;
        long totalEligible = 0;
        long totalUnexpected = 0;
        int logged = 0;
        String continuation = null;
        Set<String> seenKeys = new HashSet<>();
        Instant olderThan = clock.instant().minusSeconds(graceSeconds);
        try {
            for (int pageNumber = 0; pageNumber < maxPages; pageNumber++) {
                ListObjectsV2Response page =
                        s3.listObjectsV2(
                                ListObjectsV2Request.builder()
                                        .bucket(bucket)
                                        .prefix(PREFIX)
                                        .maxKeys(PAGE_SIZE)
                                        .continuationToken(continuation)
                                        .build());
                LinkedHashMap<String, S3Object> valid = new LinkedHashMap<>();
                for (S3Object object : page.contents()) {
                    String key = object.key();
                    if (key == null || !key.matches(KEY_PATTERN)) {
                        totalUnexpected++;
                        continue;
                    }
                    if (!seenKeys.add(key)) {
                        continue;
                    }
                    if (object.lastModified() == null) {
                        throw new IllegalStateException("Object has no LastModified: " + key);
                    }
                    valid.put(key, object);
                }
                totalScanned += valid.size();
                if (!valid.isEmpty()) {
                    List<String> known =
                            jdbc.queryForList(
                                    "SELECT storage_key FROM file_assets "
                                            + "WHERE storage_provider = 'S3' AND storage_key IN (:keys)",
                                    new MapSqlParameterSource(
                                            "keys", new ArrayList<>(valid.keySet())),
                                    String.class);
                    Set<String> knownKeys = new HashSet<>(known);
                    for (S3Object object : valid.values()) {
                        if (knownKeys.contains(object.key())) {
                            continue;
                        }
                        totalUnregistered++;
                        boolean old = object.lastModified().isBefore(olderThan);
                        if (old) {
                            totalEligible++;
                        }
                        if (logged < maxLogEntries) {
                            log.warn(
                                    "S3_AUDIT_UNREGISTERED key={} eligibleForReview={} lastModified={}",
                                    object.key(),
                                    old,
                                    object.lastModified());
                            logged++;
                        }
                    }
                }
                if (!Boolean.TRUE.equals(page.isTruncated())) {
                    scanned.set(totalScanned);
                    unregistered.set(totalUnregistered);
                    eligible.set(totalEligible);
                    unexpected.set(totalUnexpected);
                    lastSuccess.set(clock.instant().getEpochSecond());
                    log.info(
                            "S3_AUDIT_COMPLETE bucket={} scanned={} unregistered={} "
                                    + "eligibleForReview={} unexpected={} deletions=0",
                            bucket,
                            totalScanned,
                            totalUnregistered,
                            totalEligible,
                            totalUnexpected);
                    return;
                }
                String next = page.nextContinuationToken();
                if (next == null || next.isBlank() || next.equals(continuation)) {
                    throw new IllegalStateException("Invalid S3 pagination continuation token");
                }
                continuation = next;
            }
            throw new IllegalStateException(
                    "S3 audit max-pages limit exceeded; metrics not updated");
        } catch (RuntimeException error) {
            failures.increment();
            log.error(
                    "S3_AUDIT_FAILED bucket={} lastSuccessfulAuditEpochSeconds={}",
                    bucket,
                    lastSuccess.get(),
                    error);
        }
    }
}
