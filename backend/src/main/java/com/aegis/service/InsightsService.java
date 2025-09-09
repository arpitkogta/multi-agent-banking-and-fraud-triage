package com.aegis.service;

import com.aegis.entity.Transaction;
import com.aegis.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InsightsService {
    
    private static final Logger logger = LoggerFactory.getLogger(InsightsService.class);
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    /**
     * Generates customer insights summary
     */
    public Map<String, Object> generateCustomerInsights(String customerId) {
        logger.debug("Generating insights for customerId={}", customerId);
        
        Map<String, Object> insights = new HashMap<>();
        
        try {
            // Get transactions from last 90 days
            OffsetDateTime fromDate = OffsetDateTime.now().minusDays(90);
            List<Transaction> transactions = transactionRepository
                .findByCustomerIdAndTsAfterOrderByTsDesc(customerId, fromDate, 
                    org.springframework.data.domain.PageRequest.of(0, 1000))
                .getContent();
            
            if (transactions.isEmpty()) {
                insights.put("message", "No transactions found for the last 90 days");
                return insights;
            }
            
            // Calculate total spend
            long totalSpend = transactions.stream()
                .mapToLong(txn -> Math.abs(txn.getAmount()))
                .sum();
            
            // Top merchants
            List<Map<String, Object>> topMerchants = getTopMerchants(transactions);
            
            // Category breakdown
            List<Map<String, Object>> categories = getCategoryBreakdown(transactions);
            
            // Monthly trend
            List<Map<String, Object>> monthlyTrend = getMonthlyTrend(transactions);
            
            // Risk indicators
            Map<String, Object> riskIndicators = getRiskIndicators(transactions);
            
            insights.put("totalSpend", totalSpend);
            insights.put("currency", "INR");
            insights.put("period", "90 days");
            insights.put("transactionCount", transactions.size());
            insights.put("topMerchants", topMerchants);
            insights.put("categories", categories);
            insights.put("monthlyTrend", monthlyTrend);
            insights.put("riskIndicators", riskIndicators);
            insights.put("generatedAt", OffsetDateTime.now());
            
            logger.debug("Generated insights for customerId={}: {} transactions, {} total spend", 
                        customerId, transactions.size(), totalSpend);
            
        } catch (Exception e) {
            logger.error("Error generating insights for customerId={}", customerId, e);
            insights.put("error", "Failed to generate insights");
        }
        
        return insights;
    }
    
    /**
     * Gets top merchants by transaction count and amount
     */
    private List<Map<String, Object>> getTopMerchants(List<Transaction> transactions) {
        Map<String, MerchantStats> merchantStats = new HashMap<>();
        
        for (Transaction txn : transactions) {
            String merchant = txn.getMerchant();
            MerchantStats stats = merchantStats.computeIfAbsent(merchant, k -> new MerchantStats());
            stats.addTransaction(Math.abs(txn.getAmount()));
        }
        
        return merchantStats.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue().totalAmount, a.getValue().totalAmount))
            .limit(5)
            .map(entry -> {
                Map<String, Object> merchant = new HashMap<>();
                merchant.put("name", entry.getKey());
                merchant.put("transactionCount", entry.getValue().transactionCount);
                merchant.put("totalAmount", entry.getValue().totalAmount);
                return merchant;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Gets category breakdown by MCC
     */
    private List<Map<String, Object>> getCategoryBreakdown(List<Transaction> transactions) {
        Map<String, CategoryStats> categoryStats = new HashMap<>();
        
        for (Transaction txn : transactions) {
            String mcc = txn.getMcc();
            String categoryName = getCategoryName(mcc);
            CategoryStats stats = categoryStats.computeIfAbsent(categoryName, k -> new CategoryStats());
            stats.addTransaction(Math.abs(txn.getAmount()));
        }
        
        long totalAmount = categoryStats.values().stream()
            .mapToLong(stats -> stats.totalAmount)
            .sum();
        
        return categoryStats.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue().totalAmount, a.getValue().totalAmount))
            .map(entry -> {
                Map<String, Object> category = new HashMap<>();
                category.put("name", entry.getKey());
                category.put("mcc", getMccForCategory(entry.getKey()));
                category.put("transactionCount", entry.getValue().transactionCount);
                category.put("totalAmount", entry.getValue().totalAmount);
                category.put("percentage", (double) entry.getValue().totalAmount / totalAmount * 100);
                return category;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Gets monthly spending trend
     */
    private List<Map<String, Object>> getMonthlyTrend(List<Transaction> transactions) {
        Map<String, Long> monthlySpend = new HashMap<>();
        
        for (Transaction txn : transactions) {
            String month = txn.getTs().toLocalDate().withDayOfMonth(1).toString();
            monthlySpend.merge(month, Math.abs(txn.getAmount()), Long::sum);
        }
        
        return monthlySpend.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                Map<String, Object> month = new HashMap<>();
                month.put("month", entry.getKey());
                month.put("amount", entry.getValue());
                return month;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Gets risk indicators
     */
    private Map<String, Object> getRiskIndicators(List<Transaction> transactions) {
        Map<String, Object> indicators = new HashMap<>();
        
        // Check for unusual spending patterns
        long avgAmount = transactions.stream()
            .mapToLong(txn -> Math.abs(txn.getAmount()))
            .sum() / transactions.size();
        
        long maxAmount = transactions.stream()
            .mapToLong(txn -> Math.abs(txn.getAmount()))
            .max()
            .orElse(0L);
        
        // Check for multiple cities
        Set<String> cities = transactions.stream()
            .map(Transaction::getGeoCity)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        // Check for multiple devices
        Set<String> devices = transactions.stream()
            .map(Transaction::getDeviceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        indicators.put("averageTransactionAmount", avgAmount);
        indicators.put("maxTransactionAmount", maxAmount);
        indicators.put("citiesVisited", cities.size());
        indicators.put("devicesUsed", devices.size());
        indicators.put("hasUnusualPatterns", maxAmount > avgAmount * 5 || cities.size() > 3 || devices.size() > 2);
        
        return indicators;
    }
    
    /**
     * Maps MCC code to category name
     */
    private String getCategoryName(String mcc) {
        return switch (mcc) {
            case "5411" -> "Grocery";
            case "5812" -> "Restaurants";
            case "4121" -> "Transportation";
            case "6011" -> "ATM";
            case "5311" -> "Retail";
            case "7995" -> "Entertainment";
            case "5814" -> "Fast Food";
            case "7011" -> "Hotels";
            case "4511" -> "Airlines";
            default -> "Other";
        };
    }
    
    /**
     * Gets MCC code for category name
     */
    private String getMccForCategory(String categoryName) {
        return switch (categoryName) {
            case "Grocery" -> "5411";
            case "Restaurants" -> "5812";
            case "Transportation" -> "4121";
            case "ATM" -> "6011";
            case "Retail" -> "5311";
            case "Entertainment" -> "7995";
            case "Fast Food" -> "5814";
            case "Hotels" -> "7011";
            case "Airlines" -> "4511";
            default -> "0000";
        };
    }
    
    // Helper classes
    private static class MerchantStats {
        int transactionCount = 0;
        long totalAmount = 0;
        
        void addTransaction(long amount) {
            transactionCount++;
            totalAmount += amount;
        }
    }
    
    private static class CategoryStats {
        int transactionCount = 0;
        long totalAmount = 0;
        
        void addTransaction(long amount) {
            transactionCount++;
            totalAmount += amount;
        }
    }
}
