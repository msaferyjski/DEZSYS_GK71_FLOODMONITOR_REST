package com.example.floodmonitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "stations")
public class StationList {
    @JsonProperty("stations")
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "station")
    private List<Station> stations = new ArrayList<>();

    public StationList() {}

    public StationList(List<Station> stations) {
        this.stations = stations;
    }

    public List<Station> getStations() { return stations; }
    public void setStations(List<Station> stations) { this.stations = stations; }
}
