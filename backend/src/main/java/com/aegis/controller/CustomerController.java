package com.aegis.controller;

import com.aegis.entity.Transaction;
import com.aegis.repository.TransactionRepository;
import com.aegis.service.InsightsService;
import com.aegis.service.PiiRedactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/customer")
public class CustomerController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private InsightsService insightsService;
    
    @Autowired
    private PiiRedactionService piiRedactionService;
    
    /**
     * GET /api/customer/{id}/transactions - Get customer transactions with pagination
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<Map<String, Object>> getCustomerTransactions(
            @PathVariable String id,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @RequestParam(value = "last", required = false) Integer lastDays) {
        
        String maskedCustomerId = piiRedactionService.maskCustomerId(id);
        logger.info("Getting transactions for customerId={}, page={}, size={}", maskedCustomerId, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("ts").descending());
            Page<Transaction> transactions;
            
            // Determine date range
            OffsetDateTime fromDate = from;
            OffsetDateTime toDate = to;
            
            if (lastDays != null) {
                fromDate = OffsetDateTime.now().minusDays(lastDays);
                toDate = OffsetDateTime.now();
            } else if (fromDate == null) {
                fromDate = OffsetDateTime.now().minusDays(90); // Default to 90 days
            }
            
            if (toDate == null) {
                toDate = OffsetDateTime.now();
            }
            
            // Query transactions
            if (fromDate != null && toDate != null) {
                transactions = transactionRepository.findByCustomerIdAndTsBetweenOrderByTsDesc(
                    id, fromDate, toDate, pageable);
            } else {
                transactions = transactionRepository.findByCustomerIdOrderByTsDesc(id, pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("transactions", transactions.getContent());
            response.put("page", transactions.getNumber());
            response.put("size", transactions.getSize());
            response.put("totalElements", transactions.getTotalElements());
            response.put("totalPages", transactions.getTotalPages());
            response.put("from", fromDate);
            response.put("to", toDate);
            response.put("customerId", id);
            
            logger.info("Retrieved {} transactions for customerId={}", 
                       transactions.getContent().size(), maskedCustomerId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving transactions for customerId={}", maskedCustomerId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve transactions"));
        }
    }
    
    /**
     * GET /api/customer/{id}/insights/summary - Get customer insights summary
     */
    @GetMapping("/{id}/insights/summary")
    public ResponseEntity<Map<String, Object>> getCustomerInsights(@PathVariable String id) {
        String maskedCustomerId = piiRedactionService.maskCustomerId(id);
        logger.info("Getting insights for customerId={}", maskedCustomerId);
        
        try {
            Map<String, Object> insights = insightsService.generateCustomerInsights(id);
            insights.put("customerId", id);
            
            logger.info("Generated insights for customerId={}", maskedCustomerId);
            
            return ResponseEntity.ok(insights);
            
        } catch (Exception e) {
            logger.error("Error generating insights for customerId={}", maskedCustomerId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to generate insights"));
        }
    }
}
