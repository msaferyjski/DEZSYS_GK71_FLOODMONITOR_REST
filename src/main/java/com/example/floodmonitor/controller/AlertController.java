package com.example.floodmonitor.controller;

import com.example.floodmonitor.exception.ErrorResponse;
import com.example.floodmonitor.model.AlertList;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/alerts", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@ApiResponse(responseCode = "401", description = "API-Key fehlt oder ist ungültig", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@Tag(name = "Warnungen")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "Aktuelle Warnungen abrufen", description = "Ohne warningLevel werden WARNING und CRITICAL geliefert")
    @ApiResponse(responseCode = "200", description = "Liste der Warnungen")
    @ApiResponse(responseCode = "400", description = "Ungültige Eingabe", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "406", description = "Format nicht unterstützt", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AlertList getAlerts(
            @Parameter(description = "Warnstufe") @RequestParam(required = false) WarningLevel warningLevel,
            @Parameter(description = "Fluss") @RequestParam(required = false) String river) {
        return new AlertList(alertService.currentAlerts(warningLevel, river));
    }
}
