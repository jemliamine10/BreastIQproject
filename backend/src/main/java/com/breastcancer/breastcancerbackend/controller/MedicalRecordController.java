package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.MedicalRecord;
import com.breastcancer.breastcancerbackend.service.MedicalRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    /**
     * GET /api/medical-records/patient/{patientId}
     * Fetch the full aggregated medical record for a patient.
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<MedicalRecordResponseDto> getFullRecord(
            @PathVariable UUID patientId,
            @RequestParam UUID doctorId
    ) {
        return ResponseEntity.ok(medicalRecordService.getFullRecord(patientId, doctorId));
    }

    /**
     * POST /api/medical-records
     * Create a new medical record for a patient.
     */
    @PostMapping
    public ResponseEntity<MedicalRecordResponseDto> createRecord(
            @RequestBody MedicalRecordCreateDto dto,
            @RequestParam UUID doctorId
    ) {
        return ResponseEntity.ok(medicalRecordService.createFullRecord(dto, doctorId));
    }

    /**
     * PUT /api/medical-records/patient/{patientId}/diagnosis
     * Update diagnosis, stage, and tumor type.
     */
    @PutMapping("/patient/{patientId}/diagnosis")
    public ResponseEntity<MedicalRecordResponseDto> updateDiagnosis(
            @PathVariable UUID patientId,
            @RequestParam UUID doctorId,
            @RequestParam(required = false) String diagnosis,
            @RequestParam(required = false) MedicalRecord.CancerStage cancerStage,
            @RequestParam(required = false) MedicalRecord.TumorType tumorType
    ) {
        return ResponseEntity.ok(medicalRecordService.updateDiagnosis(patientId, doctorId, diagnosis, cancerStage, tumorType));
    }

    /**
     * PUT /api/medical-records/patient/{patientId}/clinical-data
     * Update clinical parameters (ER, PR, HER2, etc.)
     */
    @PutMapping("/patient/{patientId}/clinical-data")
    public ResponseEntity<ClinicalDataDto> updateClinicalData(
            @PathVariable UUID patientId,
            @RequestParam UUID doctorId,
            @RequestBody ClinicalDataDto dto
    ) {
        return ResponseEntity.ok(medicalRecordService.updateClinicalData(patientId, doctorId, dto));
    }

    /**
     * POST /api/medical-records/patient/{patientId}/history
     * Add a medical history entry.
     */
    @PostMapping("/patient/{patientId}/history")
    public ResponseEntity<MedicalHistoryDto> addHistory(
            @PathVariable UUID patientId,
            @RequestParam UUID doctorId,
            @RequestBody MedicalHistoryDto dto
    ) {
        return ResponseEntity.ok(medicalRecordService.addMedicalHistory(patientId, doctorId, dto));
    }

    /**
     * DELETE /api/medical-records/history/{historyId}
     * Soft-delete a medical history entry.
     */
    @DeleteMapping("/history/{historyId}")
    public ResponseEntity<Void> deleteHistory(
            @PathVariable UUID historyId,
            @RequestParam UUID doctorId
    ) {
        medicalRecordService.deleteMedicalHistory(historyId, doctorId);
        return ResponseEntity.noContent().build();
    }
}
