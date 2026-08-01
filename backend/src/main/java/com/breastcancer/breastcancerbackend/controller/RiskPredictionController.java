package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.RiskPredictionRequestDto;
import com.breastcancer.breastcancerbackend.dto.RiskPredictionResponseDto;
import com.breastcancer.breastcancerbackend.service.RiskPredictionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for breast cancer recurrence risk prediction.
 */
@RestController
@RequestMapping("/api/v1/risk-prediction")
public class RiskPredictionController {

    private final RiskPredictionService riskPredictionService;

    public RiskPredictionController(RiskPredictionService riskPredictionService) {
        this.riskPredictionService = riskPredictionService;
    }

    /**
     * Check if the risk prediction AI service is available.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        boolean healthy = riskPredictionService.isRiskServiceHealthy();
        return ResponseEntity.ok(Map.of(
                "riskServiceAvailable", healthy,
                "message", healthy ? "Risk prediction service is ready" : "Risk prediction service is not available"
        ));
    }

    /**
     * Direct prediction with all 22 features provided manually.
     */
    @PostMapping("/predict")
    public ResponseEntity<?> predict(@RequestBody RiskPredictionRequestDto request) {
        try {
            RiskPredictionResponseDto result = riskPredictionService.predict(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Prediction failed",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Predict for a known patient — auto-populates features from DB,
     * then merges with manually-provided overrides.
     */
    @PostMapping("/predict-for-patient/{patientId}")
    public ResponseEntity<?> predictForPatient(
            @PathVariable String patientId,
            @RequestBody RiskPredictionRequestDto overrides
    ) {
        try {
            RiskPredictionResponseDto result = riskPredictionService.predictForPatient(
                    UUID.fromString(patientId), overrides
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Prediction failed",
                    "message", e.getMessage()
            ));
        }
    }
}
