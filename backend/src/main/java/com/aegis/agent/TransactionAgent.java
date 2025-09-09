package com.aegis.agent;

import com.aegis.entity.Transaction;
import com.aegis.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class TransactionAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionAgent.class);
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    /**
     * Retrieves recent transactions for a customer
     */
    public Map<String, Object> getRecentTransactions(String customerId, int days) {
        logger.debug("Getting recent transactions for customerId={}, days={}", customerId, days);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            OffsetDateTime fromDate = OffsetDateTime.now().minusDays(days);
            Pageable pageable = PageRequest.of(0, 100); // Limit to 100 recent transactions
            
            Page<Transaction> transactions = transactionRepository
                .findByCustomerIdAndTsAfterOrderByTsDesc(customerId, fromDate, pageable);
            
            List<Map<String, Object>> transactionList = new ArrayList<>();
            
            for (Transaction txn : transactions.getContent()) {
                Map<String, Object> txnData = new HashMap<>();
                txnData.put("id", txn.getId());
                txnData.put("merchant", txn.getMerchant());
                txnData.put("amount", txn.getAmount());
                txnData.put("currency", txn.getCurrency());
                txnData.put("mcc", txn.getMcc());
                txnData.put("ts", txn.getTs());
                txnData.put("status", txn.getStatus());
                txnData.put("deviceId", txn.getDeviceId());
                
                if (txn.getGeoLat() != null && txn.getGeoLon() != null) {
                    Map<String, Object> geo = new HashMap<>();
                    geo.put("lat", txn.getGeoLat());
                    geo.put("lon", txn.getGeoLon());
                    geo.put("country", txn.getGeoCountry());
                    geo.put("city", txn.getGeoCity());
                    txnData.put("geo", geo);
                }
                
                transactionList.add(txnData);
            }
            
            result.put("transactions", transactionList);
            result.put("totalCount", transactions.getTotalElements());
            result.put("fromDate", fromDate);
            result.put("toDate", OffsetDateTime.now());
            
            // Analyze transaction patterns
            analyzeTransactionPatterns(transactionList, result);
            
            logger.debug("Retrieved {} transactions for customerId={}", 
                        transactionList.size(), customerId);
            
        } catch (Exception e) {
            logger.error("Error retrieving transactions for customerId={}", customerId, e);
            result.put("error", "Failed to retrieve transactions");
            result.put("transactions", new ArrayList<>());
        }
        
        return result;
    }
    
    /**
     * Analyzes transaction patterns for risk signals
     */
    private void analyzeTransactionPatterns(List<Map<String, Object>> transactions, Map<String, Object> result) {
        if (transactions.isEmpty()) {
            return;
        }
        
        // Check for duplicate transactions (pending vs captured)
        Map<String, List<Map<String, Object>>> merchantGroups = new HashMap<>();
        for (Map<String, Object> txn : transactions) {
            String merchant = (String) txn.get("merchant");
            merchantGroups.computeIfAbsent(merchant, k -> new ArrayList<>()).add(txn);
        }
        
        for (Map.Entry<String, List<Map<String, Object>>> entry : merchantGroups.entrySet()) {
            List<Map<String, Object>> merchantTxns = entry.getValue();
            if (merchantTxns.size() > 1) {
                // Check for pending and captured transactions
                boolean hasPending = merchantTxns.stream()
                    .anyMatch(txn -> "pending".equals(txn.get("status")));
                boolean hasCaptured = merchantTxns.stream()
                    .anyMatch(txn -> "captured".equals(txn.get("status")));
                
                if (hasPending && hasCaptured) {
                    result.put("duplicate_transaction", true);
                    result.put("duplicate_merchant", entry.getKey());
                    break;
                }
            }
        }
        
        // Check for geo-velocity violations (simplified)
        Set<String> cities = new HashSet<>();
        for (Map<String, Object> txn : transactions) {
            Map<String, Object> geo = (Map<String, Object>) txn.get("geo");
            if (geo != null && geo.get("city") != null) {
                cities.add((String) geo.get("city"));
            }
        }
        
        if (cities.size() > 3) {
            result.put("geo_velocity_violation", true);
            result.put("cities_visited", cities.size());
        }
        
        // Check for device changes
        Set<String> devices = new HashSet<>();
        for (Map<String, Object> txn : transactions) {
            String deviceId = (String) txn.get("deviceId");
            if (deviceId != null) {
                devices.add(deviceId);
            }
        }
        
        if (devices.size() > 2) {
            result.put("device_change", true);
            result.put("device_count", devices.size());
        }
    }
}
