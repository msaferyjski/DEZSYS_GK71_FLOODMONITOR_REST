package com.example.floodmonitor;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import com.example.floodmonitor.service.StationPoller;
import com.example.floodmonitor.service.StationService;
import com.example.floodmonitor.service.WarningLevelService;
import com.example.floodmonitor.worker.StationProcess;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static org.assertj.core.api.Assertions.assertThat;

class StationPollingTest {
    @Test
    void collectsEachActiveStationOnceAndContinuesAfterFailure() throws Exception {
        StationRepository stations = new StationRepository();
        MeasurementRepository readings = new MeasurementRepository();
        StationService service = new StationService(stations, readings, new WarningLevelService(300));
        List<HttpServer> servers = new ArrayList<>();
        StringJoiner endpoints = new StringJoiner(",");
        try {
            for (var station : stations.findAll()) {
                HttpServer server = new StationProcess(station.getId(), station.getNormalWaterLevel(), 60000, "sensor")
                        .listen(0);
                servers.add(server);
                endpoints.add(station.getId() + ":" + server.getAddress().getPort());
            }
            int port = servers.get(0).getAddress().getPort();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/measurements"))
                    .header("X-API-Key", "viewer").build();
            assertThat(HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode())
                    .isEqualTo(401);
            servers.remove(0).stop(0);
            StationPoller poller = new StationPoller(stations, readings, service, endpoints.toString(), "sensor");
            poller.poll();
            poller.poll();
            assertThat(readings.latestByStation()).extracting(Measurement::getStationId)
                    .containsExactly("ST-002", "ST-003", "ST-004");
            List<Measurement> history = readings.findByStationId("ST-002");
            assertThat(history).hasSize(60).extracting(Measurement::getTimestamp).isSorted();
            assertThat(history).extracting(Measurement::getWarningLevel).doesNotContain(WarningLevel.UNKNOWN);
        } finally {
            servers.forEach(server -> server.stop(0));
        }
    }
}
