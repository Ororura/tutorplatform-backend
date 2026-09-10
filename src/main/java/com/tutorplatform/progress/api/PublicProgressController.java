package com.tutorplatform.progress.api;

import com.tutorplatform.progress.api.response.PublicCurrentProgressResponse;
import com.tutorplatform.progress.application.PublicProgressShareService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/progress")
public class PublicProgressController implements PublicProgressApi {

    private final PublicProgressShareService progressShareService;

    public PublicProgressController(PublicProgressShareService progressShareService) {
        this.progressShareService = progressShareService;
    }

    @Override
    @GetMapping("/{token}")
    public PublicCurrentProgressResponse getPublicCurrentProgress(@PathVariable String token) {
        return progressShareService.get(token);
    }
}
