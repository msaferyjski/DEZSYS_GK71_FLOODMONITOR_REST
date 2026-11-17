package com.example.floodmonitor.controller;

import com.example.floodmonitor.exception.ErrorResponse;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.MeasurementPage;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationList;
import com.example.floodmonitor.model.Statistics;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.service.MeasurementService;
import com.example.floodmonitor.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;

@RestController
@RequestMapping(value = "/api/v1/stations", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@ApiResponse(responseCode = "401", description = "API-Key fehlt oder ist ungültig", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@Tag(name = "Stationen", description = "Messstationen, Messwerte und Statistik")
@ApiResponse(responseCode = "400", description = "Ungültige Eingabe", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "406", description = "Format nicht unterstützt", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
@ApiResponse(responseCode = "500", description = "Interner Fehler", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class StationController {

    private final StationService stationService;
    private final MeasurementService measurementService;

    public StationController(StationService stationService, MeasurementService measurementService) {
        this.stationService = stationService;
        this.measurementService = measurementService;
    }

    @GetMapping
    @Operation(summary = "Alle Messstationen abrufen")
    @ApiResponse(responseCode = "200", description = "Liste der Stationen")
    public StationList getStations(
            @Parameter(description = "Fluss, z.B. Donau") @RequestParam(required = false) String river,
            @Parameter(description = "Nur aktive oder inaktive Stationen") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Aktuelle Warnstufe") @RequestParam(required = false) WarningLevel warningLevel) {
        return new StationList(stationService.findAll(river, isActive, warningLevel));
    }

    @GetMapping("/{stationId}")
    @Operation(summary = "Einzelne Station abrufen")
    @ApiResponse(responseCode = "200", description = "Station")
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Station getStation(@PathVariable String stationId) {
        return stationService.findById(stationId);
    }

    @GetMapping("/{stationId}/measurements/latest")
    @Operation(summary = "Aktuellsten Messwert abrufen")
    @ApiResponse(responseCode = "200", description = "Messwert")
    @ApiResponse(responseCode = "404", description = "Station oder Messwert nicht gefunden", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Measurement getLatest(@PathVariable String stationId) {
        return measurementService.latest(stationId);
    }

    @GetMapping("/{stationId}/measurements")
    @Operation(summary = "Historische Messwerte abrufen", description = "Filterung und chronologische Sortierung vor der Seitenauswahl. Leere Seiten liefern 200.")
    @ApiResponse(responseCode = "200", description = "Liste der Messwerte")
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MeasurementPage getMeasurements(
            @PathVariable String stationId,
            @Parameter(description = "Beginn, ISO-8601", example = "2026-05-01T08:00:00Z") @RequestParam(required = false) Instant from,
            @Parameter(description = "Ende, ISO-8601", example = "2026-05-01T12:00:00Z") @RequestParam(required = false) Instant to,
            @Parameter(description = "Messwerte pro Seite, Standard 100", schema = @Schema(type = "integer", format = "int32", minimum = "1", maximum = "1000", defaultValue = "100")) @RequestParam(required = false) Integer limit,
            @Parameter(description = "Warnstufe") @RequestParam(required = false) WarningLevel warningLevel,
            @Parameter(description = "Seitennummer ab 1, Standard 1", schema = @Schema(type = "integer", format = "int32", minimum = "1", defaultValue = "1"))
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Chronologische Reihenfolge, Standard desc", schema = @Schema(defaultValue = "desc", allowableValues = {"asc", "desc"}))
            @RequestParam(defaultValue = "desc") String sort) {
        return measurementService.history(stationId, from, to, limit, warningLevel, page, sort);
    }

    @GetMapping("/{stationId}/statistics")
    @Operation(summary = "Statistik einer Station abrufen")
    @ApiResponse(responseCode = "200", description = "Statistik")
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Statistics getStatistics(
            @PathVariable String stationId,
            @Parameter(description = "Beginn, ISO-8601") @RequestParam(required = false) Instant from,
            @Parameter(description = "Ende, ISO-8601") @RequestParam(required = false) Instant to) {
        return measurementService.statistics(stationId, from, to);
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Station anlegen", security = @SecurityRequirement(name = "apiKey"))
    @ApiResponse(responseCode = "201", description = "Station erstellt")
    @ApiResponse(responseCode = "403", description = "Admin-Rechte erforderlich", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Station existiert bereits", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Medientyp nicht unterstützt", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Station> createStation(@RequestBody Station station) {
        Station created = stationService.create(station);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri()).body(created);
    }

    @DeleteMapping("/{stationId}")
    @Operation(summary = "Station löschen", security = @SecurityRequirement(name = "apiKey"))
    @ApiResponse(responseCode = "204", description = "Station gelöscht")
    @ApiResponse(responseCode = "403", description = "Admin-Rechte erforderlich", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteStation(@PathVariable String stationId) {
        stationService.delete(stationId);
        return ResponseEntity.noContent().build();
    }
}
