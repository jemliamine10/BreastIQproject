package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.PatientDashboardDto;
import com.breastcancer.breastcancerbackend.service.PatientDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/patient/dashboard")
public class PatientDashboardController {

    private final PatientDashboardService dashboardService;

    public PatientDashboardController(PatientDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{patientProfileId}")
    public ResponseEntity<PatientDashboardDto> getDashboard(@PathVariable UUID patientProfileId) {
        PatientDashboardDto dto = dashboardService.getDashboard(patientProfileId);
        return ResponseEntity.ok(dto);
    }
}
