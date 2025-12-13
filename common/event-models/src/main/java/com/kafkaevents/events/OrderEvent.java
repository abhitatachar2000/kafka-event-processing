package com.kafkaevents.events;
import java.time.Instant;

public class OrdersEvent {
    private String orderId;
    private String productId;
    private int quantity;
    private double price;
    private Instant timestamp;
}
