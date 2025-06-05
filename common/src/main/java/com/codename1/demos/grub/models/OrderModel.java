package com.codename1.demos.grub.models;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.util.ArrayList;
import java.util.List;

/**
 * Modèle représentant une commande
 */
public class OrderModel {
    private String orderId;
    private String restaurantId;
    private String userId;
    private List<OrderItem> items;
    private double totalAmount;
    private OrderStatus status;
    private long orderDate;
    private long deliveryDate;
    private String deliveryAddress;
    private String deliveryInstructions;
    
    public OrderModel(String orderId, String restaurantId, String userId) {
        this.orderId = orderId;
        this.restaurantId = restaurantId;
        this.userId = userId;
        this.items = new ArrayList<>();
        this.status = OrderStatus.PENDING;
        this.orderDate = System.currentTimeMillis();
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getRestaurantId() {
        return restaurantId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public List<OrderItem> getItems() {
        return items;
    }
    
    public void addItem(OrderItem item) {
        items.add(item);
        recalculateTotal();
    }
    
    public void removeItem(OrderItem item) {
        items.remove(item);
        recalculateTotal();
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public long getOrderDate() {
        return orderDate;
    }
    
    public long getDeliveryDate() {
        return deliveryDate;
    }
    
    public void setDeliveryDate(long deliveryDate) {
        this.deliveryDate = deliveryDate;
    }
    
    public String getDeliveryAddress() {
        return deliveryAddress;
    }
    
    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    
    public String getDeliveryInstructions() {
        return deliveryInstructions;
    }
    
    public void setDeliveryInstructions(String deliveryInstructions) {
        this.deliveryInstructions = deliveryInstructions;
    }
    
    private void recalculateTotal() {
        totalAmount = items.stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();
    }
    
    public static class OrderItem {
        private String dishId;
        private String name;
        private double price;
        private int quantity;
        
        public OrderItem(String dishId, String name, double price, int quantity) {
            this.dishId = dishId;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }
        
        public String getDishId() {
            return dishId;
        }
        
        public String getName() {
            return name;
        }
        
        public double getPrice() {
            return price;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
    
    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PREPARING,
        READY_FOR_DELIVERY,
        DELIVERED,
        CANCELLED
    }
}
