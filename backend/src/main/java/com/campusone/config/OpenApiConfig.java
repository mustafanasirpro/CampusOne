package com.campusone.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI campusOneOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CampusOne Backend API")
                        .description("REST API foundation for CampusOne")
                        .version("v1")
                        .contact(new Contact().name("CampusOne")));
    }
}
