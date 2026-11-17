package com.example.floodmonitor.controller;

import com.example.floodmonitor.exception.ErrorResponse;
import com.example.floodmonitor.model.MeasurementList;
import com.example.floodmonitor.repository.MeasurementRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OverviewController {
    private final MeasurementRepository measurements;

    public OverviewController(MeasurementRepository measurements) {
        this.measurements = measurements;
    }

    @Operation(summary = "Letzten empfangenen Messwert je Station zusammenfassen", tags = "Stationen")
    @ApiResponse(responseCode = "401", description = "API-Key fehlt oder ist ungültig",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping(value = "/api/v1/measurements/latest", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public MeasurementList latest() {
        return new MeasurementList(measurements.latestByStation());
    }
}
