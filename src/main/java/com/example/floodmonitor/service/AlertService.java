package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Alert;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final WarningLevelService warningLevelService;

    public AlertService(StationRepository stationRepository, MeasurementRepository measurementRepository,
                        WarningLevelService warningLevelService) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.warningLevelService = warningLevelService;
    }

    public List<Alert> currentAlerts(WarningLevel warningLevel, String river) {
        return stationRepository.findAll().stream()
                .filter(s -> river == null || s.getRiver().equalsIgnoreCase(river))
                .map(this::toAlert)
                .filter(a -> warningLevel == null
                        ? a.getWarningLevel() == WarningLevel.WARNING || a.getWarningLevel() == WarningLevel.CRITICAL
                        : a.getWarningLevel() == warningLevel)
                .toList();
    }

    private Alert toAlert(Station station) {
        Optional<Measurement> latest = measurementRepository.findLatest(station.getId());
        return new Alert(station.getId(), station.getName(), station.getRiver(),
                latest.map(Measurement::getTimestamp).orElse(null),
                latest.map(Measurement::getWaterLevel).orElse(null),
                station.getWarningWaterLevel(), station.getCriticalWaterLevel(),
                warningLevelService.current(station, latest));
    }
}
