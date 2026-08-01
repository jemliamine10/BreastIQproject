package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.AlertResponseDto;
import com.breastcancer.breastcancerbackend.service.AlertEngine;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertEngine alertEngine;

    public AlertController(AlertEngine alertEngine) {
        this.alertEngine = alertEngine;
    }

    @GetMapping("/patient/{patientId}")
    public List<AlertResponseDto> getPatientAlerts(
            @PathVariable("patientId") UUID patientId,
            @RequestParam("doctorId") UUID doctorId,
            @RequestParam(name = "unresolvedOnly", defaultValue = "false") boolean unresolvedOnly
    ) {
        return alertEngine.getPatientAlerts(patientId, doctorId, unresolvedOnly);
    }

    @GetMapping("/doctor/{doctorId}")
    public List<AlertResponseDto> getDoctorAlerts(@PathVariable("doctorId") UUID doctorId) {
        return alertEngine.getDoctorAlerts(doctorId);
    }

    @PutMapping("/{alertId}/resolve")
    public AlertResponseDto resolve(
            @PathVariable("alertId") UUID alertId,
            @RequestParam("doctorUserId") UUID doctorUserId,
            @RequestParam(name = "notes", required = false) String notes
    ) {
        return alertEngine.resolveAlert(alertId, doctorUserId, notes);
    }
}
