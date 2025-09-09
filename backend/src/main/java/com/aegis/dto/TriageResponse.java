package com.aegis.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class TriageResponse {
    
    private String requestId;
    private String customerId;
    private String suspectTxnId;
    private String riskScore; // low, medium, high
    private String recommendedAction;
    private List<String> reasons;
    private boolean requiresOTP;
    private boolean fallbackUsed;
    private Map<String, Object> traceData;
    private OffsetDateTime completedAt;
    
    // Constructors
    public TriageResponse() {}
    
    public TriageResponse(String requestId, String customerId, String suspectTxnId) {
        this.requestId = requestId;
        this.customerId = customerId;
        this.suspectTxnId = suspectTxnId;
    }
    
    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
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
    
    public String getRiskScore() {
        return riskScore;
    }
    
    public void setRiskScore(String riskScore) {
        this.riskScore = riskScore;
    }
    
    public String getRecommendedAction() {
        return recommendedAction;
    }
    
    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }
    
    public List<String> getReasons() {
        return reasons;
    }
    
    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
    
    public boolean isRequiresOTP() {
        return requiresOTP;
    }
    
    public void setRequiresOTP(boolean requiresOTP) {
        this.requiresOTP = requiresOTP;
    }
    
    public boolean isFallbackUsed() {
        return fallbackUsed;
    }
    
    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }
    
    public Map<String, Object> getTraceData() {
        return traceData;
    }
    
    public void setTraceData(Map<String, Object> traceData) {
        this.traceData = traceData;
    }
    
    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
