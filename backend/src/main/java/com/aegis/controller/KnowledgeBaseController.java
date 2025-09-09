package com.aegis.controller;

import com.aegis.agent.KnowledgeBaseAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/kb")
public class KnowledgeBaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseController.class);
    
    @Autowired
    private KnowledgeBaseAgent knowledgeBaseAgent;
    
    /**
     * GET /api/kb/search - Search knowledge base
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchKnowledgeBase(@RequestParam String q) {
        logger.info("Knowledge base search request: query={}", q);
        
        try {
            Map<String, Object> searchResult = knowledgeBaseAgent.searchKnowledgeBase(q);
            
            Map<String, Object> response = new HashMap<>();
            response.put("results", searchResult.get("results"));
            response.put("query", searchResult.get("query"));
            response.put("totalMatches", searchResult.get("totalMatches"));
            response.put("timestamp", java.time.OffsetDateTime.now());
            
            logger.info("Knowledge base search completed: query={}, matches={}", 
                       q, searchResult.get("totalMatches"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error searching knowledge base for query: {}", q, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to search knowledge base"));
        }
    }
    
    /**
     * GET /api/kb/document/{id} - Get specific knowledge base document
     */
    @GetMapping("/document/{id}")
    public ResponseEntity<Map<String, Object>> getDocument(@PathVariable String id) {
        logger.info("Get knowledge base document request: id={}", id);
        
        try {
            Map<String, Object> document = knowledgeBaseAgent.getDocumentById(id);
            
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("document", document);
            response.put("timestamp", java.time.OffsetDateTime.now());
            
            logger.info("Retrieved knowledge base document: id={}", id);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving knowledge base document: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve document"));
        }
    }
}
