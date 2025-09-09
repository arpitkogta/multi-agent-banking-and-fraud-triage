package com.aegis.controller;

import com.aegis.agent.ComplianceAgent;
import com.aegis.metrics.MetricsService;
import com.aegis.service.PiiRedactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/action")
public class ActionsController {
    
    private static final Logger logger = LoggerFactory.getLogger(ActionsController.class);
    
    @Autowired
    private ComplianceAgent complianceAgent;
    
    @Autowired
    private PiiRedactionService piiRedactionService;
    
    @Autowired
    private MetricsService metricsService;
    
    /**
     * POST /api/action/freeze-card - Freeze a card
     */
    @PostMapping("/freeze-card")
    public ResponseEntity<Map<String, Object>> freezeCard(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        String requestId = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        String cardId = (String) request.get("cardId");
        String otp = (String) request.get("otp");
        
        String maskedCardId = cardId != null ? "****" + cardId.substring(Math.max(0, cardId.length() - 4)) : "****";
        logger.info("Freeze card request: requestId={}, cardId={}, hasOTP={}", 
                   requestId, maskedCardId, otp != null);
        
        try {
            if (apiKey == null || !isValidApiKey(apiKey)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid API key"));
            }
            
            Map<String, Object> context = new HashMap<>();
            context.put("cardId", cardId);
            context.put("otp", otp);
            
            Map<String, Object> validation = complianceAgent.validateAction("freeze_card", cardId, context);
            
            if (!(Boolean) validation.get("isCompliant")) {
                logger.warn("Freeze card blocked for cardId={}, violations: {}", 
                           maskedCardId, validation.get("violations"));
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "BLOCKED");
                response.put("reason", "Policy violation");
                response.put("violations", validation.get("violations"));
                response.put("requestId", requestId);
                
                return ResponseEntity.ok(response);
            }
            
            Map<String, Object> requirements = (Map<String, Object>) validation.get("requirements");
            boolean otpRequired = (Boolean) requirements.getOrDefault("otpRequired", false);
            
            if (otpRequired && otp == null) {
                logger.info("OTP required for freeze card: cardId={}", maskedCardId);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "PENDING_OTP");
                response.put("message", "OTP required to freeze card");
                response.put("requestId", requestId);
                
                return ResponseEntity.ok(response);
            }
            
            if (otpRequired && !isValidOTP(otp)) {
                logger.warn("Invalid OTP for freeze card: cardId={}", maskedCardId);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "INVALID_OTP");
                response.put("message", "Invalid OTP provided");
                response.put("requestId", requestId);
                
                return ResponseEntity.ok(response);
            }
            
            boolean success = executeFreezeCard(cardId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", success ? "FROZEN" : "FAILED");
            response.put("cardId", cardId);
            response.put("requestId", requestId);
            response.put("timestamp", java.time.OffsetDateTime.now());
            
            logger.info("Card freeze {} for cardId={}", success ? "successful" : "failed", maskedCardId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error freezing card for cardId={}", maskedCardId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to freeze card"));
        }
    }
    
    /**
     * POST /api/action/open-dispute - Open a dispute
     */
    @PostMapping("/open-dispute")
    public ResponseEntity<Map<String, Object>> openDispute(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        String requestId = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        String txnId = (String) request.get("txnId");
        String reasonCode = (String) request.get("reasonCode");
        Boolean confirm = (Boolean) request.getOrDefault("confirm", false);
        
        logger.info("Open dispute request: requestId={}, txnId={}, reasonCode={}, confirm={}", 
                   requestId, txnId, reasonCode, confirm);
        
        try {
            if (apiKey == null || !isValidApiKey(apiKey)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid API key"));
            }
            
            Map<String, Object> context = new HashMap<>();
            context.put("transactionId", txnId);
            context.put("reasonCode", reasonCode);
            context.put("confirm", confirm);
            
            Map<String, Object> validation = complianceAgent.validateAction("open_dispute", txnId, context);
            
            if (!(Boolean) validation.get("isCompliant")) {
                logger.warn("Open dispute blocked for txnId={}, violations: {}", 
                           txnId, validation.get("violations"));
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "BLOCKED");
                response.put("reason", "Policy violation");
                response.put("violations", validation.get("violations"));
                response.put("requestId", requestId);
                
                return ResponseEntity.ok(response);
            }
            
            if (!confirm) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "PENDING_CONFIRMATION");
                response.put("message", "Confirmation required to open dispute");
                response.put("requestId", requestId);
                
                return ResponseEntity.ok(response);
            }
            
            String caseId = executeOpenDispute(txnId, reasonCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("caseId", caseId);
            response.put("status", "OPEN");
            response.put("txnId", txnId);
            response.put("reasonCode", reasonCode);
            response.put("requestId", requestId);
            response.put("timestamp", java.time.OffsetDateTime.now());
            
            logger.info("Dispute opened: caseId={}, txnId={}", caseId, txnId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error opening dispute for txnId={}", txnId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to open dispute"));
        }
    }
    
    /**
     * POST /api/action/contact-customer - Contact customer
     */
    @PostMapping("/contact-customer")
    public ResponseEntity<Map<String, Object>> contactCustomer(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        
        String customerId = (String) request.get("customerId");
        String message = (String) request.get("message");
        String maskedCustomerId = piiRedactionService.maskCustomerId(customerId);
        
        logger.info("Contact customer request: customerId={}, message={}", 
                   maskedCustomerId, message != null ? "provided" : "none");
        
        try {
            if (apiKey == null || !isValidApiKey(apiKey)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid API key"));
            }
            
            Map<String, Object> context = new HashMap<>();
            context.put("message", message);
            
            Map<String, Object> validation = complianceAgent.validateAction("contact_customer", customerId, context);
            
            if (!(Boolean) validation.get("isCompliant")) {
                logger.warn("Contact customer blocked for customerId={}, violations: {}", 
                           maskedCustomerId, validation.get("violations"));
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "BLOCKED");
                response.put("reason", "Policy violation");
                response.put("violations", validation.get("violations"));
                
                return ResponseEntity.ok(response);
            }
            
            String contactId = executeContactCustomer(customerId, message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("contactId", contactId);
            response.put("status", "SENT");
            response.put("customerId", customerId);
            response.put("timestamp", java.time.OffsetDateTime.now());
            
            logger.info("Customer contacted: contactId={}, customerId={}", contactId, maskedCustomerId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error contacting customer for customerId={}", maskedCustomerId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to contact customer"));
        }
    }
    
    private boolean isValidApiKey(String apiKey) {
        return "test-api-key-123".equals(apiKey) || "admin-api-key-456".equals(apiKey);
    }
    
    private boolean isValidOTP(String otp) {
        return "123456".equals(otp);
    }
    
    private boolean executeFreezeCard(String cardId) {
        logger.info("Executing card freeze for cardId={}", cardId);
        return true; // Mock success
    }
    
    private String executeOpenDispute(String txnId, String reasonCode) {
        String caseId = "CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        logger.info("Executing dispute creation: caseId={}, txnId={}, reasonCode={}", caseId, txnId, reasonCode);
        return caseId;
    }
    
    private String executeContactCustomer(String customerId, String message) {
        String contactId = "CONTACT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        logger.info("Executing customer contact: contactId={}, customerId={}", contactId, customerId);
        return contactId;
    }
}
