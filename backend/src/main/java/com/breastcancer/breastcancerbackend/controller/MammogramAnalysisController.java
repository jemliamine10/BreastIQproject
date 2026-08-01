package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.MammogramAnalysisDetailDto;
import com.breastcancer.breastcancerbackend.dto.MammogramAnalysisHistoryDto;
import com.breastcancer.breastcancerbackend.dto.MammogramAnalysisResponseDto;
import com.breastcancer.breastcancerbackend.service.MammogramAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for mammogram AI analysis.
 * Provides endpoints for doctors to upload mammogram images, link analyses to patients,
 * view history, and generate AI reports.
 */
@RestController
@RequestMapping("/api/v1")
public class MammogramAnalysisController {

    private final MammogramAnalysisService analysisService;

    public MammogramAnalysisController(MammogramAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * Check if the AI service is available.
     */
    @GetMapping("/mammogram/health")
    public ResponseEntity<Map<String, Object>> checkAiHealth() {
        boolean healthy = analysisService.isAiServiceHealthy();
        return ResponseEntity.ok(Map.of(
                "aiServiceAvailable", healthy,
                "message", healthy ? "AI service is ready" : "AI service is not available"
        ));
    }

    /**
     * Analyze a mammogram image linked to a patient.
     */
    @PostMapping("/doctor/{doctorId}/mammogram/analyze")
    public ResponseEntity<?> analyzeMammogram(
            @PathVariable String doctorId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("patientProfileId") String patientProfileId,
            @RequestParam(value = "pixelSpacing", required = false, defaultValue = "0.1") String pixelSpacing
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No file provided",
                    "message", "Please upload a mammogram image"
            ));
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid file",
                    "message", "File must have a valid name"
            ));
        }

        String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        if (!ext.matches("\\.(png|jpg|jpeg|dcm|dicom)")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Unsupported file type",
                    "message", "Accepted formats: PNG, JPG, JPEG, DICOM"
            ));
        }

        try {
            MammogramAnalysisResponseDto result = analysisService.analyzeMammogram(
                    file, pixelSpacing,
                    UUID.fromString(patientProfileId),
                    UUID.fromString(doctorId)
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Analysis failed",
                    "message", "Unable to analyze image: " + e.getMessage()
            ));
        }
    }

    /**
     * Get all analysis history for a doctor (all patients).
     */
    @GetMapping("/doctor/{doctorId}/mammogram/history")
    public ResponseEntity<List<MammogramAnalysisHistoryDto>> getDoctorHistory(
            @PathVariable String doctorId
    ) {
        List<MammogramAnalysisHistoryDto> history = analysisService.getHistoryByDoctor(UUID.fromString(doctorId));
        return ResponseEntity.ok(history);
    }

    /**
     * Get analysis history for a specific patient under a doctor.
     */
    @GetMapping("/doctor/{doctorId}/mammogram/patient/{patientId}/history")
    public ResponseEntity<List<MammogramAnalysisHistoryDto>> getPatientHistory(
            @PathVariable String doctorId,
            @PathVariable String patientId
    ) {
        List<MammogramAnalysisHistoryDto> history = analysisService.getHistoryByPatientAndDoctor(
                UUID.fromString(patientId), UUID.fromString(doctorId)
        );
        return ResponseEntity.ok(history);
    }

    /**
     * Get detailed view of a specific analysis (with images reloaded from disk).
     */
    @GetMapping("/mammogram/analysis/{analysisId}")
    public ResponseEntity<MammogramAnalysisDetailDto> getAnalysisDetail(
            @PathVariable String analysisId
    ) {
        MammogramAnalysisDetailDto detail = analysisService.getAnalysisDetail(UUID.fromString(analysisId));
        return ResponseEntity.ok(detail);
    }

    /**
     * Generate (or regenerate) an AI report for an existing analysis.
     */
    @PostMapping("/mammogram/analysis/{analysisId}/report")
    public ResponseEntity<Map<String, Object>> generateReport(
            @PathVariable String analysisId
    ) {
        try {
            String report = analysisService.generateReport(UUID.fromString(analysisId));
            return ResponseEntity.ok(Map.of(
                    "report", report,
                    "analysisId", analysisId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Report generation failed",
                    "message", e.getMessage()
            ));
        }
    }
}
