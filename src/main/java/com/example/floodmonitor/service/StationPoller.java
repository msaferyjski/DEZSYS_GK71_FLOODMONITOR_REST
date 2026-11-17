package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "polling.enabled", havingValue = "true", matchIfMissing = true)
public class StationPoller {
    private static final Logger log = LoggerFactory.getLogger(StationPoller.class);
    private final Map<String, Integer> ports = new LinkedHashMap<>();
    private final StationRepository stations;
    private final MeasurementRepository measurements;
    private final StationService stationService;
    private final RestClient client;

    public StationPoller(StationRepository stations, MeasurementRepository measurements, StationService stationService,
                         @Value("${station.ports}") String endpoints, @Value("${station.key}") String key) {
        this.stations = stations;
        this.measurements = measurements;
        this.stationService = stationService;
        for (String endpoint : endpoints.split(",")) {
            String[] parts = endpoint.strip().split(":", -1);
            if (parts.length != 2 || !parts[0].matches("[A-Z0-9-]{1,20}")) {
                throw new IllegalArgumentException("Stationsadresse muss ID:Port sein");
            }
            int port = Integer.parseInt(parts[1]);
            if (port < 1 || port > 65535 || ports.containsValue(port) || ports.containsKey(parts[0])) {
                throw new IllegalArgumentException("Stations-ID und gültiger Port müssen eindeutig sein");
            }
            ports.put(parts[0], port);
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(1500);
        this.client = RestClient.builder().requestFactory(factory).defaultHeader("X-Station-Key", key).build();
    }

    @Scheduled(fixedDelayString = "${polling.interval}")
    public void poll() {
        ports.forEach((id, port) -> stations.findById(id).filter(Station::isActive).ifPresent(station -> {
            String url = "http://127.0.0.1:" + port + "/measurements";
            Instant latest = measurements.findLatest(id).map(Measurement::getTimestamp).orElse(null);
            if (latest != null) {
                url += "?after=" + latest;
            }
            try {
                Measurement[] batch = client.get().uri(url).retrieve().body(Measurement[].class);
                if (batch != null) {
                    stationService.collect(station, batch);
                }
            } catch (RestClientException e) {
                log.warn("Messstation {} nicht erreichbar oder Antwort ungültig", id);
            }
        }));
    }
}
