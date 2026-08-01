package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.Appointment;
import com.breastcancer.breastcancerbackend.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // ✅ POST /api/appointments
    @PostMapping
    public AppointmentResponseDto create(@Valid @RequestBody AppointmentCreateRequestDto dto) {
        return appointmentService.create(dto);
    }

    // ✅ PUT /api/appointments/{appointmentId}
    @PutMapping("/{appointmentId}")
    public AppointmentResponseDto update(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody AppointmentUpdateRequestDto dto
    ) {
        return appointmentService.update(appointmentId, dto);
    }

    // ✅ PUT /api/appointments/{appointmentId}/status
    @PutMapping("/{appointmentId}/status")
    public AppointmentResponseDto updateStatus(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody AppointmentStatusUpdateDto dto
    ) {
        return appointmentService.updateStatus(appointmentId, dto);
    }

    @PutMapping("/{appointmentId}/cancel")
    public AppointmentResponseDto cancel(@PathVariable UUID appointmentId) {
        return appointmentService.cancelAppointment(appointmentId);
    }

    @PutMapping("/{appointmentId}/reschedule")
    public AppointmentResponseDto reschedule(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody AppointmentRescheduleRequestDto dto
    ) {
        return appointmentService.rescheduleAppointment(appointmentId, dto);
    }

    // ✅ GET /api/appointments/by-link/{linkId}
    @GetMapping("/by-link/{linkId}")
    public List<AppointmentResponseDto> listByLink(@PathVariable UUID linkId) {
        return appointmentService.listByLink(linkId);
    }

    // ✅ GET /api/appointments/doctor-calendar?doctorId=...&from=...&to=...
    @GetMapping("/doctor-calendar")
    public List<AppointmentResponseDto> listDoctorCalendar(
            @RequestParam UUID doctorId,
            @RequestParam Instant from,
            @RequestParam Instant to
    ) {
        return appointmentService.listDoctorCalendar(doctorId, from, to);
    }

    // ✅ GET /api/appointments/patient-calendar?patientId=...&from=...&to=...
    @GetMapping("/patient-calendar")
    public List<AppointmentResponseDto> listPatientCalendar(
            @RequestParam UUID patientId,
            @RequestParam Instant from,
            @RequestParam Instant to
    ) {
        return appointmentService.listPatientCalendar(patientId, from, to);
    }

    // ✅ GET /api/appointments/available?doctorId=...&date=...&heure=...&durationMinutes=...
    @GetMapping("/available")
    public AppointmentAvailabilityResponseDto available(
            @RequestParam UUID doctorId,
            @RequestParam LocalDate date,
            @RequestParam LocalTime heure,
            @RequestParam(required = false) Integer durationMinutes,
            @RequestParam(required = false, name = "typeRDV") Appointment.AppointmentType typeRDV
    ) {
        return appointmentService.checkAvailability(doctorId, date, heure, durationMinutes);
    }

    // ✅ POST /api/appointments/create
    @PostMapping("/create")
    public PatientAppointmentDto createFront(@Valid @RequestBody AppointmentCreateFrontRequestDto dto) {
        return appointmentService.createFromFront(dto);
    }

    // ✅ GET /api/appointments/patient
    @GetMapping("/patient")
    public Page<PatientAppointmentDto> listPatientAlias(
            @RequestParam UUID patientId,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false, name = "typeRDV") Appointment.AppointmentType typeRDV,
            @RequestParam(required = false) Appointment.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return appointmentService.listPatientAppointmentsAlias(patientId, doctorId, date, typeRDV, status, page, size);
    }

    // ✅ GET /api/appointments/doctor
    @GetMapping("/doctor")
    public List<AppointmentResponseDto> listDoctorAlias(
            @RequestParam UUID doctorId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        return appointmentService.listDoctorAppointmentsAlias(doctorId, from, to);
    }
}
