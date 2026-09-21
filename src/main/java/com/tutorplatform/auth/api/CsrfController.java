package com.tutorplatform.auth.api;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class CsrfController implements CsrfTokenApi {

    @GetMapping(value = "/csrf", produces = MediaType.APPLICATION_JSON_VALUE)
    public CsrfTokenResponse getCsrfToken(@Parameter(hidden = true) CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName());
    }
}
