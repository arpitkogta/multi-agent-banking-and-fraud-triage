package com.aegis.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    
    @Id
    private String id;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "card_id", nullable = false)
    private String cardId;
    
    @Column(nullable = false)
    private String mcc;
    
    @Column(nullable = false)
    private String merchant;
    
    @Column(nullable = false)
    private Long amount; // Amount in smallest currency unit (paise for INR)
    
    @Column(nullable = false)
    private String currency = "INR";
    
    @Column(nullable = false)
    private OffsetDateTime ts;
    
    @Column(name = "device_id")
    private String deviceId;
    
    @Column(name = "geo_lat")
    private BigDecimal geoLat;
    
    @Column(name = "geo_lon")
    private BigDecimal geoLon;
    
    @Column(name = "geo_country")
    private String geoCountry;
    
    @Column(name = "geo_city")
    private String geoCity;
    
    @Column(nullable = false)
    private String status = "captured";
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;
    
    // Constructors
    public Transaction() {}
    
    public Transaction(String id, String customerId, String cardId, String mcc, 
                      String merchant, Long amount, String currency, OffsetDateTime ts) {
        this.id = id;
        this.customerId = customerId;
        this.cardId = cardId;
        this.mcc = mcc;
        this.merchant = merchant;
        this.amount = amount;
        this.currency = currency;
        this.ts = ts;
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
    
    public String getCardId() {
        return cardId;
    }
    
    public void setCardId(String cardId) {
        this.cardId = cardId;
    }
    
    public String getMcc() {
        return mcc;
    }
    
    public void setMcc(String mcc) {
        this.mcc = mcc;
    }
    
    public String getMerchant() {
        return merchant;
    }
    
    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }
    
    public Long getAmount() {
        return amount;
    }
    
    public void setAmount(Long amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public OffsetDateTime getTs() {
        return ts;
    }
    
    public void setTs(OffsetDateTime ts) {
        this.ts = ts;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public BigDecimal getGeoLat() {
        return geoLat;
    }
    
    public void setGeoLat(BigDecimal geoLat) {
        this.geoLat = geoLat;
    }
    
    public BigDecimal getGeoLon() {
        return geoLon;
    }
    
    public void setGeoLon(BigDecimal geoLon) {
        this.geoLon = geoLon;
    }
    
    public String getGeoCountry() {
        return geoCountry;
    }
    
    public void setGeoCountry(String geoCountry) {
        this.geoCountry = geoCountry;
    }
    
    public String getGeoCity() {
        return geoCity;
    }
    
    public void setGeoCity(String geoCity) {
        this.geoCity = geoCity;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
