package com.tutorplatform.report.api;

import com.tutorplatform.report.api.response.PublicProgressReportResponse;
import com.tutorplatform.report.application.PublicProgressReportService;
import com.tutorplatform.report.application.ProgressReportPdfDownload;
import com.tutorplatform.report.application.ProgressReportPdfService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/reports")
public class PublicProgressReportController implements PublicProgressReportApi {

    private final PublicProgressReportService publicProgressReportService;
    private final ProgressReportPdfService pdfService;

    public PublicProgressReportController(
        PublicProgressReportService publicProgressReportService,
        ProgressReportPdfService pdfService
    ) {
        this.publicProgressReportService = publicProgressReportService;
        this.pdfService = pdfService;
    }

    @Override
    @GetMapping("/{token}")
    public PublicProgressReportResponse getPublicProgressReport(@PathVariable String token) {
        return publicProgressReportService.get(token);
    }

    @Override
    @GetMapping("/{token}/pdf")
    public ResponseEntity<byte[]> downloadPublicProgressReportPdf(@PathVariable String token) {
        ProgressReportPdfDownload download = pdfService.downloadPublic(token);
        return ProgressReportPdfResponse.attachment(download);
    }
}
