package com.aegis.agent;

import com.aegis.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MerchantDisambiguationAgent extends BaseAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(MerchantDisambiguationAgent.class);
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    // Common merchant name variations and their canonical forms
    private static final Map<String, List<String>> MERCHANT_VARIATIONS = Map.of(
        "Gaming Store", Arrays.asList("Gaming Store", "GamingStore", "Gaming Store Inc", "Gaming Store LLC", "GamingStore.com"),
        "Amazon", Arrays.asList("Amazon", "Amazon.com", "AMAZON.COM", "Amazon Marketplace", "AMZN"),
        "Netflix", Arrays.asList("Netflix", "NETFLIX", "Netflix.com", "Netflix Streaming"),
        "Uber", Arrays.asList("Uber", "UBER", "Uber Technologies", "Uber Eats", "Uber Rides"),
        "Starbucks", Arrays.asList("Starbucks", "STARBUCKS", "Starbucks Coffee", "SBUX")
    );
    
    /**
     * Analyzes merchant name for disambiguation needs
     */
    public Map<String, Object> analyzeMerchantDisambiguation(String merchantName, String customerId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Analyzing merchant disambiguation for: {}", merchantName);
            
            // Check if merchant name is ambiguous
            boolean isAmbiguous = isMerchantAmbiguous(merchantName);
            result.put("isAmbiguous", isAmbiguous);
            
            if (isAmbiguous) {
                // Find similar merchants in customer's transaction history
                List<Map<String, Object>> candidates = findMerchantCandidates(merchantName, customerId);
                result.put("candidates", candidates);
                result.put("disambiguationRequired", true);
                result.put("originalMerchant", merchantName);
                
                // Generate disambiguation prompt
                String prompt = generateDisambiguationPrompt(merchantName, candidates);
                result.put("disambiguationPrompt", prompt);
                
                logger.info("Found {} candidates for merchant: {}", candidates.size(), merchantName);
            } else {
                result.put("disambiguationRequired", false);
                result.put("canonicalMerchant", merchantName);
            }
            
            result.put("status", "success");
            
        } catch (Exception e) {
            logger.error("Error in merchant disambiguation analysis", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("disambiguationRequired", false);
        }
        
        return result;
    }
    
    /**
     * Processes user's merchant selection
     */
    public Map<String, Object> processMerchantSelection(String originalMerchant, String selectedMerchant, String customerId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Processing merchant selection: {} -> {}", originalMerchant, selectedMerchant);
            
            // Validate the selection
            List<Map<String, Object>> candidates = findMerchantCandidates(originalMerchant, customerId);
            boolean isValidSelection = candidates.stream()
                .anyMatch(candidate -> selectedMerchant.equals(candidate.get("merchantName")));
            
            if (isValidSelection) {
                result.put("selectedMerchant", selectedMerchant);
                result.put("selectionValid", true);
                
                // Update merchant mapping for future reference
                updateMerchantMapping(originalMerchant, selectedMerchant, customerId);
                
                logger.info("Merchant selection processed successfully");
            } else {
                result.put("selectionValid", false);
                result.put("error", "Invalid merchant selection");
                logger.warn("Invalid merchant selection: {}", selectedMerchant);
            }
            
            result.put("status", "success");
            
        } catch (Exception e) {
            logger.error("Error processing merchant selection", e);
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Checks if a merchant name is ambiguous
     */
    private boolean isMerchantAmbiguous(String merchantName) {
        if (merchantName == null || merchantName.trim().isEmpty()) {
            return false;
        }
        
        // Check against known ambiguous patterns
        String normalized = merchantName.toLowerCase().trim();
        
        // Check for common ambiguous patterns
        return normalized.contains("store") || 
               normalized.contains("shop") ||
               normalized.contains("market") ||
               normalized.contains("center") ||
               normalized.contains("inc") ||
               normalized.contains("llc") ||
               normalized.contains("corp") ||
               normalized.contains("ltd");
    }
    
    /**
     * Finds potential merchant candidates based on customer history
     */
    private List<Map<String, Object>> findMerchantCandidates(String merchantName, String customerId) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        
        try {
            // Get customer's recent transactions
            List<com.aegis.entity.Transaction> recentTransactions = 
                transactionRepository.findByCustomerIdOrderByTsDesc(customerId, 
                    org.springframework.data.domain.PageRequest.of(0, 100)).getContent();
            
            // Group by merchant and find similar names
            Map<String, List<com.aegis.entity.Transaction>> merchantGroups = 
                recentTransactions.stream()
                    .collect(Collectors.groupingBy(com.aegis.entity.Transaction::getMerchant));
            
            String normalizedTarget = merchantName.toLowerCase();
            
            for (Map.Entry<String, List<com.aegis.entity.Transaction>> entry : merchantGroups.entrySet()) {
                String existingMerchant = entry.getKey();
                String normalizedExisting = existingMerchant.toLowerCase();
                
                // Check for similarity
                if (isSimilarMerchant(normalizedTarget, normalizedExisting)) {
                    Map<String, Object> candidate = new HashMap<>();
                    candidate.put("merchantName", existingMerchant);
                    candidate.put("transactionCount", entry.getValue().size());
                    candidate.put("lastTransaction", entry.getValue().get(0).getTs());
                    candidate.put("totalAmount", entry.getValue().stream()
                        .mapToLong(com.aegis.entity.Transaction::getAmount)
                        .sum());
                    candidate.put("similarityScore", calculateSimilarity(normalizedTarget, normalizedExisting));
                    
                    candidates.add(candidate);
                }
            }
            
            // Sort by similarity score and transaction count
            candidates.sort((a, b) -> {
                double scoreA = (Double) a.get("similarityScore");
                double scoreB = (Double) b.get("similarityScore");
                int countA = (Integer) a.get("transactionCount");
                int countB = (Integer) b.get("transactionCount");
                
                if (Math.abs(scoreA - scoreB) < 0.1) {
                    return Integer.compare(countB, countA); // More transactions first
                }
                return Double.compare(scoreB, scoreA); // Higher similarity first
            });
            
            // Return top 3 candidates
            return candidates.stream().limit(3).collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error finding merchant candidates", e);
            return candidates;
        }
    }
    
    /**
     * Checks if two merchant names are similar
     */
    private boolean isSimilarMerchant(String name1, String name2) {
        // Simple similarity check - can be enhanced with more sophisticated algorithms
        if (name1.equals(name2)) return true;
        
        // Check if one contains the other
        if (name1.contains(name2) || name2.contains(name1)) return true;
        
        // Check for common words
        String[] words1 = name1.split("\\s+");
        String[] words2 = name2.split("\\s+");
        
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.length() > 2 && word2.length() > 2 && 
                    (word1.contains(word2) || word2.contains(word1))) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Calculates similarity score between two merchant names
     */
    private double calculateSimilarity(String name1, String name2) {
        if (name1.equals(name2)) return 1.0;
        
        // Simple Jaccard similarity
        Set<String> set1 = new HashSet<>(Arrays.asList(name1.split("\\s+")));
        Set<String> set2 = new HashSet<>(Arrays.asList(name2.split("\\s+")));
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Generates disambiguation prompt for user
     */
    private String generateDisambiguationPrompt(String originalMerchant, List<Map<String, Object>> candidates) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("I found multiple merchants that might match \"").append(originalMerchant).append("\":\n\n");
        
        for (int i = 0; i < candidates.size(); i++) {
            Map<String, Object> candidate = candidates.get(i);
            prompt.append(i + 1).append(". ").append(candidate.get("merchantName"));
            prompt.append(" (").append(candidate.get("transactionCount")).append(" transactions)");
            prompt.append("\n");
        }
        
        prompt.append("\nPlease select which merchant you meant, or say 'none' if none of these match.");
        
        return prompt.toString();
    }
    
    /**
     * Updates merchant mapping for future reference
     */
    private void updateMerchantMapping(String originalMerchant, String selectedMerchant, String customerId) {
        // In a real implementation, this would store the mapping in a database
        // For now, we'll just log it
        logger.info("Updated merchant mapping for customer {}: {} -> {}", 
                   customerId, originalMerchant, selectedMerchant);
    }
}
