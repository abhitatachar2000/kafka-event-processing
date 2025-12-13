package com.kafkaevents.events;
import java.time.Instant;

public class OrderEvent {
    private String orderId;
    private String productId;
    private int quantity;
    private double price;
    private Instant timestamp;
}
