package com.tutorplatform.report.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.report.domain.ProgressReportSnapshotSchemas;
import com.tutorplatform.report.domain.ProgressReportSnapshotV1;
import com.tutorplatform.report.domain.UnsupportedProgressReportSnapshotException;
import org.springframework.stereotype.Component;

@Component
public class ProgressReportSnapshotJsonCodec {

    private final ObjectMapper objectMapper;

    public ProgressReportSnapshotJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    JsonNode writeV1(ProgressReportSnapshotV1 snapshot) {
        return objectMapper.valueToTree(snapshot);
    }

    ProgressReportSnapshotV1 read(int schemaVersion, JsonNode json) {
        if (schemaVersion != ProgressReportSnapshotSchemas.V1) {
            throw new UnsupportedProgressReportSnapshotException(schemaVersion);
        }
        try {
            return objectMapper.treeToValue(json, ProgressReportSnapshotV1.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid ProgressReport snapshot v1 JSON", exception);
        }
    }
}
