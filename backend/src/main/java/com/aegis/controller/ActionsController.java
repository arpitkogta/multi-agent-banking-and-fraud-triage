package com.aegis.controller;

import com.aegis.agent.ComplianceAgent;
import com.aegis.metrics.MetricsService;
import com.aegis.service.PiiRedactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

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
    
    // Cache API key validations for 1 hour
    private final Cache<String, Boolean> apiKeyCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();
        
    // Cache OTP validations for 5 minutes
    private final Cache<String, String> otpCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    // Track card freeze requests to prevent duplicates
    private final Set<String> inProgressFreezes = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
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
        
        if (cardId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Card ID is required"));
        }
        
        String maskedCardId = piiRedactionService.maskCustomerId(cardId);
        String freezeKey = cardId + "-" + requestId;

        try {
            if (!isValidApiKey(apiKey)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid API key"));
            }
            
            if (!inProgressFreezes.add(freezeKey)) {
                return ResponseEntity.ok(Map.of(
                    "status", "IN_PROGRESS", 
                    "requestId", requestId));
            }

            // Faster compliance check with timeout
            Map<String, Object> context = Map.of("cardId", cardId, "otp", otp != null ? otp : "");
            Map<String, Object> validation = CompletableFuture
                .supplyAsync(() -> complianceAgent.validateAction("freeze_card", cardId, context))
                .get(500, TimeUnit.MILLISECONDS);

            if (!(Boolean) validation.get("isCompliant")) {
                return ResponseEntity.ok(Map.of(
                    "status", "BLOCKED",
                    "violations", validation.get("violations"),
                    "requestId", requestId));
            }

            Map<String, Object> requirements = (Map<String, Object>) validation.get("requirements");
            boolean otpRequired = (Boolean) requirements.getOrDefault("otpRequired", false);

            if (otpRequired) {
                if (otp == null) {
                    String cachedOtp = generateAndCacheOTP(cardId);
                    return ResponseEntity.ok(Map.of(
                        "status", "PENDING_OTP",
                        "requestId", requestId,
                        "otpSent", true));
                }
                
                if (!validateOTP(cardId, otp)) {
                    return ResponseEntity.ok(Map.of(
                        "status", "INVALID_OTP",
                        "requestId", requestId));
                }
            }

            // Execute freeze with timeout
            boolean success = CompletableFuture
                .supplyAsync(() -> executeFreezeCard(cardId))
                .get(750, TimeUnit.MILLISECONDS);

            return ResponseEntity.ok(Map.of(
                "status", success ? "FROZEN" : "FAILED",
                "cardId", cardId,
                "requestId", requestId,
                "timestamp", OffsetDateTime.now()));

        } catch (TimeoutException e) {
            logger.error("Timeout freezing card for cardId={}", maskedCardId);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(Map.of(
                    "error", "Operation timed out",
                    "requestId", requestId,
                    "retryAfter", 1000));
                    
        } catch (Exception e) {
            logger.error("Error freezing card for cardId={}", maskedCardId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Failed to freeze card",
                    "requestId", requestId));
                    
        } finally {
            inProgressFreezes.remove(freezeKey);
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
        if (apiKey == null) {
            return false;
        }
        return apiKeyCache.get(apiKey, k -> 
            "test-api-key-123".equals(k) || "admin-api-key-456".equals(k));
    }
    
    private String generateAndCacheOTP(String cardId) {
        String otp = String.format("%06d", new Random().nextInt(1000000));
        otpCache.put(cardId, otp);
        return otp;
    }
    
    private boolean validateOTP(String cardId, String otp) {
        String cachedOtp = otpCache.getIfPresent(cardId);
        if (cachedOtp != null && cachedOtp.equals(otp)) {
            otpCache.invalidate(cardId);
            return true;
        }
        return false;
    }
    
    private boolean executeFreezeCard(String cardId) {
        // Simulated card freeze with 95% success rate
        boolean success = Math.random() < 0.95;
        if (success) {
            logger.info("Card freeze executed successfully for cardId={}", cardId);
        } else {
            logger.error("Card freeze failed for cardId={}", cardId);
        }
        return success;
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
