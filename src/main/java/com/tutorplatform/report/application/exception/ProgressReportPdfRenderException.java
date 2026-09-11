package com.tutorplatform.report.application.exception;

public class ProgressReportPdfRenderException extends RuntimeException {
    private final String category;

    public ProgressReportPdfRenderException(String category) {
        super("Progress report PDF generation failed");
        this.category = category;
    }

    public ProgressReportPdfRenderException(String category, Throwable cause) {
        super("Progress report PDF generation failed", cause);
        this.category = category;
    }

    public String category() {
        return category;
    }
}
