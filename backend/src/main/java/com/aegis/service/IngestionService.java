package com.aegis.service;

import com.aegis.dto.IngestResponse;
import com.aegis.entity.Transaction;
import com.aegis.repository.TransactionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(IngestionService.class);
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> processedRequests = ConcurrentHashMap.newKeySet();
    
    /**
     * Ingests transactions from uploaded file
     */
    public IngestResponse ingestFromFile(MultipartFile file, String requestId) {
        logger.info("Ingesting transactions from file: {}, requestId={}", file.getOriginalFilename(), requestId);
        
        // Check idempotency
        if (processedRequests.contains(requestId)) {
            logger.info("Request already processed: {}", requestId);
            return new IngestResponse(true, 0, requestId, "Request already processed");
        }
        
        try {
            List<Transaction> transactions = parseTransactionFile(file);
            int savedCount = saveTransactions(transactions);
            
            processedRequests.add(requestId);
            
            return new IngestResponse(true, savedCount, requestId, 
                String.format("Successfully ingested %d transactions", savedCount));
            
        } catch (Exception e) {
            logger.error("Error ingesting from file for requestId={}", requestId, e);
            return new IngestResponse(false, 0, requestId, "Ingestion failed: " + e.getMessage());
        }
    }
    
    /**
     * Ingests transactions from fixtures
     */
    public IngestResponse ingestFromFixtures(String source, String requestId) {
        logger.info("Ingesting transactions from fixtures: {}, requestId={}", source, requestId);
        
        // Check idempotency
        if (processedRequests.contains(requestId)) {
            logger.info("Request already processed: {}", requestId);
            return new IngestResponse(true, 0, requestId, "Request already processed");
        }
        
        try {
            List<Transaction> transactions = loadFixtureTransactions(source);
            int savedCount = saveTransactions(transactions);
            
            processedRequests.add(requestId);
            
            return new IngestResponse(true, savedCount, requestId, 
                String.format("Successfully loaded %d transactions from fixtures", savedCount));
            
        } catch (Exception e) {
            logger.error("Error ingesting from fixtures for requestId={}", requestId, e);
            return new IngestResponse(false, 0, requestId, "Fixture loading failed: " + e.getMessage());
        }
    }
    
    /**
     * Loads fixture data
     */
    public IngestResponse loadFixtures(String dataset, String requestId) {
        logger.info("Loading fixtures: dataset={}, requestId={}", dataset, requestId);
        
        try {
            int totalCount = 0;
            
            if ("all".equals(dataset) || "transactions".equals(dataset)) {
                List<Transaction> transactions = loadFixtureTransactions("transactions");
                totalCount += saveTransactions(transactions);
            }
            
            // Add other fixture types as needed
            
            return new IngestResponse(true, totalCount, requestId, 
                String.format("Successfully loaded %d records from fixtures", totalCount));
            
        } catch (Exception e) {
            logger.error("Error loading fixtures for requestId={}", requestId, e);
            return new IngestResponse(false, 0, requestId, "Fixture loading failed: " + e.getMessage());
        }
    }
    
    /**
     * Parses transaction file (CSV or JSON)
     */
    private List<Transaction> parseTransactionFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        
        if (filename != null && filename.endsWith(".json")) {
            return parseJsonTransactions(file);
        } else if (filename != null && filename.endsWith(".csv")) {
            return parseCsvTransactions(file);
        } else {
            throw new IllegalArgumentException("Unsupported file format. Please use JSON or CSV.");
        }
    }
    
    /**
     * Parses JSON transaction file
     */
    private List<Transaction> parseJsonTransactions(MultipartFile file) throws IOException {
        List<Map<String, Object>> transactionData = objectMapper.readValue(
            file.getInputStream(), new TypeReference<List<Map<String, Object>>>() {});
        
        return transactionData.stream()
            .map(this::mapToTransaction)
            .toList();
    }
    
    /**
     * Parses CSV transaction file (simplified implementation)
     */
    private List<Transaction> parseCsvTransactions(MultipartFile file) throws IOException {
        // Simplified CSV parsing - in real implementation, would use proper CSV library
        throw new UnsupportedOperationException("CSV parsing not implemented yet");
    }
    
    /**
     * Loads transactions from fixture files
     */
    private List<Transaction> loadFixtureTransactions(String source) throws IOException {
        String resourcePath = "fixtures/transactions.json";
        if ("large".equals(source)) {
            resourcePath = "fixtures/transactions_large.json";
        }
        
        ClassPathResource resource = new ClassPathResource(resourcePath);
        List<Map<String, Object>> transactionData = objectMapper.readValue(
            resource.getInputStream(), new TypeReference<List<Map<String, Object>>>() {});
        
        return transactionData.stream()
            .map(this::mapToTransaction)
            .toList();
    }
    
    /**
     * Maps transaction data to Transaction entity
     */
    private Transaction mapToTransaction(Map<String, Object> data) {
        Transaction transaction = new Transaction();
        
        transaction.setId((String) data.get("id"));
        transaction.setCustomerId((String) data.get("customerId"));
        transaction.setCardId((String) data.get("cardId"));
        transaction.setMcc((String) data.get("mcc"));
        transaction.setMerchant((String) data.get("merchant"));
        transaction.setAmount(((Number) data.get("amount")).longValue());
        transaction.setCurrency((String) data.getOrDefault("currency", "INR"));
        transaction.setStatus((String) data.getOrDefault("status", "captured"));
        
        // Parse timestamp
        if (data.get("ts") != null) {
            transaction.setTs(java.time.OffsetDateTime.parse((String) data.get("ts")));
        }
        
        // Parse device ID
        if (data.get("deviceId") != null) {
            transaction.setDeviceId((String) data.get("deviceId"));
        }
        
        // Parse geo data
        if (data.get("geo") instanceof Map) {
            Map<String, Object> geo = (Map<String, Object>) data.get("geo");
            if (geo.get("lat") != null) {
                transaction.setGeoLat(new java.math.BigDecimal(geo.get("lat").toString()));
            }
            if (geo.get("lon") != null) {
                transaction.setGeoLon(new java.math.BigDecimal(geo.get("lon").toString()));
            }
            if (geo.get("country") != null) {
                transaction.setGeoCountry((String) geo.get("country"));
            }
            if (geo.get("city") != null) {
                transaction.setGeoCity((String) geo.get("city"));
            }
        }
        
        return transaction;
    }
    
    /**
     * Saves transactions to database with deduplication
     */
    private int saveTransactions(List<Transaction> transactions) {
        int savedCount = 0;
        
        for (Transaction transaction : transactions) {
            try {
                // Check if transaction already exists (deduplication)
                if (!transactionRepository.existsById(transaction.getId())) {
                    transactionRepository.save(transaction);
                    savedCount++;
                }
            } catch (Exception e) {
                logger.warn("Failed to save transaction {}: {}", transaction.getId(), e.getMessage());
            }
        }
        
        logger.info("Saved {} new transactions", savedCount);
        return savedCount;
    }
}
