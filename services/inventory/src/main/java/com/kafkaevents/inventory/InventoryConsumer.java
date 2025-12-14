package com.kafkaevents.inventory;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.kafkaevents.events.OrderEvent;
import com.kafkaevents.events.LowStockNotificationEvent;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.kafkaevents.events.InventoryUpdateEvent;

@Component
@RequiredArgsConstructor
public class InventoryConsumer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<Integer, Integer> inventory = new ConcurrentHashMap<>();

    @PostConstruct
    void initInventory() {
        inventory.put(1, 50);
        inventory.put(2, 40);
        inventory.put(3, 100);
        inventory.put(4, 30);
        inventory.put(5, 60);
        inventory.put(6, 80);
    }

    @KafkaListener (
            topics = "orders.validated",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "6"
    )
    public void consume(ConsumerRecord<String, OrderEvent> record, Acknowledgment ack) {
        OrderEvent order = record.value();
        Integer productId = order.getProductId();
        Integer quantity = order.getQuantity();
        logger.info(String.format("Processing order with id %s", order.getOrderId()));
        int currentStock = inventory.getOrDefault(productId, 0);

        if (currentStock >= quantity) {
            int updatedStock = currentStock - quantity;
            inventory.put(productId, updatedStock);

            InventoryUpdateEvent inventoryUpdateEvent = new InventoryUpdateEvent(
                    productId,
                    updatedStock,
                    Instant.now()
            );

            kafkaTemplate.send(
                    "inventory.updates",
                    String.valueOf(productId),
                    inventoryUpdateEvent
            );
            logger.info(String.format("Processed order with id %s", order.getOrderId()));

            if (updatedStock <= 5) {
                logger.info(String.format("Product %s has fewer than 5 items available: %s", productId, updatedStock));
                LowStockNotificationEvent lowStockNotificationEvent = new LowStockNotificationEvent(
                        productId,
                        updatedStock
                );
                kafkaTemplate.send(
                        "inventory.restock",
                        String.valueOf(productId),
                        lowStockNotificationEvent
                );
            }
        } else {
            logger.info(String.format("Product %s is out of stock. Rejecting order with id %s", productId, order.getOrderId()));
            kafkaTemplate.send(
                    "orders.rejected",
                    String.valueOf(productId),
                    order
            );
        }
        ack.acknowledge();
    }
}
