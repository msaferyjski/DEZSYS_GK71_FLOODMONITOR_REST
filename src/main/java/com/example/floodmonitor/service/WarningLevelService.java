package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class WarningLevelService {

    private final Duration maxAge;

    public WarningLevelService(@Value("${measurement.max-age-seconds:300}") long maxAgeSeconds) {
        this.maxAge = Duration.ofSeconds(maxAgeSeconds);
    }

    public WarningLevel calculate(Station station, double waterLevel) {
        if (Double.isNaN(waterLevel) || waterLevel < 0) {
            return WarningLevel.UNKNOWN;
        }
        if (waterLevel >= station.getCriticalWaterLevel()) {
            return WarningLevel.CRITICAL;
        }
        if (waterLevel >= station.getWarningWaterLevel()) {
            return WarningLevel.WARNING;
        }
        return WarningLevel.NORMAL;
    }

    public WarningLevel current(Station station, Optional<Measurement> latest) {
        if (!station.isActive() || latest.isEmpty() || latest.get().getTimestamp() == null) {
            return WarningLevel.UNKNOWN;
        }
        if (latest.get().getTimestamp().isBefore(Instant.now().minus(maxAge))) {
            return WarningLevel.UNKNOWN;
        }
        return calculate(station, latest.get().getWaterLevel());
    }
}
