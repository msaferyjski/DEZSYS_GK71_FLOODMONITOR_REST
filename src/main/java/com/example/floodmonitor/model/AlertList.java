package com.example.floodmonitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "alerts")
public class AlertList {
    @JsonProperty("alerts")
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "alert")
    private List<Alert> alerts = new ArrayList<>();

    public AlertList() {}

    public AlertList(List<Alert> alerts) {
        this.alerts = alerts;
    }

    public List<Alert> getAlerts() { return alerts; }
    public void setAlerts(List<Alert> alerts) { this.alerts = alerts; }
}
