package com.aegis.controller;

import com.aegis.dto.IngestResponse;
import com.aegis.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/ingest")
public class IngestionController {
    
    private static final Logger logger = LoggerFactory.getLogger(IngestionController.class);
    
    @Autowired
    private IngestionService ingestionService;
    
    /**
     * POST /api/ingest/transactions - Ingest transactions from CSV or JSON
     */
    @PostMapping("/transactions")
    public ResponseEntity<IngestResponse> ingestTransactions(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "source", defaultValue = "fixtures") String source,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        String requestId = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        
        logger.info("Ingestion request received: requestId={}, source={}, hasFile={}", 
                   requestId, source, file != null);
        
        try {
            IngestResponse response;
            
            if (file != null && !file.isEmpty()) {
                // Ingest from uploaded file
                response = ingestionService.ingestFromFile(file, requestId);
            } else {
                // Ingest from fixtures
                response = ingestionService.ingestFromFixtures(source, requestId);
            }
            
            logger.info("Ingestion completed: requestId={}, accepted={}, count={}", 
                       requestId, response.isAccepted(), response.getCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during ingestion for requestId={}", requestId, e);
            
            IngestResponse errorResponse = new IngestResponse(false, 0, requestId, 
                "Ingestion failed: " + e.getMessage());
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * POST /api/ingest/fixtures - Load fixture data
     */
    @PostMapping("/fixtures")
    public ResponseEntity<IngestResponse> loadFixtures(
            @RequestParam(value = "dataset", defaultValue = "all") String dataset,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        String requestId = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        
        logger.info("Fixture loading request: requestId={}, dataset={}", requestId, dataset);
        
        try {
            IngestResponse response = ingestionService.loadFixtures(dataset, requestId);
            
            logger.info("Fixture loading completed: requestId={}, accepted={}, count={}", 
                       requestId, response.isAccepted(), response.getCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error loading fixtures for requestId={}", requestId, e);
            
            IngestResponse errorResponse = new IngestResponse(false, 0, requestId, 
                "Fixture loading failed: " + e.getMessage());
            
            return ResponseEntity.ok(errorResponse);
        }
    }
}
