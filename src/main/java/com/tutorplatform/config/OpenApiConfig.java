package com.tutorplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI tutorPlatformOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Tutor Learning Platform API")
                .version("v1")
                .description("Backend contract for the Tutor Learning Platform MVP."));
    }
}
