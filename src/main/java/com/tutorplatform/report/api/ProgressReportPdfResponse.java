package com.tutorplatform.report.api;

import com.tutorplatform.report.application.ProgressReportPdfDownload;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

final class ProgressReportPdfResponse {

    private ProgressReportPdfResponse() {
    }

    static ResponseEntity<byte[]> attachment(ProgressReportPdfDownload download) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(download.content().length)
            .header("Content-Disposition", ContentDisposition.attachment()
                .filename(download.filename()).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .header("Cache-Control", "no-store")
            .body(download.content());
    }
}
