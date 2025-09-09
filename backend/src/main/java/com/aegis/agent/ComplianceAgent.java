package com.aegis.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ComplianceAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(ComplianceAgent.class);
    
    /**
     * Validates if an action is compliant with policies
     */
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
        // Check if customer has active cards
        if (!hasActiveCards(customerId)) {
            violations.add("no_active_cards");
            return false;
        }
        
        // Check if OTP is required
        if (isHighRiskCustomer(customerId)) {
            requirements.put("otpRequired", true);
            requirements.put("otpTimeout", 300); // 5 minutes
        }
        
        // Check if customer is already frozen
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
        // Check if customer is actually frozen
        if (!isCustomerFrozen(customerId)) {
            violations.add("not_frozen");
            return false;
        }
        
        // Check identity verification requirements
        if (!hasIdentityVerification(customerId)) {
            violations.add("identity_verification_required");
            requirements.put("identityVerification", true);
            requirements.put("handoffRequired", true);
            return false;
        }
        
        // Check if OTP is required
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
        // Check if transaction exists
        String transactionId = (String) context.get("transactionId");
        if (transactionId == null || !transactionExists(transactionId)) {
            violations.add("transaction_not_found");
            return false;
        }
        
        // Check dispute time limits
        if (isDisputeExpired(transactionId)) {
            violations.add("dispute_time_expired");
            return false;
        }
        
        // Check if dispute already exists
        if (disputeAlreadyExists(transactionId)) {
            violations.add("dispute_already_exists");
            return false;
        }
        
        // Check reason code validity
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
        // Check if customer has valid contact information
        if (!hasValidContactInfo(customerId)) {
            violations.add("no_contact_info");
            return false;
        }
        
        // Check contact frequency limits
        if (hasExceededContactLimit(customerId)) {
            violations.add("contact_limit_exceeded");
            return false;
        }
        
        return true;
    }
    
    // Helper methods for validation logic
    private boolean hasActiveCards(String customerId) {
        // Simplified - in real implementation, would check database
        return !customerId.contains("no_cards");
    }
    
    private boolean isHighRiskCustomer(String customerId) {
        // Simplified - in real implementation, would check risk flags
        return customerId.contains("high_risk") || customerId.contains("cust_025");
    }
    
    private boolean isCustomerFrozen(String customerId) {
        // Simplified - in real implementation, would check card status
        return customerId.contains("frozen");
    }
    
    private boolean hasIdentityVerification(String customerId) {
        // Simplified - in real implementation, would check verification status
        return !customerId.contains("no_identity");
    }
    
    private boolean transactionExists(String transactionId) {
        // Simplified - in real implementation, would check database
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
        // Valid reason codes for disputes
        Set<String> validCodes = Set.of("10.4", "10.5", "10.6", "10.7", "10.8");
        return validCodes.contains(reasonCode);
    }
    
    private boolean hasValidContactInfo(String customerId) {
        // Simplified - in real implementation, would check customer data
        return !customerId.contains("no_contact");
    }
    
    private boolean hasExceededContactLimit(String customerId) {
        // Simplified - in real implementation, would check contact history
        return customerId.contains("contact_limit");
    }
}
