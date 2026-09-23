package com.tutorplatform.file.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

class ScheduledS3AuditTest {
    private static final Instant NOW = Instant.parse("2026-09-23T18:00:00Z");
    private static final String KNOWN = "materials/00000000-0000-4000-8000-000000000001";
    private static final String OLD = "materials/00000000-0000-4000-8000-000000000002";
    private static final String RECENT = "materials/00000000-0000-4000-8000-000000000003";

    @Test
    void auditsPagesAndCountsOldVersusRecentWithoutDeleting() {
        S3Client s3 = mock(S3Client.class);
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        var registry = new SimpleMeterRegistry();
        when(s3.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(
                        ListObjectsV2Response.builder()
                                .contents(
                                        object(KNOWN, NOW.minusSeconds(172800)),
                                        object(OLD, NOW.minusSeconds(172800)))
                                .isTruncated(true)
                                .nextContinuationToken("next")
                                .build(),
                        ListObjectsV2Response.builder()
                                .contents(
                                        object(RECENT, NOW.minusSeconds(60)),
                                        object(
                                                "materials/unexpected.txt",
                                                NOW.minusSeconds(172800)))
                                .isTruncated(false)
                                .build());
        when(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(String.class)))
                .thenReturn(List.of(KNOWN), List.of());
        var audit =
                new ScheduledS3Audit(
                        s3,
                        jdbc,
                        registry,
                        "ororura-tutor-demo-files",
                        86400,
                        100,
                        20,
                        Clock.fixed(NOW, ZoneOffset.UTC));

        audit.audit();

        assertThat(registry.get("tutor.file.s3.audit.scanned").gauge().value()).isEqualTo(3.0);
        assertThat(registry.get("tutor.file.s3.audit.unregistered").gauge().value()).isEqualTo(2.0);
        assertThat(registry.get("tutor.file.s3.audit.eligible").gauge().value()).isEqualTo(1.0);
        assertThat(registry.get("tutor.file.s3.audit.unexpected").gauge().value()).isEqualTo(1.0);
        assertThat(registry.get("tutor.file.s3.audit.failures").counter().count()).isZero();
        verify(s3, org.mockito.Mockito.never())
                .deleteObject(
                        any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));
    }

    @Test
    void failedS3ListingKeepsLastCompletedGauges() {
        S3Client s3 = mock(S3Client.class);
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        var registry = new SimpleMeterRegistry();
        when(s3.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(
                        ListObjectsV2Response.builder()
                                .contents(object(OLD, NOW.minusSeconds(172800)))
                                .isTruncated(false)
                                .build())
                .thenThrow(new IllegalStateException("S3 unavailable"));
        when(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class), eq(String.class)))
                .thenReturn(List.of());
        var audit =
                new ScheduledS3Audit(
                        s3,
                        jdbc,
                        registry,
                        "ororura-tutor-demo-files",
                        86400,
                        100,
                        20,
                        Clock.fixed(NOW, ZoneOffset.UTC));

        audit.audit();
        audit.audit();

        assertThat(registry.get("tutor.file.s3.audit.unregistered").gauge().value()).isEqualTo(1.0);
        assertThat(registry.get("tutor.file.s3.audit.failures").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("tutor.file.s3.audit.last.success.epoch.seconds").gauge().value())
                .isEqualTo((double) NOW.getEpochSecond());
    }

    private static S3Object object(String key, Instant modified) {
        return S3Object.builder().key(key).lastModified(modified).size(10L).build();
    }
}
