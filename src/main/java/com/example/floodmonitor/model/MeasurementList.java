package com.example.floodmonitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "measurements")
public class MeasurementList {
    @JsonProperty("measurements")
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "measurement")
    private List<Measurement> measurements = new ArrayList<>();

    public MeasurementList() {}

    public MeasurementList(List<Measurement> measurements) {
        this.measurements = measurements;
    }

    public List<Measurement> getMeasurements() { return measurements; }
    public void setMeasurements(List<Measurement> measurements) { this.measurements = measurements; }
}
