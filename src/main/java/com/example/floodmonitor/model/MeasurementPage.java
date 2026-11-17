package com.example.floodmonitor.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JacksonXmlRootElement(localName = "measurements")
public class MeasurementPage extends MeasurementList {
    @Schema(description = "Angeforderte Seite", minimum = "1")
    private final int page;
    @Schema(description = "Maximale Anzahl Messwerte pro Seite")
    private final int limit;
    @Schema(description = "Treffer nach Filterung, vor Seitenauswahl")
    private final int total;
    private final String sort;

    public MeasurementPage(List<Measurement> measurements, int page, int limit, int total, String sort) {
        super(measurements);
        this.page = page;
        this.limit = limit;
        this.total = total;
        this.sort = sort;
    }

    public int getPage() { return page; }
    public int getLimit() { return limit; }
    public int getTotal() { return total; }
    @Schema(description = "Gesamtzahl der Seiten, 0 bei leerem Ergebnis")
    public int getTotalPages() { return (int) Math.ceil((double) total / limit); }
    public String getSort() { return sort; }
}
