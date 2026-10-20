package com.example.floodmonitor;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import com.example.floodmonitor.service.WarningLevelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest(properties = {"simulation.enabled=false", "api.key=test-key"})
@AutoConfigureMockMvc
class FloodMonitorApiTest {

    private static final String BASE = "/api/v1";
    private static final Instant T0 = Instant.parse("2026-05-01T08:00:00Z");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private StationRepository stationRepository;
    @Autowired
    private MeasurementRepository measurementRepository;
    @Autowired
    private WarningLevelService warningLevelService;

    @BeforeEach
    void seed() {
        measurementRepository.deleteAll();
        for (int i = 0; i <= 4; i++) {
            save("ST-001", T0.plus(i, ChronoUnit.HOURS), 300 + i * 10);
        }
    }

    private void save(String stationId, Instant timestamp, double waterLevel) {
        Station station = stationRepository.findById(stationId).orElseThrow();
        measurementRepository.save(new Measurement(stationId, timestamp, waterLevel, 700, 2, 14, 90,
                StationStatus.ONLINE, warningLevelService.calculate(station, waterLevel)));
    }

    @Test
    void stationsAsJson() throws Exception {
        mvc.perform(get(BASE + "/stations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stations", hasSize(5)))
                .andExpect(jsonPath("$.stations[0].id").value("ST-001"));
    }

    @Test
    void stationsAsXml() throws Exception {
        mvc.perform(get(BASE + "/stations").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("count(/stations/station)").number(5.0))
                .andExpect(xpath("/stations/station[1]/id").string("ST-001"));
    }

    @Test
    void existingStation() throws Exception {
        mvc.perform(get(BASE + "/stations/ST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ST-001"))
                .andExpect(jsonPath("$.river").value("Donau"));
    }

    @Test
    void unknownStation() throws Exception {
        mvc.perform(get(BASE + "/stations/ST-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value(BASE + "/stations/ST-999"));
    }

    @Test
    void latestMeasurement() throws Exception {
        mvc.perform(get(BASE + "/stations/ST-001/measurements/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value("ST-001"))
                .andExpect(jsonPath("$.timestamp").value("2026-05-01T12:00:00Z"))
                .andExpect(jsonPath("$.waterLevel").value(340.0));
    }

    @Test
    void filterByTimeRange() throws Exception {
        mvc.perform(get(BASE + "/stations/ST-001/measurements")
                        .param("from", "2026-05-01T09:00:00Z")
                        .param("to", "2026-05-01T11:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements", hasSize(3)))
                .andExpect(jsonPath("$.measurements[0].timestamp").value("2026-05-01T11:00:00Z"));
    }

    @Test
    void fromAfterTo() throws Exception {
        mvc.perform(get(BASE + "/stations/ST-001/measurements")
                        .param("from", "2026-05-01T12:00:00Z")
                        .param("to", "2026-05-01T08:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidDate() throws Exception {
        mvc.perform(get(BASE + "/stations/ST-001/measurements").param("from", "gestern"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidWarningLevel() throws Exception {
        mvc.perform(get(BASE + "/alerts").param("warningLevel", "PANIC"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void warningLevelNormal() throws Exception {
        save("ST-001", T0.plus(5, ChronoUnit.HOURS), 399);
        mvc.perform(get(BASE + "/stations/ST-001/measurements/latest"))
                .andExpect(jsonPath("$.warningLevel").value("NORMAL"));
    }

    @Test
    void warningLevelWarning() throws Exception {
        save("ST-001", T0.plus(5, ChronoUnit.HOURS), 400);
        mvc.perform(get(BASE + "/stations/ST-001/measurements/latest"))
                .andExpect(jsonPath("$.warningLevel").value("WARNING"));
    }

    @Test
    void warningLevelCritical() throws Exception {
        save("ST-001", T0.plus(5, ChronoUnit.HOURS), 550);
        mvc.perform(get(BASE + "/stations/ST-001/measurements/latest"))
                .andExpect(jsonPath("$.warningLevel").value("CRITICAL"));
    }

    @Test
    void unsupportedFormat() throws Exception {
        mvc.perform(get(BASE + "/stations").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void maximumLimit() throws Exception {
        mvc.perform(get(BASE + "/stations/ST-001/measurements").param("limit", "1000"))
                .andExpect(status().isOk());
        mvc.perform(get(BASE + "/stations/ST-001/measurements").param("limit", "1001"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/stations/ST-001/measurements").param("limit", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/stations/ST-001/measurements").param("limit", "2"))
                .andExpect(jsonPath("$.measurements", hasSize(2)));
    }
}
