package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.MedicalEventResponseDto;
import com.breastcancer.breastcancerbackend.entity.MedicalEvent;
import com.breastcancer.breastcancerbackend.service.TimelineService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/timeline")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/patient/{patientId}")
    public List<MedicalEventResponseDto> getTimeline(
            @PathVariable("patientId") UUID patientId,
            @RequestParam("doctorId") UUID doctorId
    ) {
        return timelineService.getTimeline(patientId, doctorId);
    }

    @GetMapping("/patient/{patientId}/events")
    public List<MedicalEventResponseDto> getMyTimeline(
            @PathVariable("patientId") UUID patientId
    ) {
        return timelineService.getTimelineForPatient(patientId);
    }

    @GetMapping("/patient/{patientId}/type/{eventType}")
    public List<MedicalEventResponseDto> getByType(
            @PathVariable("patientId") UUID patientId,
            @RequestParam("doctorId") UUID doctorId,
            @PathVariable("eventType") MedicalEvent.EventType eventType
    ) {
        return timelineService.getTimelineByType(patientId, doctorId, eventType);
    }
}
