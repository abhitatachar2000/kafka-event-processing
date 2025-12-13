package com.kafkaevents.orderproducer.service;

import com.kafkaevents.events.OrderEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

@Service
@RequiredArgsConstructor
public class OrdersGeneratorService {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Logger logger = LoggerFactory.getLogger(getClass());

    private volatile boolean running = false;

    public void generate(Integer rate, Integer duration) throws Exception {
        if (running) {
            throw new Exception("Order is already being generated");
        }
        running = true;
        long intervalNanos = 1_000_000_000L / rate;
        executor.submit(() -> {
            int orderID = 0;
            long endTime = System.currentTimeMillis() + duration * 1000L;
            long nextSendTime = System.nanoTime();
            while (running && System.currentTimeMillis() < endTime) {
                orderID = orderID + 1;
                OrderEvent event = OrderEventGenerator.generate(orderID);
                logger.info(String.format("Sending new order with id %s", orderID));
                kafkaTemplate.send(
                        "orders.raw",
                        String.valueOf(event.getProductId()),
                        event
                );
                logger.info(String.format("Sent new order with id %s", orderID));
                nextSendTime = nextSendTime + intervalNanos;
                long sleepTime = nextSendTime - System.nanoTime();

                if (sleepTime > 0) {
                    LockSupport.parkNanos(sleepTime);
                }
            }

            running = false;
        });
    }
}
