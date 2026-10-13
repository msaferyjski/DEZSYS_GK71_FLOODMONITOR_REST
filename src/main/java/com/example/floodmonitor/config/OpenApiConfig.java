package com.example.floodmonitor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI floodMonitorOpenApi() {
        return new OpenAPI()
                .info(new Info().title("FloodMonitor REST API").version("v1")
                        .description("Hochwasser-Frühwarnsystem. Alle Antworten als application/json (Standard) oder application/xml über den Accept-Header. "
                                + "Lesende Endpunkte sind öffentlich, schreibende benötigen den Header X-API-Key."))
                .components(new Components().addSecuritySchemes("apiKey", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name(ApiKeyInterceptor.HEADER)));
    }
}
