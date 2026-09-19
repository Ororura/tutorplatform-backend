package com.tutorplatform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI tutorPlatformOpenApi() {
        return new OpenAPI()
            .components(new Components().addSecuritySchemes(
                "cookieSession",
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .name("TUTOR_SESSION")
                    .description("Spring Security session cookie")
            ))
            .addSecurityItem(new SecurityRequirement().addList("cookieSession"))
            .info(new Info()
                .title("Tutor Learning Platform API")
                .version("v1")
                .description("Backend contract for the Tutor Learning Platform MVP."));
    }

    @Bean
    OpenApiCustomizer publicOperationsDoNotRequireSessionCookie() {
        return openApi -> {
            clearGetSecurity(openApi, "/api/v1/auth/csrf");
            clearGetSecurity(openApi, "/api/v1/public/registration-settings");
            clearGetSecurity(openApi, "/api/v1/public/teacher-invitations/{token}");
            clearPostSecurity(openApi, "/api/v1/public/teacher-invitations/{token}/accept");
            clearPostSecurity(openApi, "/api/v1/auth/register/teacher");
            clearPostSecurity(openApi, "/api/v1/auth/login");
            clearPostSecurity(openApi, "/api/v1/auth/logout");
            clearGetSecurity(openApi, "/api/v1/public/student-invitations/{token}");
            clearPostSecurity(openApi, "/api/v1/public/student-invitations/{token}/accept");
            clearGetSecurity(openApi, "/api/v1/public/progress/{token}");
            clearGetSecurity(openApi, "/api/v1/public/reports/{token}");
            clearGetSecurity(openApi, "/api/v1/public/reports/{token}/pdf");
        };
    }

    private static void clearGetSecurity(OpenAPI openApi, String path) {
        if (openApi.getPaths().get(path) != null && openApi.getPaths().get(path).getGet() != null) {
            openApi.getPaths().get(path).getGet().setSecurity(List.of());
        }
    }

    private static void clearPostSecurity(OpenAPI openApi, String path) {
        if (openApi.getPaths().get(path) != null && openApi.getPaths().get(path).getPost() != null) {
            openApi.getPaths().get(path).getPost().setSecurity(List.of());
        }
    }
}
