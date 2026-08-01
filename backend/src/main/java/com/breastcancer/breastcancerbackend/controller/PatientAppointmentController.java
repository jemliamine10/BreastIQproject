package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.Appointment;
import com.breastcancer.breastcancerbackend.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patient/appointments")
public class PatientAppointmentController {

    private final AppointmentService appointmentService;

    public PatientAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public Page<PatientAppointmentDto> list(
            @RequestParam UUID patientId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Appointment.AppointmentType type,
            @RequestParam(required = false) Appointment.Status status,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return appointmentService.getPatientAppointments(patientId, date, type, status, doctorId, page, size);
    }

    @GetMapping("/{id}")
    public PatientAppointmentDto details(
            @PathVariable UUID id,
            @RequestParam UUID patientId
    ) {
        return appointmentService.getAppointmentDetails(patientId, id);
    }

    @PostMapping
    public PatientAppointmentDto create(@Valid @RequestBody CreatePatientAppointmentDto dto) {
        return appointmentService.createPatientAppointment(dto);
    }

    @PutMapping("/{id}")
    public PatientAppointmentDto update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePatientAppointmentDto dto
    ) {
        return appointmentService.updatePatientAppointment(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID id,
            @RequestParam UUID patientId
    ) {
        appointmentService.cancelPatientAppointment(patientId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/next")
    public ResponseEntity<?> getNextAppointment(@RequestParam UUID patientId) {
        PatientAppointmentDto next = appointmentService.getNextAppointmentDto(patientId).orElse(null);
        return ResponseEntity.ok(new NextAppointmentResponse(next));
    }

    @GetMapping("/stats")
    public AppointmentStatsDto stats(@RequestParam UUID patientId) {
        return appointmentService.getAppointmentStats(patientId);
    }

    @GetMapping("/timeline")
    public List<TimelineEventDto> timeline(@RequestParam UUID patientId) {
        return appointmentService.generateTimeline(patientId);
    }
}
