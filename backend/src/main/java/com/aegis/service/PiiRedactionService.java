package com.aegis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

@Service
public class PiiRedactionService {
    
    private static final Logger logger = LoggerFactory.getLogger(PiiRedactionService.class);
    private static final int CACHE_SIZE = 10_000;
    
    // Single compiled pattern for better performance
    private static final Pattern PII_PATTERN = Pattern.compile(
        "\\b(?:" +
        // Credit card numbers (major networks)
        "(?:4[0-9]{12}(?:[0-9]{3})?|" +          // Visa
        "5[1-5][0-9]{14}|" +                      // Mastercard
        "3[47][0-9]{13}|" +                       // American Express
        "6(?:011|5[0-9]{2})[0-9]{12}|" +         // Discover
        "3(?:0[0-5]|[68][0-9])[0-9]{11}|" +      // Diners Club
        "(?:2131|1800|35\\d{3})\\d{11})|" +      // JCB
        // Other PII
        "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}|" +  // Email
        "[6-9]\\d{9}|" +                          // Phone (IN)
        "[A-Z]{5}[0-9]{4}[A-Z]|" +               // PAN
        "\\d{4}\\s?\\d{4}\\s?\\d{4}" +           // Aadhaar
        ")\\b"
    );
    
    private final String replacement;
    private final Cache<String, Boolean> detectionCache;
    private final Cache<String, String> redactionCache;
    
    public PiiRedactionService(
            @Value("${aegis.security.pii.replacement:****REDACTED****}") String replacement) {
        this.replacement = replacement;
        
        this.detectionCache = Caffeine.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();
            
        this.redactionCache = Caffeine.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();
    }
    
    public boolean containsPii(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        return detectionCache.get(text, k -> PII_PATTERN.matcher(text).find());
    }
    
    public String redactPii(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        return redactionCache.get(text, k -> {
            StringBuilder result = new StringBuilder(text.length());
            Matcher matcher = PII_PATTERN.matcher(text);
            int lastEnd = 0;
            
            while (matcher.find()) {
                result.append(text, lastEnd, matcher.start())
                      .append(replacement);
                lastEnd = matcher.end();
            }
            
            if (lastEnd < text.length()) {
                result.append(text, lastEnd, text.length());
            }
            
            return result.toString();
        });
    }
    
    public String maskCustomerId(String customerId) {
        if (customerId == null || customerId.length() < 4) {
            return "****";
        }
        return customerId.substring(0, 2) + "***" + 
               customerId.substring(customerId.length() - 2);
    }
}
