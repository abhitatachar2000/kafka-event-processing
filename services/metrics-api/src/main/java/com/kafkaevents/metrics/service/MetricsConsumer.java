package com.kafkaevents.metrics.service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.kafkaevents.events.MetricEvent;

@Component
@RequiredArgsConstructor
public class MetricsConsumer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final MetricsService metricsService;

    @KafkaListener(
            topics = "metrics.output",
            concurrency = "6",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            ConsumerRecord<String, MetricEvent> record,
            Acknowledgment ack
    ) {
        MetricEvent metricEvent = record.value();
        metricsService.update(metricEvent);
        ack.acknowledge();
    }

}
