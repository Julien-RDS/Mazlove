package com.mazlove.app.models;

import java.io.Serializable;
import com.codenameone.ui.util.DateTime;

public class Session implements Serializable {
    private String id;
    private String providerId;
    private String start;
    private int duration;
    private DateTime createdAt = new DateTime();

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    
    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public DateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(DateTime createdAt) { this.createdAt = createdAt; }
}
