package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.DoctorProfileResponseDto;
import com.breastcancer.breastcancerbackend.dto.DoctorProfileUpdateRequestDto;
import com.breastcancer.breastcancerbackend.dto.DoctorCalendarDto;
import com.breastcancer.breastcancerbackend.entity.DoctorProfile;
import com.breastcancer.breastcancerbackend.service.CalendarService;
import com.breastcancer.breastcancerbackend.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final CalendarService calendarService;

    public DoctorController(DoctorService doctorService, CalendarService calendarService) {
        this.doctorService = doctorService;
        this.calendarService = calendarService;
    }

    // ✅ GET /api/doctors/{doctorProfileId}
    @GetMapping("/{doctorProfileId}")
    public DoctorProfileResponseDto getById(@PathVariable UUID doctorProfileId) {
        return doctorService.getById(doctorProfileId);
    }

    // ✅ GET /api/doctors/by-user/{userId}
    @GetMapping("/by-user/{userId}")
    public DoctorProfileResponseDto getByUserId(@PathVariable UUID userId) {
        return doctorService.getByUserId(userId);
    }

    // ✅ PUT /api/doctors/{doctorProfileId}
    @PutMapping("/{doctorProfileId}")
    public DoctorProfileResponseDto update(
            @PathVariable UUID doctorProfileId,
            @Valid @RequestBody DoctorProfileUpdateRequestDto dto
    ) {
        return doctorService.update(doctorProfileId, dto);
    }

    // ✅ PUT /api/doctors/{doctorProfileId}/location
    @PutMapping("/{doctorProfileId}/location")
    public DoctorProfileResponseDto updateLocation(
            @PathVariable UUID doctorProfileId,
            @RequestParam(required = false) String addressText,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        return doctorService.updateLocation(doctorProfileId, addressText, lat, lon);
    }

    // ✅ PUT /api/doctors/{doctorProfileId}/verified?value=true/false
    @PutMapping("/{doctorProfileId}/verified")
    public DoctorProfileResponseDto setVerified(
            @PathVariable UUID doctorProfileId,
            @RequestParam("value") boolean verified
    ) {
        return doctorService.setVerified(doctorProfileId, verified);
    }

    // ✅ GET /api/doctors/search?... (query params optionnels)
    @GetMapping("/search")
    public List<DoctorProfileResponseDto> search(
            @RequestParam(required = false) DoctorProfile.DoctorType doctorType,
            @RequestParam(required = false) Boolean verifiedOnly,
            @RequestParam(required = false) Boolean hasLocation,
            @RequestParam(required = false) DoctorProfile.ConsultationMode consultationMode,
            @RequestParam(required = false) Double minFee,
            @RequestParam(required = false) Double maxFee
    ) {
        DoctorService.DoctorSearchFilter filter = new DoctorService.DoctorSearchFilter();
        filter.setDoctorType(doctorType);
        filter.setVerifiedOnly(verifiedOnly);
        filter.setHasLocation(hasLocation);
        filter.setConsultationMode(consultationMode);
        filter.setMinFee(minFee);
        filter.setMaxFee(maxFee);
        return doctorService.search(filter);
    }

    @GetMapping("/{doctorProfileId}/calendar")
    public DoctorCalendarDto getCalendar(
            @PathVariable UUID doctorProfileId,
            @RequestParam LocalDate date
    ) {
        return calendarService.getDoctorCalendar(doctorProfileId, date);
    }
}
