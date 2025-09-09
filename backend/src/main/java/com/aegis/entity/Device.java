package com.aegis.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "devices")
public class Device {
    
    @Id
    private String id;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "device_type", nullable = false)
    private String deviceType;
    
    private String os;
    private String browser;
    private String fingerprint;
    
    @Column(name = "last_seen")
    private OffsetDateTime lastSeen;
    
    @Column(name = "is_trusted", nullable = false)
    private Boolean isTrusted = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;
    
    // Constructors
    public Device() {}
    
    public Device(String id, String customerId, String deviceType, String os, String browser) {
        this.id = id;
        this.customerId = customerId;
        this.deviceType = deviceType;
        this.os = os;
        this.browser = browser;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getDeviceType() {
        return deviceType;
    }
    
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
    
    public String getOs() {
        return os;
    }
    
    public void setOs(String os) {
        this.os = os;
    }
    
    public String getBrowser() {
        return browser;
    }
    
    public void setBrowser(String browser) {
        this.browser = browser;
    }
    
    public String getFingerprint() {
        return fingerprint;
    }
    
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
    
    public OffsetDateTime getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(OffsetDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public Boolean getIsTrusted() {
        return isTrusted;
    }
    
    public void setIsTrusted(Boolean isTrusted) {
        this.isTrusted = isTrusted;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
