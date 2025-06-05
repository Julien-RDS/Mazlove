package com.mazlove.app.models;

import java.io.Serializable;
import java.util.List;
import com.codenameone.ui.util.TimeSlot;
import com.codenameone.ui.util.DateTime;

public class Provider implements Serializable {
    private String id;
    private String name;
    private String description;
    private float rating;
    private boolean verified;
    private List<TimeSlot> availability;
    private DateTime createdAt = new DateTime();

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    
    public List<TimeSlot> getAvailability() { return availability; }
    public void setAvailability(List<TimeSlot> availability) { this.availability = availability; }
    
    public DateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(DateTime createdAt) { this.createdAt = createdAt; }
}
