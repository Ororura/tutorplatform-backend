package com.tutorplatform.file.infrastructure;

import com.tutorplatform.file.application.FileStorage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Retry deletes after the lesson material transaction committed but external storage failed. Only
 * committed, unreferenced file_assets older than the grace window qualify. A row lock plus
 * idempotent storage.delete allow safe retries across backend instances. The S3 bucket is NOT
 * scanned: unregistered S3 objects need a separate guarded workflow.
 */
@Component
@ConditionalOnProperty(prefix = "app.file-storage.cleanup", name = "enabled", havingValue = "true")
public class ScheduledFileCleanup {
    private static final Logger log = LoggerFactory.getLogger(ScheduledFileCleanup.class);
    private static final String ELIGIBLE =
            """
            FROM file_assets fa
            WHERE fa.created_at < now() - (? * interval '1 second')
              AND NOT EXISTS (
                  SELECT 1 FROM lesson_materials lm WHERE lm.file_asset_id = fa.id
              )
            """;

    private final JdbcTemplate jdbc;
    private final FileStorage storage;
    private final TransactionTemplate transaction;
    private final long graceSeconds;
    private final int batchSize;
    private final AtomicInteger pending = new AtomicInteger();
    private final Counter cleaned;
    private final Counter failures;

    public ScheduledFileCleanup(
            JdbcTemplate jdbc,
            FileStorage storage,
            PlatformTransactionManager transactionManager,
            MeterRegistry registry,
            @Value("${app.file-storage.cleanup.grace-seconds:600}") long graceSeconds,
            @Value("${app.file-storage.cleanup.batch-size:50}") int batchSize) {
        if (graceSeconds < 60 || batchSize < 1 || batchSize > 500) {
            throw new IllegalArgumentException("Invalid file cleanup settings");
        }
        this.jdbc = jdbc;
        this.storage = storage;
        this.graceSeconds = graceSeconds;
        this.batchSize = batchSize;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.cleaned =
                Counter.builder("tutor.file.cleanup.cleaned")
                        .description("File assets removed after external object deletion")
                        .register(registry);
        this.failures =
                Counter.builder("tutor.file.cleanup.failures")
                        .description("Failed orphan file cleanup attempts")
                        .register(registry);
        Gauge.builder("tutor.file.cleanup.pending", pending, AtomicInteger::get)
                .description("Last observed count of eligible orphan file assets")
                .register(registry);
    }

    @Scheduled(
            fixedDelayString = "${app.file-storage.cleanup.interval-ms:3600000}",
            initialDelayString = "${app.file-storage.cleanup.initial-delay-ms:60000}")
    public void reconcile() {
        // Select IDs without holding locks; each row is revalidated and locked per attempt.
        List<UUID> ids =
                jdbc.query(
                        "SELECT fa.id " + ELIGIBLE + " ORDER BY fa.created_at, fa.id LIMIT ?",
                        (rs, rowNum) -> rs.getObject("id", UUID.class),
                        graceSeconds,
                        batchSize);
        Integer total =
                jdbc.queryForObject("SELECT count(*) " + ELIGIBLE, Integer.class, graceSeconds);
        pending.set(total == null ? 0 : total);
        for (UUID id : ids) {
            try {
                Boolean deleted = transaction.execute(status -> deleteOneIfStillOrphan(id));
                if (Boolean.TRUE.equals(deleted)) {
                    cleaned.increment();
                    log.info("FILE_STORAGE_ORPHAN_CLEANED assetId={}", id);
                }
            } catch (RuntimeException error) {
                failures.increment();
                log.error("FILE_STORAGE_CLEANUP_RETRY_FAILED assetId={}", id, error);
            }
        }
    }

    private boolean deleteOneIfStillOrphan(UUID id) {
        // SKIP LOCKED: another cleanup worker may already be processing this asset.
        List<Asset> assets =
                jdbc.query(
                        "SELECT fa.storage_provider, fa.storage_key "
                                + ELIGIBLE
                                + " AND fa.id = ? FOR UPDATE OF fa SKIP LOCKED",
                        (rs, rowNum) ->
                                new Asset(
                                        rs.getString("storage_provider"),
                                        rs.getString("storage_key")),
                        graceSeconds,
                        id);
        if (assets.isEmpty()) {
            return false; // Referenced, too new, deleted, or locked by another worker.
        }
        Asset asset = assets.getFirst();
        // Never remove DB metadata unless deletion from the recorded provider succeeds.
        // S3 and LOCAL implementations make deletion idempotent for retries after crashes.
        storage.delete(asset.provider(), asset.key());
        int rows =
                jdbc.update(
                        "DELETE FROM file_assets fa WHERE fa.id = ? "
                                + "AND NOT EXISTS (SELECT 1 FROM lesson_materials lm "
                                + "WHERE lm.file_asset_id = fa.id)",
                        id);
        return rows == 1;
    }

    private record Asset(String provider, String key) {}
}
