package com.tutorplatform.report.api;

import com.tutorplatform.report.api.response.PublicProgressReportResponse;
import com.tutorplatform.report.application.PublicProgressReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/reports")
public class PublicProgressReportController implements PublicProgressReportApi {

    private final PublicProgressReportService publicProgressReportService;

    public PublicProgressReportController(PublicProgressReportService publicProgressReportService) {
        this.publicProgressReportService = publicProgressReportService;
    }

    @Override
    @GetMapping("/{token}")
    public PublicProgressReportResponse getPublicProgressReport(@PathVariable String token) {
        return publicProgressReportService.get(token);
    }
}
