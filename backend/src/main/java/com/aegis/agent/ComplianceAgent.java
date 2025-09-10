package com.aegis.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ComplianceAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(ComplianceAgent.class);
    
    // Cache validation results for 5 minutes
    private final Cache<String, Map<String, Object>> validationCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    // Cache customer risk status for 1 hour    
    private final Cache<String, Boolean> riskStatusCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();
        
    public Map<String, Object> validateAction(String action, String customerId, Map<String, Object> context) {
        String cacheKey = action + ":" + customerId + ":" + context.hashCode();
        
        return validationCache.get(cacheKey, k -> {
            logger.debug("Validating action: {} for customerId={}", action, customerId);
            
            Map<String, Object> result = new HashMap<>();
            List<String> violations = new ArrayList<>();
            Map<String, Object> requirements = new HashMap<>();
            
            try {
                boolean isCompliant = true;
                
                switch (action.toLowerCase()) {
                    case "freeze_card":
                        // Execute critical checks first
                        if (!hasActiveCards(customerId)) {
                            violations.add("no_active_cards");
                            isCompliant = false;
                            break; // Exit early if no active cards
                        }
                        
                        if (isCustomerFrozen(customerId)) {
                            violations.add("already_frozen");
                            isCompliant = false;
                            break; // Exit early if already frozen
                        }
                        
                        // Only check risk status if previous checks pass
                        if (isHighRiskCustomer(customerId)) {
                            requirements.put("otpRequired", true);
                            requirements.put("otpTimeout", 300);
                        }
                        break;
                        
                    case "unfreeze_card":
                        if (!isCustomerFrozen(customerId)) {
                            violations.add("not_frozen");
                            isCompliant = false;
                        }
                        
                        if (!hasIdentityVerification(customerId)) {
                            violations.add("identity_verification_required");
                            requirements.put("identityVerification", true);
                            requirements.put("handoffRequired", true);
                            isCompliant = false;
                        }
                        
                        if (isHighRiskCustomer(customerId)) {
                            requirements.put("otpRequired", true);
                        }
                        break;
                        
                    case "open_dispute":
                        String txnId = (String) context.get("transactionId");
                        String reasonCode = (String) context.get("reasonCode");
                        
                        CompletableFuture<Boolean> txnExistsFuture = CompletableFuture
                            .supplyAsync(() -> transactionExists(txnId));
                            
                        CompletableFuture<Boolean> disputeExistsFuture = CompletableFuture
                            .supplyAsync(() -> !disputeAlreadyExists(txnId));
                            
                        CompletableFuture<Boolean> reasonCodeFuture = CompletableFuture
                            .supplyAsync(() -> isValidReasonCode(reasonCode));
                            
                        // Wait for all checks to complete
                        CompletableFuture.allOf(
                            txnExistsFuture,
                            disputeExistsFuture,
                            reasonCodeFuture
                        ).join();
                        
                        if (!txnExistsFuture.get()) {
                            violations.add("transaction_not_found");
                            isCompliant = false;
                        }
                        
                        if (!disputeExistsFuture.get()) {
                            violations.add("dispute_already_exists");
                            isCompliant = false;
                        }
                        
                        if (!reasonCodeFuture.get()) {
                            violations.add("invalid_reason_code");
                            isCompliant = false;
                        }
                        break;
                        
                    case "contact_customer":
                        CompletableFuture<Boolean> contactInfoFuture = CompletableFuture
                            .supplyAsync(() -> hasValidContactInfo(customerId));
                            
                        CompletableFuture<Boolean> contactLimitFuture = CompletableFuture
                            .supplyAsync(() -> !hasExceededContactLimit(customerId));
                            
                        // Wait for both checks
                        CompletableFuture.allOf(contactInfoFuture, contactLimitFuture).join();
                        
                        if (!contactInfoFuture.get()) {
                            violations.add("no_contact_info");
                            isCompliant = false;
                        }
                        
                        if (!contactLimitFuture.get()) {
                            violations.add("contact_limit_exceeded");
                            isCompliant = false;
                        }
                        break;
                }
                
                result.put("isCompliant", isCompliant);
                result.put("violations", violations);
                result.put("requirements", requirements);
                
            } catch (Exception e) {
                logger.error("Validation error for action={}, customerId={}", action, customerId, e);
                result.put("isCompliant", false);
                result.put("violations", List.of("validation_error"));
                result.put("requirements", new HashMap<>());
            }
            
            return result;
        });
    }
    
    private boolean hasActiveCards(String customerId) {
        return !customerId.contains("no_cards");
    }
    
    private boolean isHighRiskCustomer(String customerId) {
        return riskStatusCache.get(customerId, id -> 
            id.contains("high_risk") || id.contains("cust_025"));
    }
    
    private boolean isCustomerFrozen(String customerId) {
        return customerId.contains("frozen");
    }
    
    private boolean hasIdentityVerification(String customerId) {
        return !customerId.contains("no_identity");
    }
    
    private boolean transactionExists(String txnId) {
        return txnId != null && txnId.startsWith("txn_");
    }
    
    private boolean disputeAlreadyExists(String txnId) {
        return txnId != null && txnId.contains("disputed");
    }
    
    private boolean isValidReasonCode(String reasonCode) {
        return reasonCode != null && reasonCode.matches("\\d{2}\\.\\d{1,2}");
    }
    
    private boolean hasValidContactInfo(String customerId) {
        return !customerId.contains("no_contact");
    }
    
    private boolean hasExceededContactLimit(String customerId) {
        return customerId.contains("spam") || customerId.contains("exceeded");
    }
}
