package com.kafkaevents.analyticsstreams.topology;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.common.serialization.Serde;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.kafkaevents.events.InventoryUpdateEvent;
import com.kafkaevents.events.MetricEvent;
import com.kafkaevents.events.OrderEvent;

@Configuration
public class SerdeConfig {
    @Bean
    public Serde<OrderEvent> orderEventSerde() {
        JsonSerde<OrderEvent> jsonSerde = new JsonSerde<>(OrderEvent.class);
        Map<String, Object> props = new HashMap<>();
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.kafkaevents.events");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderEvent.class);

        jsonSerde.configure(props, false);
        return jsonSerde;
    }

    @Bean
    public Serde<InventoryUpdateEvent> inventoryUpdateEventSerde() {
        JsonSerde<InventoryUpdateEvent> jsonSerde = new JsonSerde<>(InventoryUpdateEvent.class);
        Map<String, Object> props = new HashMap<>();
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.kafkaevents.events");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, InventoryUpdateEvent.class);

        jsonSerde.configure(props, false);
        return jsonSerde;
    }

    @Bean
    public Serde<MetricEvent> metricEventSerde() {
        JsonSerde<MetricEvent> jsonSerde = new JsonSerde<>(MetricEvent.class);
        Map<String, Object> props = new HashMap<>();
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.kafkaevents.events");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MetricEvent.class);

        jsonSerde.configure(props, false);
        return jsonSerde;
    }
}
