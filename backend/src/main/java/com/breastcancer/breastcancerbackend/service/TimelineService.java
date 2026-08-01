package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.MedicalEventResponseDto;
import com.breastcancer.breastcancerbackend.entity.MedicalEvent;
import com.breastcancer.breastcancerbackend.entity.PatientProfile;
import com.breastcancer.breastcancerbackend.repository.MedicalEventRepository;
import com.breastcancer.breastcancerbackend.repository.PatientProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

@Service
public class TimelineService {

    private final MedicalEventRepository eventRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorPatientLinkGuardService doctorPatientLinkGuardService;

    public TimelineService(MedicalEventRepository eventRepository,
                           PatientProfileRepository patientProfileRepository,
                           DoctorPatientLinkGuardService doctorPatientLinkGuardService) {
        this.eventRepository = eventRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorPatientLinkGuardService = doctorPatientLinkGuardService;
    }

    /**
     * Record a medical event on the patient's timeline.
     * Called internally by other services — NOT directly by controllers.
     */
    @Transactional
    public MedicalEvent recordEvent(UUID patientId,
                                     MedicalEvent.EventType eventType,
                                     String title,
                                     String description,
                                     String severity,
                                     UUID referenceId,
                                     String referenceType) {
        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));

        MedicalEvent event = new MedicalEvent();
        event.setPatient(patient);
        event.setEventType(eventType);
        event.setTitle(title);
        event.setDescription(description);
        event.setSeverity(severity);
        event.setReferenceId(referenceId);
        event.setReferenceType(referenceType);
        event.setEventDate(Instant.now());

        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<MedicalEventResponseDto> getTimeline(UUID patientId, UUID doctorId) {
        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);
        return eventRepository.findByPatient_IdOrderByEventDateDesc(patientId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalEventResponseDto> getTimelineForPatient(UUID patientId) {
        return eventRepository.findByPatient_IdOrderByEventDateDesc(patientId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalEventResponseDto> getTimelineByType(UUID patientId, UUID doctorId, MedicalEvent.EventType eventType) {
        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);
        return eventRepository.findByPatient_IdAndEventTypeOrderByEventDateDesc(patientId, eventType)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private MedicalEventResponseDto toDto(MedicalEvent e) {
        MedicalEventResponseDto dto = new MedicalEventResponseDto();
        dto.setId(e.getId());
        dto.setPatientId(e.getPatient().getId());
        dto.setEventType(e.getEventType());
        dto.setTitle(e.getTitle());
        dto.setDescription(e.getDescription());
        dto.setSeverity(e.getSeverity());
        dto.setReferenceId(e.getReferenceId());
        dto.setReferenceType(e.getReferenceType());
        dto.setEventDate(e.getEventDate());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}
