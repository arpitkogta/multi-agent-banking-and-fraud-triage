package com.aegis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class PiiRedactionService {
    
    private static final Logger logger = LoggerFactory.getLogger(PiiRedactionService.class);
    
    private final List<Pattern> redactionPatterns;
    private final String replacement;
    
    public PiiRedactionService(@Value("${aegis.security.pii.replacement:****REDACTED****}") String replacement) {
        this.replacement = replacement;
        this.redactionPatterns = List.of(
            // Credit card numbers (13-19 digits)
            Pattern.compile("\\b\\d{13,19}\\b"),
            // Email addresses
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
            // Phone numbers (Indian format)
            Pattern.compile("\\b[6-9]\\d{9}\\b"),
            // PAN numbers (Indian format)
            Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]{1}\\b"),
            // Aadhaar numbers (12 digits)
            Pattern.compile("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\b")
        );
    }
    
    /**
     * Redacts PII from the given text
     * @param text The text to redact
     * @return Redacted text with PII replaced
     */
    public String redactPii(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String redactedText = text;
        boolean hasPii = false;
        
        for (Pattern pattern : redactionPatterns) {
            if (pattern.matcher(redactedText).find()) {
                redactedText = pattern.matcher(redactedText).replaceAll(replacement);
                hasPii = true;
            }
        }
        
        if (hasPii) {
            logger.debug("PII redacted from text: {} -> {}", text, redactedText);
        }
        
        return redactedText;
    }
    
    /**
     * Checks if the given text contains PII
     * @param text The text to check
     * @return true if PII is detected
     */
    public boolean containsPii(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        return redactionPatterns.stream()
            .anyMatch(pattern -> pattern.matcher(text).find());
    }
    
    /**
     * Redacts PII from a JSON string
     * @param jsonString The JSON string to redact
     * @return Redacted JSON string
     */
    public String redactPiiFromJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return jsonString;
        }
        
        return redactPii(jsonString);
    }
    
    /**
     * Creates a log-safe version of customer ID for logging
     * @param customerId The customer ID to mask
     * @return Masked customer ID
     */
    public String maskCustomerId(String customerId) {
        if (customerId == null || customerId.length() < 4) {
            return "****";
        }
        return customerId.substring(0, 2) + "***" + customerId.substring(customerId.length() - 2);
    }
}
