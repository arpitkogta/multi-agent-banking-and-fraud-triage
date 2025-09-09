package com.aegis.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public abstract class BaseAgent {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    protected Map<String, Object> createErrorResponse(String error) {
        Map<String, Object> result = new HashMap<>();
        result.put("error", error);
        return result;
    }
    
    protected Map<String, Object> createSuccessResponse(Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        result.putAll(data);
        return result;
    }
    
    protected boolean isValidInput(String... inputs) {
        for (String input : inputs) {
            if (input == null || input.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}