package com.aegis.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "email_masked", nullable = false)
    private String emailMasked;
    
    @Column(name = "risk_flags", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> riskFlags;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updatedAt;
    
    @Column(nullable = false)
    private String status = "active";
    
    // Constructors
    public Customer() {}
    
    public Customer(String id, String name, String emailMasked, List<String> riskFlags) {
        this.id = id;
        this.name = name;
        this.emailMasked = emailMasked;
        this.riskFlags = riskFlags;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmailMasked() {
        return emailMasked;
    }
    
    public void setEmailMasked(String emailMasked) {
        this.emailMasked = emailMasked;
    }
    
    public List<String> getRiskFlags() {
        return riskFlags;
    }
    
    public void setRiskFlags(List<String> riskFlags) {
        this.riskFlags = riskFlags;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
