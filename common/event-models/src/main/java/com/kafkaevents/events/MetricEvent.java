package com.kafkaevents.events;

import java.time.Instant;

public class MetricEvent {
    private String metricName;
    private String metricKey;
    private Number metricValue;
    private Instant timestamp;

    public MetricEvent() {}

    public MetricEvent(String metricName, String metricKey, Number metricValue, Instant timestamp) {
        this.metricName = metricName;
        this.metricKey = metricKey;
        this.metricValue = metricValue;
        this.timestamp = timestamp;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getMetricKey() {
        return metricKey;
    }

    public void setMetricKey(String metricKey) {
        this.metricKey = metricKey;
    }

    public Number getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(Number metricValue) {
        this.metricValue = metricValue;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

}
