package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.TreatmentSessionResponseDto;
import com.breastcancer.breastcancerbackend.dto.TreatmentResponseDto;
import com.breastcancer.breastcancerbackend.entity.Treatment;
import com.breastcancer.breastcancerbackend.service.TreatmentManagementService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/treatment-management")
public class TreatmentManagementController {

    private final TreatmentManagementService treatmentService;

    public TreatmentManagementController(TreatmentManagementService treatmentService) {
        this.treatmentService = treatmentService;
    }

    // ===== Treatment lifecycle =====

    @PostMapping
    public void createTreatment(
            @RequestParam("doctorId") UUID doctorId,
            @RequestParam("patientId") UUID patientId,
            @RequestParam("type") Treatment.TreatmentType type,
            @RequestParam(name = "protocol", required = false) String protocol,
            @RequestParam(name = "medicationName", required = false) String medicationName,
            @RequestParam(name = "dosage", required = false) String dosage,
            @RequestParam(name = "startDate", required = false) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) LocalDate endDate,
            @RequestParam(name = "cyclesTotal", required = false) Integer cyclesTotal,
            @RequestParam(name = "intervalDays", defaultValue = "21") int intervalDays,
            @RequestParam(name = "notes", required = false) String notes
    ) {
            treatmentService.createTreatment(doctorId, patientId, type, protocol, medicationName, dosage,
                startDate, endDate, cyclesTotal, intervalDays, notes);
    }

    @DeleteMapping("/{treatmentId}")
            public void softDelete(
                @PathVariable("treatmentId") UUID treatmentId,
                @RequestParam("doctorId") UUID doctorId
            ) {
            treatmentService.softDeleteTreatment(doctorId, treatmentId);
    }

    @GetMapping("/patient/{patientId}/treatments")
    public List<TreatmentResponseDto> listPatientTreatments(
            @PathVariable("patientId") UUID patientId,
            @RequestParam("doctorId") UUID doctorId,
            @RequestParam(name = "status", required = false) String status
    ) {
        return treatmentService.listTreatmentsForLinkedPair(doctorId, patientId, status);
    }

    // ===== Session tracking =====

    @GetMapping("/{treatmentId}/sessions")
    public List<TreatmentSessionResponseDto> getSessions(
            @PathVariable("treatmentId") UUID treatmentId,
            @RequestParam("doctorId") UUID doctorId
    ) {
        return treatmentService.getSessions(doctorId, treatmentId);
    }

    @PutMapping("/sessions/{sessionId}/done")
    public TreatmentSessionResponseDto markDone(
            @PathVariable("sessionId") UUID sessionId,
            @RequestParam("doctorId") UUID doctorId,
            @RequestParam(name = "notes", required = false) String notes,
            @RequestParam(name = "sideEffects", required = false) String sideEffects
    ) {
        return treatmentService.markSessionDone(doctorId, sessionId, notes, sideEffects);
    }

    @PutMapping("/sessions/{sessionId}/missed")
    public TreatmentSessionResponseDto markMissed(
            @PathVariable("sessionId") UUID sessionId,
            @RequestParam("doctorId") UUID doctorId,
            @RequestParam(name = "reason", required = false) String reason
    ) {
        return treatmentService.markSessionMissed(doctorId, sessionId, reason);
    }

    // ===== Refresh statuses =====

    @PostMapping("/patient/{patientId}/refresh")
    public void refreshStatuses(
            @PathVariable("patientId") UUID patientId,
            @RequestParam("doctorId") UUID doctorId
    ) {
        treatmentService.refreshAllTreatmentStatuses(doctorId, patientId);
    }
}
