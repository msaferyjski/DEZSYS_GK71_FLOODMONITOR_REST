package com.example.floodmonitor.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.Instant;

@JacksonXmlRootElement(localName = "statistics")
public class Statistics {
    private String stationId;
    private Instant from;
    private Instant to;
    private Double minWaterLevel;
    private Double maxWaterLevel;
    private Double avgWaterLevel;
    private Double avgFlowRate;
    private double totalRainfall;
    private int measurementCount;
    private int warningCount;
    private int criticalCount;

    public Statistics() {}

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }
    public Instant getFrom() { return from; }
    public void setFrom(Instant from) { this.from = from; }
    public Instant getTo() { return to; }
    public void setTo(Instant to) { this.to = to; }
    public Double getMinWaterLevel() { return minWaterLevel; }
    public void setMinWaterLevel(Double minWaterLevel) { this.minWaterLevel = minWaterLevel; }
    public Double getMaxWaterLevel() { return maxWaterLevel; }
    public void setMaxWaterLevel(Double maxWaterLevel) { this.maxWaterLevel = maxWaterLevel; }
    public Double getAvgWaterLevel() { return avgWaterLevel; }
    public void setAvgWaterLevel(Double avgWaterLevel) { this.avgWaterLevel = avgWaterLevel; }
    public Double getAvgFlowRate() { return avgFlowRate; }
    public void setAvgFlowRate(Double avgFlowRate) { this.avgFlowRate = avgFlowRate; }
    public double getTotalRainfall() { return totalRainfall; }
    public void setTotalRainfall(double totalRainfall) { this.totalRainfall = totalRainfall; }
    public int getMeasurementCount() { return measurementCount; }
    public void setMeasurementCount(int measurementCount) { this.measurementCount = measurementCount; }
    public int getWarningCount() { return warningCount; }
    public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
    public int getCriticalCount() { return criticalCount; }
    public void setCriticalCount(int criticalCount) { this.criticalCount = criticalCount; }
}
