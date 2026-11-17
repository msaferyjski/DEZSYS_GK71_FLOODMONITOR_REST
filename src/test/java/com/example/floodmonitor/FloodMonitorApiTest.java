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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest(properties = {"polling.enabled=false", "api.viewer-key=test-viewer",
        "api.admin-key=test-admin", "station.key=test-station"})
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

    private MockHttpServletRequestBuilder read(String path) {
        return get(path).header("X-API-Key", "test-viewer");
    }

    private void save(String stationId, Instant timestamp, double waterLevel) {
        Station station = stationRepository.findById(stationId).orElseThrow();
        measurementRepository.save(new Measurement(stationId, timestamp, waterLevel, 700, 2, 14, 90,
                StationStatus.ONLINE, warningLevelService.calculate(station, waterLevel)));
    }

    @Test
    void stationsAsJson() throws Exception {
        mvc.perform(read(BASE + "/stations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stations", hasSize(5)))
                .andExpect(jsonPath("$.stations[0].id").value("ST-001"));
    }

    @Test
    void stationsAsXml() throws Exception {
        mvc.perform(read(BASE + "/stations").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("count(/stations/station)").number(5.0))
                .andExpect(xpath("/stations/station[1]/id").string("ST-001"));
    }

    @Test
    void existingStation() throws Exception {
        mvc.perform(read(BASE + "/stations/ST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ST-001"))
                .andExpect(jsonPath("$.river").value("Donau"));
    }

    @Test
    void unknownStation() throws Exception {
        mvc.perform(read(BASE + "/stations/ST-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value(BASE + "/stations/ST-999"));
    }

    @Test
    void latestMeasurement() throws Exception {
        mvc.perform(read(BASE + "/stations/ST-001/measurements/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value("ST-001"))
                .andExpect(jsonPath("$.timestamp").value("2026-05-01T12:00:00Z"))
                .andExpect(jsonPath("$.waterLevel").value(340.0));
    }

    @Test
    void filterByTimeRange() throws Exception {
        mvc.perform(read(BASE + "/stations/ST-001/measurements")
                        .param("from", "2026-05-01T09:00:00Z")
                        .param("to", "2026-05-01T11:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements", hasSize(3)))
                .andExpect(jsonPath("$.measurements[0].timestamp").value("2026-05-01T11:00:00Z"));
    }

    @Test
    void fromAfterTo() throws Exception {
        mvc.perform(read(BASE + "/stations/ST-001/measurements")
                        .param("from", "2026-05-01T12:00:00Z")
                        .param("to", "2026-05-01T08:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidDate() throws Exception {
        mvc.perform(read(BASE + "/stations/ST-001/measurements").param("from", "gestern"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidWarningLevel() throws Exception {
        mvc.perform(read(BASE + "/alerts").param("warningLevel", "PANIC"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void warningLevelNormal() throws Exception {
        save("ST-001", T0.plus(5, ChronoUnit.HOURS), 399);
        mvc.perform(read(BASE + "/stations/ST-001/measurements/latest"))
                .andExpect(jsonPath("$.warningLevel").value("NORMAL"));
    }

    @Test
    void warningLevelWarning() throws Exception {
        save("ST-001", T0.plus(5, ChronoUnit.HOURS), 400);
        mvc.perform(read(BASE + "/stations/ST-001/measurements/latest"))
                .andExpect(jsonPath("$.warningLevel").value("WARNING"));
    }

    @Test
    void warningLevelCritical() throws Exception {
        save("ST-001", T0.plus(5, ChronoUnit.HOURS), 550);
        mvc.perform(read(BASE + "/stations/ST-001/measurements/latest"))
                .andExpect(jsonPath("$.warningLevel").value("CRITICAL"));
    }

    @Test
    void unsupportedFormat() throws Exception {
        mvc.perform(read(BASE + "/stations").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void maximumLimit() throws Exception {
        mvc.perform(read(BASE + "/stations/ST-001/measurements").param("limit", "1000"))
                .andExpect(status().isOk());
        mvc.perform(read(BASE + "/stations/ST-001/measurements").param("limit", "1001"))
                .andExpect(status().isBadRequest());
        mvc.perform(read(BASE + "/stations/ST-001/measurements").param("limit", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(read(BASE + "/stations/ST-001/measurements").param("limit", "2"))
                .andExpect(jsonPath("$.measurements", hasSize(2)));
    }

    @Test
    void chronologicalPagesRespectFiltersAndBoundaries() throws Exception {
        String path = BASE + "/stations/ST-001/measurements";
        mvc.perform(read(path).param("limit", "2").param("page", "2").param("sort", "asc")
                        .param("from", "2026-05-01T09:00:00Z").param("warningLevel", "NORMAL"))
                .andExpect(jsonPath("$.page").value(2)).andExpect(jsonPath("$.limit").value(2))
                .andExpect(jsonPath("$.total").value(4)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.measurements[0].waterLevel").value(330));
        mvc.perform(read(path).accept(MediaType.APPLICATION_XML).param("limit", "2").param("page", "3"))
                .andExpect(xpath("/measurements/sort").string("desc"))
                .andExpect(xpath("/measurements/totalPages").number(3.0))
                .andExpect(xpath("count(/measurements/measurement)").number(1.0))
                .andExpect(xpath("/measurements/measurement/waterLevel").number(300.0));
        mvc.perform(read(path).param("page", "2147483647")).andExpect(jsonPath("$.measurements", hasSize(0)));
        mvc.perform(read(path).param("warningLevel", "CRITICAL")).andExpect(jsonPath("$.totalPages").value(0));
        mvc.perform(read(path).param("page", "0")).andExpect(status().isBadRequest());
        mvc.perform(read(path).param("sort", "random")).andExpect(status().isBadRequest());
    }

    @Test
    void credentialsAndRoles() throws Exception {
        String path = BASE + "/stations";
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("X-API-Key", "test-station")).andExpect(status().isUnauthorized());
        mvc.perform(delete(path + "/ST-001").header("X-API-Key", "test-viewer")).andExpect(status().isForbidden());
        String body = """
                {"id":"T-1","name":"Test","river":"Mur","location":{"latitude":47,"longitude":15},
                 "normalWaterLevel":10,"warningWaterLevel":20,"criticalWaterLevel":30,"isActive":true}
                """;
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-API-Key", "test-viewer")).andExpect(status().isForbidden());
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-API-Key", "test-admin")).andExpect(status().isCreated());
        mvc.perform(get(path + "/T-1").header("X-API-Key", "test-admin")).andExpect(status().isOk());
        mvc.perform(delete(path + "/T-1").header("X-API-Key", "test-admin")).andExpect(status().isNoContent());
    }

    @Test
    void openApiDescribesPaginationAndAuthentication() throws Exception {
        String params = "$.paths['/api/v1/stations/{stationId}/measurements'].get.parameters";
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.security[0].apiKey").isArray())
                .andExpect(jsonPath(params + "[?(@.name == 'page')].schema.type").value(contains("integer")))
                .andExpect(jsonPath(params + "[?(@.name == 'limit')].schema.type").value(contains("integer")))
                .andExpect(jsonPath("$.components.schemas.MeasurementPage.properties.totalPages.type").value("integer"));
    }

    @Test
    void overviewCombinesLatestReadings() throws Exception {
        save("ST-003", T0, 280);
        mvc.perform(read(BASE + "/measurements/latest"))
                .andExpect(jsonPath("$.measurements", hasSize(2)))
                .andExpect(jsonPath("$.measurements[0].waterLevel").value(340))
                .andExpect(jsonPath("$.measurements[1].stationId").value("ST-003"));
    }
}
