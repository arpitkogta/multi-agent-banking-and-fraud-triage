package com.aegis.dto;

public class IngestResponse {
    
    private boolean accepted;
    private int count;
    private String requestId;
    private String message;
    
    // Constructors
    public IngestResponse() {}
    
    public IngestResponse(boolean accepted, int count, String requestId) {
        this.accepted = accepted;
        this.count = count;
        this.requestId = requestId;
    }
    
    public IngestResponse(boolean accepted, int count, String requestId, String message) {
        this.accepted = accepted;
        this.count = count;
        this.requestId = requestId;
        this.message = message;
    }
    
    // Getters and Setters
    public boolean isAccepted() {
        return accepted;
    }
    
    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
