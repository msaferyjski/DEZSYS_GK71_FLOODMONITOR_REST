package com.example.floodmonitor.service;

import com.example.floodmonitor.repository.MeasurementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "simulation.enabled", havingValue = "true", matchIfMissing = true)
public class SimulationScheduler {

    private final SimulationService simulationService;
    private final MeasurementRepository measurementRepository;
    private final long interval;
    private final int historySize;

    public SimulationScheduler(SimulationService simulationService, MeasurementRepository measurementRepository,
                               @Value("${simulation.interval:10000}") long interval,
                               @Value("${simulation.history-size:60}") int historySize) {
        this.simulationService = simulationService;
        this.measurementRepository = measurementRepository;
        this.interval = interval;
        this.historySize = historySize;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (measurementRepository.isEmpty()) {
            simulationService.seedHistory(historySize, Duration.ofMillis(interval));
        }
    }

    @Scheduled(fixedDelayString = "${simulation.interval:10000}", initialDelayString = "${simulation.interval:10000}")
    public void tick() {
        simulationService.simulateAll(Instant.now());
    }
}
