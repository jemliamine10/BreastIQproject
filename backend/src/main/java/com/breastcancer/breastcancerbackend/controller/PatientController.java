package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.Treatment;
import com.breastcancer.breastcancerbackend.service.PatientService;
import com.breastcancer.breastcancerbackend.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final MedicalRecordService medicalRecordService;

    public PatientController(PatientService patientService, MedicalRecordService medicalRecordService) {
        this.patientService = patientService;
        this.medicalRecordService = medicalRecordService;
    }

    // ===== Patient Profile =====

    // ✅ GET /api/patients/{patientProfileId}
    @GetMapping("/{patientProfileId}")
    public PatientProfileResponseDto getById(@PathVariable UUID patientProfileId) {
        return patientService.getById(patientProfileId);
    }

    // ✅ GET /api/patients/by-user/{userId}
    @GetMapping("/by-user/{userId}")
    public PatientProfileResponseDto getByUserId(@PathVariable UUID userId) {
        return patientService.getByUserId(userId);
    }

    // ✅ PUT /api/patients/{patientProfileId}
    @PutMapping("/{patientProfileId}")
    public PatientProfileResponseDto update(
            @PathVariable UUID patientProfileId,
            @Valid @RequestBody PatientProfileUpdateRequestDto dto
    ) {
        return patientService.update(patientProfileId, dto);
    }

    // ✅ PUT /api/patients/{patientProfileId}/location
    @PutMapping("/{patientProfileId}/location")
    public PatientProfileResponseDto updateLocation(
            @PathVariable UUID patientProfileId,
            @Valid @RequestBody LocationUpdateRequestDto dto
    ) {
        return patientService.updateLocation(patientProfileId, dto);
    }

    // ✅ PUT /api/patients/{patientProfileId}/consent?value=true/false
    @PutMapping("/{patientProfileId}/consent")
    public PatientProfileResponseDto setMedicalConsent(
            @PathVariable UUID patientProfileId,
            @RequestParam("value") boolean consent
    ) {
        return patientService.setMedicalConsent(patientProfileId, consent);
    }

    // ===== Allergies =====

    // ✅ POST /api/patients/allergies
    @PostMapping("/allergies")
    public AllergyResponseDto addAllergy(@Valid @RequestBody AllergyCreateRequestDto dto) {
        return patientService.addAllergy(dto);
    }

    // ✅ GET /api/patients/{patientProfileId}/allergies
    @GetMapping("/{patientProfileId}/allergies")
    public List<AllergyResponseDto> listAllergies(@PathVariable UUID patientProfileId) {
        return patientService.listAllergies(patientProfileId);
    }

    // ✅ PUT /api/patients/allergies/{allergyId}
    @PutMapping("/allergies/{allergyId}")
    public AllergyResponseDto updateAllergy(
            @PathVariable UUID allergyId,
            @Valid @RequestBody AllergyUpdateRequestDto dto
    ) {
        return patientService.updateAllergy(allergyId, dto);
    }

    // ✅ DELETE /api/patients/allergies/{allergyId}
    @DeleteMapping("/allergies/{allergyId}")
    public void deleteAllergy(@PathVariable UUID allergyId) {
        patientService.deleteAllergy(allergyId);
    }

    // ===== Treatments =====

    // ✅ POST /api/patients/treatments
    @PostMapping("/treatments")
    public TreatmentResponseDto addTreatment(@Valid @RequestBody TreatmentCreateRequestDto dto) {
        return patientService.addTreatment(dto);
    }

    // ✅ GET /api/patients/{patientProfileId}/treatments
    @GetMapping("/{patientProfileId}/treatments")
    public List<TreatmentResponseDto> listTreatments(@PathVariable UUID patientProfileId) {
        return patientService.listTreatments(patientProfileId);
    }

    // ✅ GET /api/patients/{patientProfileId}/treatments/by-status?status=ACTIVE...
    @GetMapping("/{patientProfileId}/treatments/by-status")
    public List<TreatmentResponseDto> listTreatmentsByStatus(
            @PathVariable UUID patientProfileId,
            @RequestParam Treatment.Status status
    ) {
        return patientService.listTreatmentsByStatus(patientProfileId, status);
    }

    // ✅ PUT /api/patients/treatments/{treatmentId}
    @PutMapping("/treatments/{treatmentId}")
    public TreatmentResponseDto updateTreatment(
            @PathVariable UUID treatmentId,
            @Valid @RequestBody TreatmentUpdateRequestDto dto
    ) {
        return patientService.updateTreatment(treatmentId, dto);
    }

    // ✅ DELETE /api/patients/treatments/{treatmentId}
    @DeleteMapping("/treatments/{treatmentId}")
    public void deleteTreatment(@PathVariable UUID treatmentId) {
        patientService.deleteTreatment(treatmentId);
    }

    // Endpoints removed to avoid clash and moved to MedicalRecordController
}
