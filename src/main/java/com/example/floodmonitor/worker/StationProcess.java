package com.example.floodmonitor.worker;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.repository.StationRepository;
import com.example.floodmonitor.service.SimulationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class StationProcess {
    private static final int WINDOW = 60;
    private final String id;
    private final double normal;
    private final Duration interval;
    private final byte[] key;
    private final Deque<Measurement> history = new ArrayDeque<>();
    private final SimulationService simulator = new SimulationService();
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public StationProcess(String id, double normal, long intervalMs, String key) {
        if (key == null || key.isBlank() || intervalMs < 1) {
            throw new IllegalArgumentException("Stationsschlüssel und positives Intervall erforderlich");
        }
        this.id = id;
        this.normal = normal;
        this.interval = Duration.ofMillis(intervalMs);
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    public static void start(String[] args) throws IOException {
        String id = args[1];
        double normal = new StationRepository().findById(id).map(Station::getNormalWaterLevel).orElse(200.0);
        long interval = Long.parseLong(System.getenv().getOrDefault("POLL_INTERVAL_MS", "10000"));
        new StationProcess(id, normal, interval, System.getenv("STATION_KEY")).listen(Integer.parseInt(args[2]));
    }

    public HttpServer listen(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/measurements", this::handle);
        server.start();
        return server;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!exchange.getRequestURI().getPath().equals("/measurements")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            String supplied = exchange.getRequestHeaders().getFirst("X-Station-Key");
            if (supplied == null || !MessageDigest.isEqual(key, supplied.getBytes(StandardCharsets.UTF_8))) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            Instant after = null;
            if (query != null) {
                if (!query.startsWith("after=")) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }
                try {
                    after = Instant.parse(query.substring(6));
                } catch (DateTimeParseException e) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }
            }
            byte[] body = json.writeValueAsBytes(sample(after));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private synchronized List<Measurement> sample(Instant after) {
        Instant now = Instant.now();
        if (history.isEmpty()) {
            for (int i = WINDOW - 1; i >= 0; i--) {
                history.add(simulator.next(id, normal, history.peekLast(), now.minus(interval.multipliedBy(i))));
            }
        } else if (!now.isBefore(history.getLast().getTimestamp().plus(interval))) {
            history.add(simulator.next(id, normal, history.getLast(), now));
            history.removeFirst();
        }
        return history.stream().filter(m -> after == null || m.getTimestamp().isAfter(after)).toList();
    }
}
