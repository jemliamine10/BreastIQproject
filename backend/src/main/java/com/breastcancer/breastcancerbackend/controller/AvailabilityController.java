package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("/doctors/{doctorId}/availability")
    public AvailabilityResponseDto create(
            @PathVariable UUID doctorId,
            @Valid @RequestBody AvailabilityUpsertRequestDto dto
    ) {
        return availabilityService.create(doctorId, dto);
    }

    @GetMapping("/doctors/{doctorId}/availability")
    public List<AvailabilityResponseDto> listByDoctor(@PathVariable UUID doctorId) {
        return availabilityService.listByDoctor(doctorId);
    }

    @PutMapping("/availability/{availabilityId}")
    public AvailabilityResponseDto update(
            @PathVariable UUID availabilityId,
            @Valid @RequestBody AvailabilityUpsertRequestDto dto
    ) {
        return availabilityService.update(availabilityId, dto);
    }

    @DeleteMapping("/availability/{availabilityId}")
    public void softDelete(@PathVariable UUID availabilityId) {
        availabilityService.softDelete(availabilityId);
    }

    @PostMapping("/doctors/{doctorId}/availability/exceptions")
    public AvailabilityExceptionResponseDto createException(
            @PathVariable UUID doctorId,
            @RequestBody AvailabilityExceptionUpsertRequestDto dto
    ) {
        return availabilityService.createException(doctorId, dto);
    }

    @GetMapping("/doctors/{doctorId}/availability/exceptions")
    public List<AvailabilityExceptionResponseDto> listExceptionsByDoctor(@PathVariable UUID doctorId) {
        return availabilityService.listExceptionsByDoctor(doctorId);
    }

    @PutMapping("/availability-exceptions/{exceptionId}")
    public AvailabilityExceptionResponseDto updateException(
            @PathVariable UUID exceptionId,
            @RequestBody AvailabilityExceptionUpsertRequestDto dto
    ) {
        return availabilityService.updateException(exceptionId, dto);
    }

    @DeleteMapping("/availability-exceptions/{exceptionId}")
    public void softDeleteException(@PathVariable UUID exceptionId) {
        availabilityService.softDeleteException(exceptionId);
    }
}
