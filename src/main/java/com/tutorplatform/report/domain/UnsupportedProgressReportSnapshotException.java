package com.tutorplatform.report.domain;

public class UnsupportedProgressReportSnapshotException extends RuntimeException {
    public UnsupportedProgressReportSnapshotException(int version) {
        super("Unsupported ProgressReport snapshot schema version " + version);
    }
}
