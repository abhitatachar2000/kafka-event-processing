package com.kafkaevents.events;

public class LowStockNotificationEvent {
    private int productId;
    private int currentStock;

    public LowStockNotificationEvent() {}

    public LowStockNotificationEvent(int productId, int currentStock) {
        this.productId = productId;
        this.currentStock = currentStock;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }
}
