package com.kafkaevents.metrics.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kafkaevents.events.MetricEvent;

import lombok.Getter;

@Service
public class MetricsService {
    @Getter
    private final Map<String, Number> ordersPerMinute = new HashMap<>();

    @Getter
    private final Map<String, Number> ordersPerProductDaily = new HashMap<>();

    @Getter
    private final Map<String, Number> revenuePerProductDaily = new HashMap<>();

    @Getter
    private final Map<String, Instant> outOfStock = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String ORDERS_PER_MIN = "ORDERS_PER_MIN";
    private static final String ORDERS_PER_PRODUCT_DAILY = "ORDERS_PER_PRODUCT_DAILY";
    private static final String REVENUE_PER_PRODUCT_DAILY = "REVENUE_PER_PRODUCT_DAILY";
    private static final String STOCK_EMPTY = "STOCK_EMPTY";

    public void update(MetricEvent metricEvent) {
        switch (metricEvent.getMetricName()) {
            case ORDERS_PER_MIN:
                ordersPerMinute.put(metricEvent.getMetricKey(), metricEvent.getMetricValue());
                break;
            case ORDERS_PER_PRODUCT_DAILY:
                ordersPerProductDaily.put(metricEvent.getMetricKey(), metricEvent.getMetricValue());
                break;
            case REVENUE_PER_PRODUCT_DAILY:
                revenuePerProductDaily.put(metricEvent.getMetricKey(), metricEvent.getMetricValue());
                break;
            case STOCK_EMPTY:
                outOfStock.put(metricEvent.getMetricKey(), metricEvent.getTimestamp());
                break;
            default:
                logger.error(String.format("Undefined metric: %s", metricEvent.getMetricName()));
                break;
        }
    }

}
