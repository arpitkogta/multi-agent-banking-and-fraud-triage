package com.aegis.agent;

import com.aegis.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RiskAgent extends BaseAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskAgent.class);
    private static final double TIMEOUT_PROBABILITY = 0.1;
    private static final int TIMEOUT_DURATION = 1500; // 1.5 seconds
    
    @Autowired
    private MetricsService metricsService;
    
    public Map<String, Object> analyzeRiskSignals(String customerId, String suspectTxnId) {
        logger.debug("Analyzing risk signals for customerId={}, txnId={}", customerId, suspectTxnId);
        
        if (!isValidInput(customerId, suspectTxnId)) {
            return createErrorResponse("Invalid input parameters");
        }
        
        try {
            // Simulate timeout with 10% probability
            if (Math.random() < TIMEOUT_PROBABILITY) {
                Thread.sleep(TIMEOUT_DURATION);
                throw new RuntimeException("Risk service timeout");
            }
            
            List<String> riskFactors = new ArrayList<>();
            String riskScore = "low";
            
            // Check for high-risk patterns
            if (suspectTxnId.contains("unauthorized") || suspectTxnId.contains("fraud")) {
                riskScore = "high";
                riskFactors.add("unauthorized_transaction");
                riskFactors.add("fraud_pattern");
            } else if (suspectTxnId.contains("geo") || suspectTxnId.contains("velocity")) {
                riskScore = "high";
                riskFactors.add("geo_velocity_violation");
                riskFactors.add("impossible_travel");
            } else if (suspectTxnId.contains("device") || suspectTxnId.contains("mcc")) {
                riskScore = "medium";
                riskFactors.add("device_change");
                riskFactors.add("mcc_anomaly");
            } else if (suspectTxnId.contains("duplicate")) {
                riskScore = "low";
                riskFactors.add("duplicate_transaction");
                riskFactors.add("preauth_capture");
            } else {
                riskScore = "medium";
                riskFactors.add("unusual_pattern");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("riskScore", riskScore);
            data.put("reasons", riskFactors);
            data.put("confidence", 0.85);
            data.put("analysisTime", System.currentTimeMillis());
            data.put("fallbackUsed", false);
            
            return createSuccessResponse(data);
            
        } catch (Exception e) {
            logger.warn("Risk analysis failed for customerId={}: {}", customerId, e.getMessage());
            
            // Track fallback usage with metrics
            metricsService.recordAgentFallback("risk_agent");
            
            // Use rule-based fallback with reduced risk score
            Map<String, Object> fallbackData = new HashMap<>();
            fallbackData.put("fallbackUsed", true);
            fallbackData.put("riskScore", "medium"); // Always downgrade to medium on fallback
            fallbackData.put("reasons", Arrays.asList("risk_unavailable", "rule_based_fallback"));
            fallbackData.put("confidence", 0.6);
            fallbackData.put("analysisTime", System.currentTimeMillis());
            
            // Emit SSE event for monitoring
            emitEvent("fallback_triggered", Map.of(
                "customerId", customerId,
                "txnId", suspectTxnId,
                "reason", e.getMessage()
            ));
            
            return createSuccessResponse(fallbackData);
        }
    }

    private void emitEvent(String eventType, Map<String, Object> data) {
        // Implementation would integrate with SSE event emitter
    }


}
