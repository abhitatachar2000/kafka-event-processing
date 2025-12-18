package com.kafkaevents.metrics.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kafkaevents.metrics.service.MetricsService;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricsController {

    private MetricsService metricsService;

    @Autowired
    public MetricsController(MetricsService metricsService){
        this.metricsService = metricsService;
    }

    @GetMapping()
    public ResponseEntity<?> produceMetrics(@RequestParam String metricName) throws RuntimeException {
        try {
            switch (metricName) {
                case "ordersPerProductPerDay":
                    return ResponseEntity.status(HttpStatus.OK).body(metricsService.getOrdersPerProductDaily());
                case "revenuePerProductPerDay":
                    return ResponseEntity.status(HttpStatus.OK).body(metricsService.getRevenuePerProductDaily());
                case "outOfStockProducts":
                    return ResponseEntity.status(HttpStatus.OK).body(metricsService.getOutOfStock());
                default:
                    throw new RuntimeException("Invalid metrics request");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
