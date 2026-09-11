package com.tutorplatform.report.application;

public interface ProgressReportPdfRenderer {
    byte[] render(ProgressReportPdfModel model);
}
