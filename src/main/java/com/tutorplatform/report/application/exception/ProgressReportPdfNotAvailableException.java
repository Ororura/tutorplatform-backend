package com.tutorplatform.report.application.exception;

public class ProgressReportPdfNotAvailableException extends RuntimeException {
    public ProgressReportPdfNotAvailableException() {
        super("Only published progress reports can be exported as PDF");
    }
}
