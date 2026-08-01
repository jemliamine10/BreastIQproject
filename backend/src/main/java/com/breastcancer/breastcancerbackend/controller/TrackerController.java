package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.TrackerEntryCreateDto;
import com.breastcancer.breastcancerbackend.dto.TrackerEntryResponseDto;
import com.breastcancer.breastcancerbackend.service.TrackerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tracker")
public class TrackerController {

    private final TrackerService trackerService;

    public TrackerController(TrackerService trackerService) {
        this.trackerService = trackerService;
    }

    /**
     * Submit a daily tracker entry.
     * This triggers the full monitoring chain:
     *   Save → AlertEngine → RiskEngine → HealthScore → Timeline → WebSocket
     */
    @PostMapping
    public TrackerEntryResponseDto submit(
            @Valid @RequestBody TrackerEntryCreateDto dto,
            @RequestParam(name = "doctorId", required = false) UUID doctorId
    ) {
        return trackerService.submitEntry(dto, doctorId);
    }

    @GetMapping("/patient/{patientId}")
    public List<TrackerEntryResponseDto> getHistory(
            @PathVariable("patientId") UUID patientId,
            @RequestParam("doctorId") UUID doctorId
    ) {
        return trackerService.getHistory(patientId, doctorId);
    }

    @GetMapping("/patient/{patientId}/latest")
    public TrackerEntryResponseDto getLatest(
            @PathVariable("patientId") UUID patientId,
            @RequestParam("doctorId") UUID doctorId
    ) {
        return trackerService.getLatest(patientId, doctorId);
    }

    @GetMapping("/my-history/{patientId}")
    public List<TrackerEntryResponseDto> getMyHistory(@PathVariable("patientId") UUID patientId) {
        return trackerService.getHistoryForPatient(patientId);
    }

    @GetMapping("/my-latest/{patientId}")
    public TrackerEntryResponseDto getMyLatest(@PathVariable("patientId") UUID patientId) {
        return trackerService.getLatestForPatient(patientId);
    }
}
