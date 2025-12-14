package com.kafkaevents.orderproducer.service;

import com.kafkaevents.events.OrderEvent;

import java.awt.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class OrderEventGenerator {
    public static Map<Integer, Double> products = Map.of(
            1, 10.99,
            2, 25.50,
            3, 7.25,
            4, 3.56,
            5, 12.13,
            6, 2.33
    );
    public static OrderEvent generate(int orderID) {
        Random random = new Random();
        int productID = random.nextInt(6) + 1;
        int quantity = random.nextInt(11);
        return new OrderEvent(
                orderID,
                productID,
                quantity,
                products.get(productID) * quantity,
                Instant.now()
        );
    }
}
