package com.aegis.agent;

import com.aegis.entity.Customer;
import com.aegis.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ProfileAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(ProfileAgent.class);
    
    @Autowired
    private CustomerRepository customerRepository;
    
    /**
     * Retrieves customer profile information
     */
    public Map<String, Object> getCustomerProfile(String customerId) {
        logger.debug("Getting customer profile for customerId={}", customerId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<Customer> customerOpt = customerRepository.findActiveById(customerId);
            
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                
                result.put("customerId", customer.getId());
                result.put("name", customer.getName());
                result.put("emailMasked", customer.getEmailMasked());
                result.put("riskFlags", customer.getRiskFlags());
                result.put("status", customer.getStatus());
                result.put("createdAt", customer.getCreatedAt());
                
                // Check for chargeback history (simplified)
                if (customer.getRiskFlags() != null && customer.getRiskFlags().contains("chargeback_history")) {
                    result.put("chargeback_history", true);
                    result.put("chargeback_count", 2); // Mock data
                }
                
                logger.debug("Retrieved profile for customerId={}", customerId);
            } else {
                result.put("error", "Customer not found");
                result.put("customerId", customerId);
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving customer profile for customerId={}", customerId, e);
            result.put("error", "Failed to retrieve customer profile");
            result.put("customerId", customerId);
        }
        
        return result;
    }
}
