package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class StationRepository {

    private final Map<String, Station> stations = new LinkedHashMap<>();

    public StationRepository() {
        save(new Station("ST-001", "Donaubrücke Linz", "Donau", new Location(48.3069, 14.2858), 250, 400, 550, true, StationStatus.ONLINE));
        save(new Station("ST-002", "Reichsbrücke Wien", "Donau", new Location(48.2290, 16.4100), 280, 450, 600, true, StationStatus.ONLINE));
        save(new Station("ST-003", "Staatsbrücke Salzburg", "Salzach", new Location(47.8010, 13.0440), 180, 260, 340, true, StationStatus.ONLINE));
        save(new Station("ST-004", "Innbrücke Innsbruck", "Inn", new Location(47.2692, 11.3933), 200, 300, 400, true, StationStatus.ONLINE));
        save(new Station("ST-005", "Hauptbrücke Graz", "Mur", new Location(47.0720, 15.4340), 150, 300, 420, false, StationStatus.OFFLINE));
    }

    public synchronized List<Station> findAll() {
        return new ArrayList<>(stations.values());
    }

    public synchronized Optional<Station> findById(String id) {
        return Optional.ofNullable(stations.get(id));
    }

    public synchronized boolean existsById(String id) {
        return stations.containsKey(id);
    }

    public synchronized Station save(Station station) {
        stations.put(station.getId(), station);
        return station;
    }

    public synchronized void deleteById(String id) {
        stations.remove(id);
    }
}
