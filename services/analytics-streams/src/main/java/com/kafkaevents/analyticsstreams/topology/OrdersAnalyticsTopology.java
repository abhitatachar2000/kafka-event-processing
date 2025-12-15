package com.kafkaevents.analyticsstreams.topology;

import java.time.Duration;
import java.time.Instant;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kafkaevents.events.InventoryUpdateEvent;
import com.kafkaevents.events.MetricEvent;
import com.kafkaevents.events.OrderEvent;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class OrdersAnalyticsTopology {

    private final Serde<OrderEvent> orderEventSerde;
    private final Serde<InventoryUpdateEvent> inventoryUpdateEventSerde;
    private final Serde<MetricEvent> metricEventSerde;

    @Bean
    public KStream<String, OrderEvent> ordersPerProductPerDay(StreamsBuilder builder) {
        KStream<String, OrderEvent> ordersStream = builder.stream(
                "orders.validated",
                Consumed.with(Serdes.String(), this.orderEventSerde)
        );

        ordersStream.groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(1)))
                .count(Materialized.with(Serdes.String(), Serdes.Long()))
                .toStream()
                .map((windowedKey, count) ->
                    new KeyValue<>(
                            windowedKey.key() + "_" + String.valueOf(Instant.ofEpochMilli(windowedKey.window().start())),
                            new MetricEvent(
                                    "ORDERS_PER_PRODUCT_DAILY",
                                    windowedKey.key() + "_" + String.valueOf(Instant.ofEpochMilli(windowedKey.window().start())),
                                    count,
                                    Instant.ofEpochMilli(windowedKey.window().start())
                            )
                    )
                ).to("metrics.output", Produced.with(Serdes.String(), metricEventSerde));
        return ordersStream;
    }

    @Bean
    public KStream<String, OrderEvent> revenuePerProduct(StreamsBuilder builder){
        KStream<String, OrderEvent> acceptedOrderStream = builder.stream(
                "orders.accepted",
                Consumed.with(Serdes.String(), this.orderEventSerde)
        );

        acceptedOrderStream.groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(1)))
                .aggregate(() -> 0D, (key, value, aggregate) -> aggregate + value.getPrice(), Materialized.with(Serdes.String(), Serdes.Double()))
                .toStream()
                .map((windowedKey, total) ->
                        new KeyValue<>(
                                windowedKey.key() + "_" + String.valueOf(Instant.ofEpochMilli(windowedKey.window().start())),  // Include window start for uniqueness
                                new MetricEvent(
                                        "REVENUE_PER_PRODUCT_DAILY",
                                        windowedKey.key() + "_" + String.valueOf(Instant.ofEpochMilli(windowedKey.window().start())),
                                        total,
                                        Instant.ofEpochMilli(windowedKey.window().start())  // Use window start time
                                )
                        )
                ).to("metrics.output", Produced.with(Serdes.String(), metricEventSerde));
        return acceptedOrderStream;
    }

    @Bean
    public KTable<String, InventoryUpdateEvent> outOfStockList(StreamsBuilder builder) {
        KTable<String, InventoryUpdateEvent> inventoryUpdateEventKTable = builder.table(
                "inventory.updates",
                Consumed.with(Serdes.String(), this.inventoryUpdateEventSerde),
                Materialized.as("inventory-store")
        );

        inventoryUpdateEventKTable.toStream()
                .filter((key, value) -> value.getStock() == 0)
                .mapValues(value -> new MetricEvent(
                        "STOCK_EMPTY",
                        String.valueOf(value.getProductID()),
                        0,
                        value.getTimestamp()
                )).to("metrics.output", Produced.with(Serdes.String(), metricEventSerde));
        return inventoryUpdateEventKTable;
    }
}
