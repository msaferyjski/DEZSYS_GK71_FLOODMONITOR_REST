package com.example.floodmonitor.service;

import com.example.floodmonitor.exception.ApiException;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final WarningLevelService warningLevelService;

    public StationService(StationRepository stationRepository, MeasurementRepository measurementRepository,
                          WarningLevelService warningLevelService) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.warningLevelService = warningLevelService;
    }

    public List<Station> findAll(String river, Boolean isActive, WarningLevel warningLevel) {
        return stationRepository.findAll().stream()
                .map(this::withWarningLevel)
                .filter(s -> river == null || s.getRiver().equalsIgnoreCase(river))
                .filter(s -> isActive == null || s.isActive() == isActive)
                .filter(s -> warningLevel == null || s.getCurrentWarningLevel() == warningLevel)
                .toList();
    }

    public Station findById(String id) {
        return stationRepository.findById(id)
                .map(this::withWarningLevel)
                .orElseThrow(() -> ApiException.notFound("Station " + id + " wurde nicht gefunden"));
    }

    public Station create(Station station) {
        validate(station);
        if (stationRepository.existsById(station.getId())) {
            throw ApiException.conflict("Station " + station.getId() + " existiert bereits");
        }
        station.setStatus(station.isActive() ? StationStatus.ONLINE : StationStatus.OFFLINE);
        return withWarningLevel(stationRepository.save(station));
    }

    public void delete(String id) {
        if (!stationRepository.existsById(id)) {
            throw ApiException.notFound("Station " + id + " wurde nicht gefunden");
        }
        stationRepository.deleteById(id);
        measurementRepository.deleteByStationId(id);
    }

    private Station withWarningLevel(Station station) {
        station.setCurrentWarningLevel(warningLevelService.current(station, measurementRepository.findLatest(station.getId())));
        return station;
    }

    private void validate(Station s) {
        if (s.getId() == null || !s.getId().matches("[A-Z0-9-]{1,20}")) {
            throw ApiException.badRequest("id muss aus 1 bis 20 Zeichen A-Z, 0-9 oder - bestehen");
        }
        if (s.getName() == null || s.getName().isBlank() || s.getRiver() == null || s.getRiver().isBlank()) {
            throw ApiException.badRequest("name und river dürfen nicht leer sein");
        }
        if (s.getLocation() == null || Math.abs(s.getLocation().getLatitude()) > 90 || Math.abs(s.getLocation().getLongitude()) > 180) {
            throw ApiException.badRequest("location ist ungültig");
        }
        if (s.getNormalWaterLevel() < 0 || s.getNormalWaterLevel() >= s.getWarningWaterLevel()
                || s.getWarningWaterLevel() >= s.getCriticalWaterLevel()) {
            throw ApiException.badRequest("Es muss gelten: 0 <= normalWaterLevel < warningWaterLevel < criticalWaterLevel");
        }
    }
}
