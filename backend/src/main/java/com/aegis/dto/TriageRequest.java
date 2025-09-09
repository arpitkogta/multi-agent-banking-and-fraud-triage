package com.aegis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TriageRequest {
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    private String suspectTxnId; // Optional for KB FAQ cases
    
    private String alertType;
    private String userMessage;
    
    // Constructors
    public TriageRequest() {}
    
    public TriageRequest(String customerId, String suspectTxnId, String alertType, String userMessage) {
        this.customerId = customerId;
        this.suspectTxnId = suspectTxnId;
        this.alertType = alertType;
        this.userMessage = userMessage;
    }
    
    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getSuspectTxnId() {
        return suspectTxnId;
    }
    
    public void setSuspectTxnId(String suspectTxnId) {
        this.suspectTxnId = suspectTxnId;
    }
    
    public String getAlertType() {
        return alertType;
    }
    
    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
}
