package com.tutorplatform.report.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.report.domain.UnsupportedProgressReportSnapshotException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProgressReportSnapshotJsonCodecTest {

    @Test
    void rejectsAnUnsupportedSnapshotVersionWithAControlledException() throws Exception {
        var codec = new ProgressReportSnapshotJsonCodec(new ObjectMapper());

        assertThatThrownBy(() -> codec.read(2, new ObjectMapper().readTree("{}")))
            .isInstanceOf(UnsupportedProgressReportSnapshotException.class)
            .hasMessageContaining("version 2");
    }
}
