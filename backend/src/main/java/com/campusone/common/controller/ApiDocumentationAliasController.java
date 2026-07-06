package com.campusone.common.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ConditionalOnProperty(
        prefix = "springdoc.api-docs",
        name = "enabled",
        havingValue = "true")
public class ApiDocumentationAliasController {

    @GetMapping({
        "/v3/api-docs",
        "/v3/api-docs/"
    })
    public String redirectOpenApiDocument() {
        return "redirect:/api/v1/openapi";
    }

    @GetMapping("/v3/api-docs/swagger-config")
    public String redirectSwaggerConfiguration() {
        return "redirect:/api/v1/openapi/swagger-config";
    }

    @GetMapping({
        "/swagger-ui",
        "/swagger-ui/",
        "/swagger-ui.html",
        "/swagger-ui/index.html",
        "/api/v1/swagger-ui/index.html"
    })
    public String redirectSwaggerUi() {
        return "redirect:/api/v1/swagger-ui";
    }
}
