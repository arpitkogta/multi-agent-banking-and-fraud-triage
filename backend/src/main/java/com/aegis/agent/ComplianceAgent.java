package com.aegis.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ComplianceAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(ComplianceAgent.class);
    
    public Map<String, Object> validateAction(String action, String customerId, Map<String, Object> context) {
        logger.debug("Validating action: {} for customerId: {}", action, customerId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean isCompliant = true;
            List<String> violations = new ArrayList<>();
            Map<String, Object> requirements = new HashMap<>();
            
            switch (action.toLowerCase()) {
                case "freeze_card":
                    isCompliant = validateFreezeCard(customerId, context, violations, requirements);
                    break;
                case "unfreeze_card":
                    isCompliant = validateUnfreezeCard(customerId, context, violations, requirements);
                    break;
                case "open_dispute":
                    isCompliant = validateOpenDispute(customerId, context, violations, requirements);
                    break;
                case "contact_customer":
                    isCompliant = validateContactCustomer(customerId, context, violations, requirements);
                    break;
                default:
                    violations.add("unknown_action");
                    isCompliant = false;
            }
            
            result.put("isCompliant", isCompliant);
            result.put("violations", violations);
            result.put("requirements", requirements);
            result.put("action", action);
            result.put("customerId", customerId);
            
            if (!isCompliant) {
                logger.warn("Action {} blocked for customerId={}, violations: {}", 
                           action, customerId, violations);
            }
            
        } catch (Exception e) {
            logger.error("Error validating action {} for customerId={}", action, customerId, e);
            result.put("isCompliant", false);
            result.put("violations", Arrays.asList("validation_error"));
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validates card freeze action
     */
    private boolean validateFreezeCard(String customerId, Map<String, Object> context, 
                                     List<String> violations, Map<String, Object> requirements) {
        if (!hasActiveCards(customerId)) {
            violations.add("no_active_cards");
            return false;
        }
        
        if (isHighRiskCustomer(customerId)) {
            requirements.put("otpRequired", true);
            requirements.put("otpTimeout", 300);
        }
        
        if (isCustomerFrozen(customerId)) {
            violations.add("already_frozen");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates card unfreeze action
     */
    private boolean validateUnfreezeCard(String customerId, Map<String, Object> context, 
                                       List<String> violations, Map<String, Object> requirements) {
        if (!isCustomerFrozen(customerId)) {
            violations.add("not_frozen");
            return false;
        }
        
        if (!hasIdentityVerification(customerId)) {
            violations.add("identity_verification_required");
            requirements.put("identityVerification", true);
            requirements.put("handoffRequired", true);
            return false;
        }
        
        if (isHighRiskCustomer(customerId)) {
            requirements.put("otpRequired", true);
        }
        
        return true;
    }
    
    /**
     * Validates dispute opening action
     */
    private boolean validateOpenDispute(String customerId, Map<String, Object> context, 
                                      List<String> violations, Map<String, Object> requirements) {
        String transactionId = (String) context.get("transactionId");
        if (transactionId == null || !transactionExists(transactionId)) {
            violations.add("transaction_not_found");
            return false;
        }
        
        if (isDisputeExpired(transactionId)) {
            violations.add("dispute_time_expired");
            return false;
        }
        
        if (disputeAlreadyExists(transactionId)) {
            violations.add("dispute_already_exists");
            return false;
        }
        
        String reasonCode = (String) context.get("reasonCode");
        if (reasonCode == null || !isValidReasonCode(reasonCode)) {
            violations.add("invalid_reason_code");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates customer contact action
     */
    private boolean validateContactCustomer(String customerId, Map<String, Object> context, 
                                          List<String> violations, Map<String, Object> requirements) {
        if (!hasValidContactInfo(customerId)) {
            violations.add("no_contact_info");
            return false;
        }
        
        if (hasExceededContactLimit(customerId)) {
            violations.add("contact_limit_exceeded");
            return false;
        }
        
        return true;
    }
    
    private boolean hasActiveCards(String customerId) {
        return !customerId.contains("no_cards");
    }
    
    private boolean isHighRiskCustomer(String customerId) {
        return customerId.contains("high_risk") || customerId.contains("cust_025");
    }
    
    private boolean isCustomerFrozen(String customerId) {
        return customerId.contains("frozen");
    }
    
    private boolean hasIdentityVerification(String customerId) {
        return !customerId.contains("no_identity");
    }
    
    private boolean transactionExists(String transactionId) {
        return transactionId != null && !transactionId.isEmpty();
    }
    
    private boolean isDisputeExpired(String transactionId) {
        // Simplified - in real implementation, would check transaction date
        return transactionId.contains("expired");
    }
    
    private boolean disputeAlreadyExists(String transactionId) {
        // Simplified - in real implementation, would check dispute records
        return transactionId.contains("disputed");
    }
    
    private boolean isValidReasonCode(String reasonCode) {
        Set<String> validCodes = Set.of("10.4", "10.5", "10.6", "10.7", "10.8");
        return validCodes.contains(reasonCode);
    }
    
    private boolean hasValidContactInfo(String customerId) {
        return !customerId.contains("no_contact");
    }
    
    private boolean hasExceededContactLimit(String customerId) {
        return customerId.contains("contact_limit");
    }
}
