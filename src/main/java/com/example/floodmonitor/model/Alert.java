package com.example.floodmonitor.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.Instant;

@JacksonXmlRootElement(localName = "alert")
public class Alert {
    private String stationId;
    private String stationName;
    private String river;
    private Instant timestamp;
    private Double waterLevel;
    private double warningWaterLevel;
    private double criticalWaterLevel;
    private WarningLevel warningLevel;

    public Alert() {}

    public Alert(String stationId, String stationName, String river, Instant timestamp, Double waterLevel,
                 double warningWaterLevel, double criticalWaterLevel, WarningLevel warningLevel) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.river = river;
        this.timestamp = timestamp;
        this.waterLevel = waterLevel;
        this.warningWaterLevel = warningWaterLevel;
        this.criticalWaterLevel = criticalWaterLevel;
        this.warningLevel = warningLevel;
    }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getRiver() { return river; }
    public void setRiver(String river) { this.river = river; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public Double getWaterLevel() { return waterLevel; }
    public void setWaterLevel(Double waterLevel) { this.waterLevel = waterLevel; }
    public double getWarningWaterLevel() { return warningWaterLevel; }
    public void setWarningWaterLevel(double warningWaterLevel) { this.warningWaterLevel = warningWaterLevel; }
    public double getCriticalWaterLevel() { return criticalWaterLevel; }
    public void setCriticalWaterLevel(double criticalWaterLevel) { this.criticalWaterLevel = criticalWaterLevel; }
    public WarningLevel getWarningLevel() { return warningLevel; }
    public void setWarningLevel(WarningLevel warningLevel) { this.warningLevel = warningLevel; }
}
