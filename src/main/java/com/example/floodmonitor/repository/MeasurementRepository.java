package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Measurement;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MeasurementRepository {

    private static final int MAX_PER_STATION = 10000;

    private final Map<String, List<Measurement>> measurements = new HashMap<>();

    public synchronized void save(Measurement measurement) {
        List<Measurement> list = measurements.computeIfAbsent(measurement.getStationId(), k -> new ArrayList<>());
        list.add(measurement);
        if (list.size() > MAX_PER_STATION) {
            list.remove(0);
        }
    }

    public synchronized List<Measurement> findByStationId(String stationId) {
        return new ArrayList<>(measurements.getOrDefault(stationId, List.of()));
    }

    public synchronized Optional<Measurement> findLatest(String stationId) {
        List<Measurement> list = measurements.get(stationId);
        return list == null || list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }

    public synchronized boolean isEmpty() {
        return measurements.isEmpty();
    }

    public synchronized void deleteByStationId(String stationId) {
        measurements.remove(stationId);
    }

    public synchronized void deleteAll() {
        measurements.clear();
    }
}
