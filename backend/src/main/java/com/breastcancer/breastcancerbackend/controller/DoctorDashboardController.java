package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.DoctorDashboardDto;
import com.breastcancer.breastcancerbackend.service.DoctorDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/doctor/dashboard")
public class DoctorDashboardController {

    private final DoctorDashboardService dashboardService;

    public DoctorDashboardController(DoctorDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{doctorProfileId}")
    public ResponseEntity<DoctorDashboardDto> getDashboard(@PathVariable UUID doctorProfileId) {
        DoctorDashboardDto dto = dashboardService.getDashboard(doctorProfileId);
        return ResponseEntity.ok(dto);
    }
}
