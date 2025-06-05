package com.mazlove.app.models;

import java.io.Serializable;
import com.codenameone.ui.util.DateTime;

public class ServiceItem implements Serializable {
    private String id;
    private String name;
    private String description;
    private float price;
    private DateTime createdAt = new DateTime();

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public float getPrice() { return price; }
    public void setPrice(float price) { this.price = price; }
    
    public DateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(DateTime createdAt) { this.createdAt = createdAt; }
}
