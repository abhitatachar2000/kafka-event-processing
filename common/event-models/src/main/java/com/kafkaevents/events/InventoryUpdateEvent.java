package com.kafkaevents.events;

import java.time.Instant;

public class InventoryUpdateEvent {
    private int productID;
    private int stock;
    private Instant timestamp;

    public InventoryUpdateEvent(int productID, int stock, Instant timestamp) {
        this.productID = productID;
        this.stock = stock;
        this.timestamp = timestamp;
    }

    public InventoryUpdateEvent() {}

    public int getProductID() {
        return productID;
    }

    public void setProductID(int productID) {
        this.productID = productID;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
