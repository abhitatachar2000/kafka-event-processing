package com.kafkaevents.ordervalidator;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.kafkaevents.events.OrderEvent;

@Component
public class OrderValidationConsumer {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public OrderValidationConsumer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "orders.raw",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "6"
    )
    public void consume(
            ConsumerRecord<String, OrderEvent> record,
            Acknowledgment ack
    ) {
        OrderEvent order = record.value();
        try {
            validate(order);
            logger.info(String.format("Order with id %s is valid", order.getOrderId()));
            kafkaTemplate.send(
                    "orders.validated",
                    String.valueOf(order.getProductId()),
                    order
            );
            ack.acknowledge();
        } catch (Exception e) {
            logger.info(String.format("Order with id %s is invalid", order.getOrderId()));
            kafkaTemplate.send(
                    "orders.dlq",
                    String.valueOf(order.getProductId()),
                    order
            );
            ack.acknowledge();
        }
    }

    private void validate(OrderEvent order) throws Exception {
        if (order.getQuantity() == 0 ||
            order.getPrice() == 0) {
            throw new Exception("Invalid Order");
        }
    }


}
