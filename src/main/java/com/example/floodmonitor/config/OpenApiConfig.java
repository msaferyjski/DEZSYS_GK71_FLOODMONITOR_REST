package com.example.floodmonitor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI floodMonitorOpenApi() {
        return new OpenAPI()
                .info(new Info().title("FloodMonitor REST API").version("v1")
                        .description("Hochwasser-Frühwarnsystem. Alle Antworten als application/json (Standard) oder application/xml über den Accept-Header. "
                                + "Viewer und Admin lesen mit X-API-Key. Nur Admin darf Stationen anlegen und löschen."))
                .addServersItem(new Server().url("/"))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"))
                .components(new Components().addSecuritySchemes("apiKey", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name(ApiKeyInterceptor.HEADER)));
    }
}
