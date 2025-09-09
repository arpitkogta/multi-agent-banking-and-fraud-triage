package com.aegis.controller;

import com.aegis.agent.AgentOrchestrator;
import com.aegis.dto.TriageRequest;
import com.aegis.dto.TriageResponse;
import com.aegis.metrics.MetricsService;
import com.aegis.service.PiiRedactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/triage")
public class TriageController {
    
    private static final Logger logger = LoggerFactory.getLogger(TriageController.class);
    
    @Autowired
    private AgentOrchestrator agentOrchestrator;
    
    @Autowired
    private PiiRedactionService piiRedactionService;
    
    @Autowired
    private MetricsService metricsService;
    
    // Simple rate limiting (in production, use Redis)
    private final Map<String, Long> requestTimes = new java.util.concurrent.ConcurrentHashMap<>();
    private final int RATE_LIMIT_WINDOW_MS = 1000; // 1 second
    private final int MAX_REQUESTS_PER_WINDOW = 5;
    
    /**
     * POST /api/triage - Execute fraud triage workflow
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> executeTriage(@Valid @RequestBody TriageRequest request) {
        String requestId = UUID.randomUUID().toString();
        String maskedCustomerId = piiRedactionService.maskCustomerId(request.getCustomerId());
        
        // Rate limiting check
        String clientId = request.getCustomerId(); // In production, use session ID or IP
        if (isRateLimited(clientId)) {
            metricsService.recordRateLimitBlock();
            logger.warn("Rate limit exceeded for clientId={}", clientId);
            
            Map<String, Object> rateLimitResponse = new java.util.HashMap<>();
            rateLimitResponse.put("error", "Rate limit exceeded");
            rateLimitResponse.put("retryAfterMs", RATE_LIMIT_WINDOW_MS);
            rateLimitResponse.put("requestId", requestId);
            
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(rateLimitResponse);
        }
        
        logger.info("Triage request received: requestId={}, customerId={}, txnId={}", 
                   requestId, maskedCustomerId, request.getSuspectTxnId());
        
        try {
            TriageResponse response = agentOrchestrator.executeTriage(request);
            response.setRequestId(requestId);
            
            logger.info("Triage completed: requestId={}, riskScore={}, action={}", 
                       requestId, response.getRiskScore(), response.getRecommendedAction());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error executing triage for requestId={}", requestId, e);
            
            TriageResponse errorResponse = new TriageResponse(requestId, request.getCustomerId(), request.getSuspectTxnId());
            errorResponse.setRiskScore("medium");
            errorResponse.setRecommendedAction("contact_customer");
            errorResponse.setReasons(java.util.Arrays.asList("system_error", "manual_review_required"));
            errorResponse.setFallbackUsed(true);
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Check if client is rate limited
     */
    private boolean isRateLimited(String clientId) {
        long currentTime = System.currentTimeMillis();
        Long lastRequestTime = requestTimes.get(clientId);
        
        if (lastRequestTime == null || currentTime - lastRequestTime > RATE_LIMIT_WINDOW_MS) {
            requestTimes.put(clientId, currentTime);
            return false;
        }
        
        // Simple rate limiting - in production, use proper token bucket
        return true;
    }
    
    /**
     * POST /api/triage/stream - Execute fraud triage workflow with SSE streaming
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeTriageStream(@Valid @RequestBody TriageRequest request) {
        String requestId = UUID.randomUUID().toString();
        String maskedCustomerId = piiRedactionService.maskCustomerId(request.getCustomerId());
        
        logger.info("Streaming triage request received: requestId={}, customerId={}", 
                   requestId, maskedCustomerId);
        
        SseEmitter emitter = new SseEmitter(30000L); // 30 second timeout
        
        CompletableFuture.runAsync(() -> {
            try {
                // Send plan built event
                sendSseEvent(emitter, "plan_built", Map.of(
                    "requestId", requestId,
                    "steps", java.util.Arrays.asList("getProfile", "getRecentTransactions", "riskSignals", "kbLookup", "decide", "proposeAction")
                ));
                
                // Execute triage workflow
                TriageResponse response = agentOrchestrator.executeTriage(request);
                response.setRequestId(requestId);
                
                // Send tool update events
                Map<String, Object> traceData = response.getTraceData();
                if (traceData != null) {
                    for (Map.Entry<String, Object> entry : traceData.entrySet()) {
                        if (entry.getKey().startsWith("step_")) {
                            sendSseEvent(emitter, "tool_update", Map.of(
                                "requestId", requestId,
                                "step", entry.getKey(),
                                "result", entry.getValue()
                            ));
                        }
                    }
                }
                
                // Send fallback event if used
                if (response.isFallbackUsed()) {
                    sendSseEvent(emitter, "fallback_triggered", Map.of(
                        "requestId", requestId,
                        "reason", "Service timeout or error"
                    ));
                }
                
                // Send final decision
                sendSseEvent(emitter, "decision_finalized", Map.of(
                    "requestId", requestId,
                    "riskScore", response.getRiskScore(),
                    "recommendedAction", response.getRecommendedAction(),
                    "reasons", response.getReasons(),
                    "requiresOTP", response.isRequiresOTP()
                ));
                
                emitter.complete();
                
            } catch (Exception e) {
                logger.error("Error in streaming triage for requestId={}", requestId, e);
                try {
                    sendSseEvent(emitter, "error", Map.of(
                        "requestId", requestId,
                        "error", e.getMessage()
                    ));
                    emitter.completeWithError(e);
                } catch (IOException ioException) {
                    logger.error("Error sending error event", ioException);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * Sends an SSE event
     */
    private void sendSseEvent(SseEmitter emitter, String eventName, Map<String, Object> data) throws IOException {
        SseEmitter.SseEventBuilder event = SseEmitter.event()
            .name(eventName)
            .data(data);
        emitter.send(event);
    }
}
