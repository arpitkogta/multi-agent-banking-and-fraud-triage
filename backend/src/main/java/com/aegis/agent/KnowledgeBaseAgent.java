package com.aegis.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class KnowledgeBaseAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseAgent.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<Map<String, Object>> kbDocuments;
    
    public KnowledgeBaseAgent() {
        loadKnowledgeBase();
    }
    
    /**
     * Searches the knowledge base for relevant information
     */
    public Map<String, Object> searchKnowledgeBase(String query) {
        logger.debug("Searching knowledge base for query: {}", query);
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            if (query == null || query.trim().isEmpty()) {
                result.put("results", results);
                return result;
            }
            
            String lowerQuery = query.toLowerCase();
            
            for (Map<String, Object> doc : kbDocuments) {
                String title = (String) doc.get("title");
                String anchor = (String) doc.get("anchor");
                List<Map<String, Object>> chunks = (List<Map<String, Object>>) doc.get("chunks");
                
                // Simple keyword matching
                if (title.toLowerCase().contains(lowerQuery) || 
                    anchor.toLowerCase().contains(lowerQuery)) {
                    
                    for (Map<String, Object> chunk : chunks) {
                        String content = (String) chunk.get("content");
                        if (content.toLowerCase().contains(lowerQuery)) {
                            Map<String, Object> match = new HashMap<>();
                            match.put("docId", doc.get("id"));
                            match.put("title", title);
                            match.put("anchor", anchor);
                            match.put("extract", content);
                            match.put("relevance", calculateRelevance(lowerQuery, content));
                            results.add(match);
                        }
                    }
                }
            }
            
            // Sort by relevance
            results.sort((a, b) -> {
                Double relevanceA = (Double) a.get("relevance");
                Double relevanceB = (Double) b.get("relevance");
                return relevanceB.compareTo(relevanceA);
            });
            
            // Limit results
            if (results.size() > 3) {
                results = results.subList(0, 3);
            }
            
            result.put("results", results);
            result.put("query", query);
            result.put("totalMatches", results.size());
            
            logger.debug("Found {} knowledge base matches for query: {}", results.size(), query);
            
        } catch (Exception e) {
            logger.error("Error searching knowledge base for query: {}", query, e);
            result.put("error", "Failed to search knowledge base");
            result.put("results", new ArrayList<>());
        }
        
        return result;
    }
    
    /**
     * Loads knowledge base documents from JSON file
     */
    private void loadKnowledgeBase() {
        try {
            // Try to load from mounted volume first, then fallback to classpath
            File kbFile = new File("/app/fixtures/kb/kb_docs.json");
            InputStream inputStream;
            if (kbFile.exists()) {
                inputStream = new FileInputStream(kbFile);
            } else {
                ClassPathResource resource = new ClassPathResource("fixtures/kb/kb_docs.json");
                inputStream = resource.getInputStream();
            }
            JsonNode rootNode = objectMapper.readTree(inputStream);
            
            kbDocuments = new ArrayList<>();
            for (JsonNode docNode : rootNode) {
                Map<String, Object> doc = new HashMap<>();
                doc.put("id", docNode.get("id").asText());
                doc.put("title", docNode.get("title").asText());
                doc.put("anchor", docNode.get("anchor").asText());
                
                List<Map<String, Object>> chunks = new ArrayList<>();
                for (JsonNode chunkNode : docNode.get("chunks")) {
                    Map<String, Object> chunk = new HashMap<>();
                    chunk.put("id", chunkNode.get("id").asText());
                    chunk.put("content", chunkNode.get("content").asText());
                    chunk.put("metadata", chunkNode.get("metadata"));
                    chunks.add(chunk);
                }
                doc.put("chunks", chunks);
                
                kbDocuments.add(doc);
            }
            
            logger.info("Loaded {} knowledge base documents", kbDocuments.size());
            
        } catch (IOException e) {
            logger.error("Failed to load knowledge base documents", e);
            kbDocuments = new ArrayList<>();
        }
    }
    
    /**
     * Calculates relevance score for a match
     */
    private double calculateRelevance(String query, String content) {
        String lowerContent = content.toLowerCase();
        String[] queryWords = query.split("\\s+");
        
        int matches = 0;
        for (String word : queryWords) {
            if (lowerContent.contains(word)) {
                matches++;
            }
        }
        
        return (double) matches / queryWords.length;
    }
    
    /**
     * Gets a specific knowledge base document by ID
     */
    public Map<String, Object> getDocumentById(String docId) {
        for (Map<String, Object> doc : kbDocuments) {
            if (docId.equals(doc.get("id"))) {
                return doc;
            }
        }
        return null;
    }
}
