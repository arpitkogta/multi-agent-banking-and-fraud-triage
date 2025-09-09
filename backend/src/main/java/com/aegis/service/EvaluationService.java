package com.aegis.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class EvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String triageApiUrl = "http://localhost:8080/api/triage";
    
    @Autowired
    public EvaluationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getEvaluationResults() {
        try {
            return loadCachedResults();
        } catch (Exception e) {
            logger.error("Failed to load cached results", e);
            return createEmptyResults();
        }
    }
    
    public Map<String, Object> runEvaluations() {
        logger.info("Starting real API evaluations...");
        
        Map<String, Object> results = new HashMap<>();
        List<Map<String, Object>> testCases = new ArrayList<>();
        List<Long> latencies = new ArrayList<>();
        int totalTests = 0;
        int passedTests = 0;
        int fallbackCount = 0;
        Map<String, Integer> policyDenials = new HashMap<>();
        
        try {
            List<Map<String, Object>> evalCases = loadEvaluationCases();
            
            for (Map<String, Object> testCase : evalCases) {
                totalTests++;
                String caseId = (String) testCase.get("id");
                String caseName = (String) testCase.get("name");
                
                logger.info("Running test case: {} - {}", caseId, caseName);
                
                long startTime = System.currentTimeMillis();
                Map<String, Object> testResult = runSingleTestCase(testCase);
                long duration = System.currentTimeMillis() - startTime;
                latencies.add(duration);
                
                boolean passed = (Boolean) testResult.getOrDefault("passed", false);
                if (passed) {
                    passedTests++;
                }
                
                if ((Boolean) testResult.getOrDefault("fallbackUsed", false)) {
                    fallbackCount++;
                }
                
                updatePolicyDenials(testResult, policyDenials);
                
                testResult.put("duration", duration / 1000.0);
                testResult.put("status", passed ? "PASS" : "FAIL");
                testCases.add(testResult);
            }
            
            results.put("taskSuccessRate", totalTests > 0 ? (double) passedTests / totalTests * 100 : 0.0);
            results.put("fallbackRate", totalTests > 0 ? (double) fallbackCount / totalTests * 100 : 0.0);
            results.put("avgLatencyP50", calculatePercentile(latencies, 50) / 1000.0);
            results.put("avgLatencyP95", calculatePercentile(latencies, 95) / 1000.0);
            results.put("policyDenials", policyDenials);
            results.put("testCases", testCases);
            results.put("confusionMatrix", calculateConfusionMatrix(testCases));
            
            saveResults(results);
            logger.info("Evaluation completed: {}/{} tests passed", passedTests, totalTests);
            
        } catch (Exception e) {
            logger.error("Error running evaluations", e);
            results.put("error", "Failed to run evaluations: " + e.getMessage());
        }
        
        return results;
    }
    
    private List<Map<String, Object>> loadEvaluationCases() throws Exception {
        List<Map<String, Object>> cases = new ArrayList<>();
        String[] caseFiles = {
            "case_001_card_lost.json", "case_002_unauthorized_charge.json", "case_003_duplicate_charges.json",
            "case_004_geo_velocity.json", "case_005_device_mcc_anomaly.json", "case_006_chargeback_escalation.json",
            "case_007_risk_timeout.json", "case_008_rate_limit.json", "case_009_policy_block.json",
            "case_010_pii_redaction.json", "case_011_kb_faq.json", "case_012_merchant_disambiguation.json"
        };
        
        for (String caseFile : caseFiles) {
            try {
                String path = "src/main/resources/fixtures/evals/" + caseFile;
                String content = new String(Files.readAllBytes(Paths.get(path)));
                Map<String, Object> testCase = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
                cases.add(testCase);
            } catch (Exception e) {
                logger.warn("Failed to load test case: {}", caseFile, e);
            }
        }
        
        return cases;
    }
    
    private Map<String, Object> runSingleTestCase(Map<String, Object> testCase) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", testCase.get("id"));
        result.put("name", testCase.get("name"));
        
        try {
            Map<String, Object> input = (Map<String, Object>) testCase.get("input");
            Map<String, Object> expected = (Map<String, Object>) testCase.get("expected");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(input, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(triageApiUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                boolean passed = validateTestCase(responseBody, expected);
                result.put("passed", passed);
                result.put("response", responseBody);
                result.put("expected", expected);
            } else {
                result.put("passed", false);
                result.put("error", "HTTP " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error running test case: {}", testCase.get("id"), e);
            result.put("passed", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    private boolean validateTestCase(Map<String, Object> actual, Map<String, Object> expected) {
        try {
            String expectedRiskScore = (String) expected.get("riskScore");
            String actualRiskScore = (String) actual.get("riskScore");
            
            if (!expectedRiskScore.equals(actualRiskScore)) {
                logger.debug("Risk score mismatch: expected={}, actual={}", expectedRiskScore, actualRiskScore);
                return false;
            }
            
            String expectedAction = (String) expected.get("recommendedAction");
            String actualAction = (String) actual.get("recommendedAction");
            
            if (expectedAction != null && !expectedAction.equals(actualAction)) {
                logger.debug("Action mismatch: expected={}, actual={}", expectedAction, actualAction);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error validating test case", e);
            return false;
        }
    }
    
    private void updatePolicyDenials(Map<String, Object> testResult, Map<String, Integer> policyDenials) {
        Map<String, Object> response = (Map<String, Object>) testResult.get("response");
        if (response != null && response.containsKey("policyDenials")) {
            Map<String, Integer> denials = (Map<String, Integer>) response.get("policyDenials");
            for (Map.Entry<String, Integer> entry : denials.entrySet()) {
                policyDenials.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
    }
    
    private long calculatePercentile(List<Long> values, int percentile) {
        if (values.isEmpty()) return 0;
        values.sort(Long::compareTo);
        int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
        return values.get(Math.max(0, Math.min(index, values.size() - 1)));
    }
    
    private Map<String, Map<String, Integer>> calculateConfusionMatrix(List<Map<String, Object>> testCases) {
        Map<String, Map<String, Integer>> matrix = new HashMap<>();
        
        for (String actual : Arrays.asList("low", "medium", "high")) {
            matrix.put(actual, new HashMap<>());
            for (String predicted : Arrays.asList("low", "medium", "high")) {
                matrix.get(actual).put(predicted, 0);
            }
        }
        
        for (Map<String, Object> testCase : testCases) {
            Map<String, Object> expected = (Map<String, Object>) testCase.get("expected");
            Map<String, Object> response = (Map<String, Object>) testCase.get("response");
            
            if (expected != null && response != null) {
                String actualRisk = (String) expected.get("riskScore");
                String predictedRisk = (String) response.get("riskScore");
                
                if (actualRisk != null && predictedRisk != null) {
                    matrix.get(actualRisk).merge(predictedRisk, 1, Integer::sum);
                }
            }
        }
        
        return matrix;
    }
    
    private Map<String, Object> loadCachedResults() throws Exception {
        String path = "eval-results.json";
        if (Files.exists(Paths.get(path))) {
            String content = new String(Files.readAllBytes(Paths.get(path)));
            return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
        }
        return createEmptyResults();
    }
    
    private void saveResults(Map<String, Object> results) {
        try {
            String json = objectMapper.writeValueAsString(results);
            Files.write(Paths.get("eval-results.json"), json.getBytes());
        } catch (Exception e) {
            logger.error("Failed to save results", e);
        }
    }
    
    private Map<String, Object> createEmptyResults() {
        Map<String, Object> results = new HashMap<>();
        results.put("taskSuccessRate", 0.0);
        results.put("fallbackRate", 0.0);
        results.put("avgLatencyP50", 0.0);
        results.put("avgLatencyP95", 0.0);
        results.put("policyDenials", new HashMap<>());
        results.put("testCases", new ArrayList<>());
        results.put("confusionMatrix", new HashMap<>());
        return results;
    }
}
