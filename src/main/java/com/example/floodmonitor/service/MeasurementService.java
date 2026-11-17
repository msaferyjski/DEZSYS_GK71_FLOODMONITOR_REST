package com.example.floodmonitor.service;

import com.example.floodmonitor.exception.ApiException;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.MeasurementPage;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.Statistics;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Stream;

@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final StationService stationService;
    private final int defaultLimit;
    private final int maxLimit;

    public MeasurementService(MeasurementRepository measurementRepository, StationService stationService,
                              @Value("${api.limit.default:100}") int defaultLimit,
                              @Value("${api.limit.max:1000}") int maxLimit) {
        this.measurementRepository = measurementRepository;
        this.stationService = stationService;
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
    }

    public Measurement latest(String stationId) {
        stationService.findById(stationId);
        return measurementRepository.findLatest(stationId)
                .orElseThrow(() -> ApiException.notFound("Für Station " + stationId + " liegen keine Messwerte vor"));
    }

    public MeasurementPage history(String stationId, Instant from, Instant to, Integer limit, WarningLevel warningLevel,
                                   int page, String sort) {
        int max = limit == null ? defaultLimit : limit;
        if (max < 1 || max > maxLimit) {
            throw ApiException.badRequest("limit muss zwischen 1 und " + maxLimit + " liegen");
        }
        if (page < 1 || !(sort.equals("asc") || sort.equals("desc"))) {
            throw ApiException.badRequest("page muss positiv sein, sort muss asc oder desc sein");
        }
        Comparator<Measurement> order = Comparator.comparing(Measurement::getTimestamp);
        List<Measurement> matches = filtered(stationId, from, to)
                .filter(m -> warningLevel == null || m.getWarningLevel() == warningLevel)
                .sorted(sort.equals("asc") ? order : order.reversed())
                .toList();
        int start = (int) Math.min((long) (page - 1) * max, matches.size());
        int end = (int) Math.min((long) start + max, matches.size());
        return new MeasurementPage(matches.subList(start, end), page, max, matches.size(), sort);
    }

    public Statistics statistics(String stationId, Instant from, Instant to) {
        List<Measurement> list = filtered(stationId, from, to).toList();
        Statistics stats = new Statistics();
        stats.setStationId(stationId);
        stats.setFrom(from);
        stats.setTo(to);
        stats.setMeasurementCount(list.size());
        if (!list.isEmpty()) {
            DoubleSummaryStatistics level = list.stream().mapToDouble(Measurement::getWaterLevel).summaryStatistics();
            stats.setMinWaterLevel(level.getMin());
            stats.setMaxWaterLevel(level.getMax());
            stats.setAvgWaterLevel(round(level.getAverage()));
            stats.setAvgFlowRate(round(list.stream().mapToDouble(Measurement::getFlowRate).average().orElse(0)));
        }
        stats.setTotalRainfall(round(list.stream().mapToDouble(Measurement::getRainfall).sum()));
        stats.setWarningCount((int) list.stream()
                .filter(m -> m.getWarningLevel() == WarningLevel.WARNING || m.getWarningLevel() == WarningLevel.CRITICAL).count());
        stats.setCriticalCount((int) list.stream().filter(m -> m.getWarningLevel() == WarningLevel.CRITICAL).count());
        return stats;
    }

    private Stream<Measurement> filtered(String stationId, Instant from, Instant to) {
        Station station = stationService.findById(stationId);
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("from darf nicht nach to liegen");
        }
        return measurementRepository.findByStationId(station.getId()).stream()
                .filter(m -> from == null || !m.getTimestamp().isBefore(from))
                .filter(m -> to == null || !m.getTimestamp().isAfter(to));
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
