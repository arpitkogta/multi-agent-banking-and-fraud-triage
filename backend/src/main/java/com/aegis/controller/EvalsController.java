package com.aegis.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.aegis.service.EvaluationService;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/evals")
@CrossOrigin(origins = "*")
public class EvalsController {

    private final EvaluationService evaluationService;

    public EvalsController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "EvalsController is working!");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/results")
    public ResponseEntity<Map<String, Object>> getEvaluationResults() {
        try {
            Map<String, Object> results = evaluationService.getEvaluationResults();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get evaluation results: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runEvaluations() {
        try {
            Map<String, Object> results = evaluationService.runEvaluations();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to run evaluations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
