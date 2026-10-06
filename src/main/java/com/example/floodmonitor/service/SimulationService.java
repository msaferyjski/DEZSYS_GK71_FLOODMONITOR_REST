package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Service
public class SimulationService {

    private static final int MAINTENANCE_BATTERY = 10;

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final WarningLevelService warningLevelService;
    private final Random random = new Random();

    public SimulationService(StationRepository stationRepository, MeasurementRepository measurementRepository,
                             WarningLevelService warningLevelService) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.warningLevelService = warningLevelService;
    }

    public void simulateAll(Instant timestamp) {
        for (Station station : stationRepository.findAll()) {
            if (!station.isActive() || station.getStatus() == StationStatus.OFFLINE) {
                continue;
            }
            Measurement previous = measurementRepository.findLatest(station.getId()).orElse(null);
            Measurement next = next(station, previous, timestamp);
            station.setStatus(next.getStatus());
            measurementRepository.save(next);
        }
    }

    public void seedHistory(int count, Duration interval) {
        Instant now = Instant.now();
        for (int i = count; i > 0; i--) {
            simulateAll(now.minus(interval.multipliedBy(i)));
        }
    }

    public Measurement next(Station station, Measurement previous, Instant timestamp) {
        double prevLevel = previous == null ? station.getNormalWaterLevel() : previous.getWaterLevel();
        double prevRain = previous == null ? 0 : previous.getRainfall();
        double prevTemp = previous == null ? 12 : previous.getTemperature();
        int prevBattery = previous == null ? 100 : previous.getBatteryLevel();
        StationStatus prevStatus = previous == null ? StationStatus.ONLINE : previous.getStatus();

        double storm = random.nextDouble() < 0.03 ? 10 + random.nextDouble() * 25 : 0;
        double rainfall = clamp(prevRain * 0.7 + Math.max(0, random.nextGaussian()) + storm, 0, 80);

        double drift = -(prevLevel - station.getNormalWaterLevel()) * 0.03;
        double delta = clamp((rainfall - 2) * 0.8 + drift + random.nextGaussian(), -10, 20);
        double waterLevel = Math.max(0, prevLevel + delta);

        double flowRate = 0.08 * Math.pow(waterLevel, 1.6);
        double temperature = clamp(prevTemp + random.nextGaussian() * 0.3, -25, 40);

        int battery;
        StationStatus status;
        if (prevStatus == StationStatus.MAINTENANCE) {
            battery = 100;
            status = StationStatus.ONLINE;
        } else {
            battery = Math.max(0, prevBattery - (random.nextBoolean() ? 1 : 0));
            status = battery < MAINTENANCE_BATTERY ? StationStatus.MAINTENANCE : StationStatus.ONLINE;
        }

        double level = round(waterLevel);
        return new Measurement(station.getId(), timestamp.truncatedTo(ChronoUnit.SECONDS), level, round(flowRate),
                round(rainfall), round(temperature), battery, status, warningLevelService.calculate(station, level));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
