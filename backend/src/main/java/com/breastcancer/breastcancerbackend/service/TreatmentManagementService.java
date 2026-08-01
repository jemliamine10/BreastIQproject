package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.TreatmentResponseDto;
import com.breastcancer.breastcancerbackend.dto.TreatmentSessionResponseDto;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.repository.PatientProfileRepository;
import com.breastcancer.breastcancerbackend.repository.TreatmentRepository;
import com.breastcancer.breastcancerbackend.repository.TreatmentSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

/**
 * TreatmentManagementService — handles the full treatment lifecycle.
 *
 * Key behaviors:
 * - Creates treatment with sessions auto-generated from cycles
 * - Individual session marking (DONE/MISSED) updates treatment progress
 * - Treatment status is auto-calculated from dates + sessions
 * - Timeline events on treatment start, session done/missed
 */
@Service
public class TreatmentManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(TreatmentManagementService.class);

    private final TreatmentRepository treatmentRepository;
    private final TreatmentSessionRepository sessionRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final TimelineService timelineService;
    private final DoctorPatientLinkGuardService doctorPatientLinkGuardService;

    public TreatmentManagementService(TreatmentRepository treatmentRepository,
                                       TreatmentSessionRepository sessionRepository,
                                       PatientProfileRepository patientProfileRepository,
                                       TimelineService timelineService,
                                       DoctorPatientLinkGuardService doctorPatientLinkGuardService) {
        this.treatmentRepository = treatmentRepository;
        this.sessionRepository = sessionRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.timelineService = timelineService;
        this.doctorPatientLinkGuardService = doctorPatientLinkGuardService;
    }

    /**
     * Create a new treatment with auto-generated sessions.
     */
    @Transactional
    public Treatment createTreatment(UUID doctorId,
                                      UUID patientId,
                                      Treatment.TreatmentType type,
                                      String protocol,
                                      String medicationName,
                                      String dosage,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      Integer cyclesTotal,
                                      int intervalDays,
                                      String notes) {
                        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);

        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));

        Treatment treatment = new Treatment();
        treatment.setPatient(patient);
        treatment.setTreatmentType(type);
        treatment.setProtocol(protocol);
        treatment.setMedicationName(medicationName);
        treatment.setDosage(dosage);
        treatment.setStartDate(startDate);
        treatment.setEndDate(endDate);
        treatment.setCyclesTotal(cyclesTotal);
        treatment.setCurrentCycle(0);
        treatment.setNotes(notes);
        treatment.refreshStatus();

        treatment = treatmentRepository.save(treatment);

        // Auto-generate sessions if cycles are defined
        if (cyclesTotal != null && cyclesTotal > 0 && startDate != null) {
            int interval = intervalDays > 0 ? intervalDays : 21; // default 3-week cycle
            for (int i = 1; i <= cyclesTotal; i++) {
                TreatmentSession session = new TreatmentSession();
                session.setTreatment(treatment);
                session.setSessionNumber(i);
                session.setScheduledDate(startDate.plusDays((long) (i - 1) * interval));
                session.setStatus(TreatmentSession.SessionStatus.PLANNED);
                sessionRepository.save(session);
            }
        }

        // Timeline
        timelineService.recordEvent(
                patientId,
                MedicalEvent.EventType.TREATMENT_START,
                "Traitement démarré : " + type.name() + (protocol != null ? " — " + protocol : ""),
                "Cycles: " + (cyclesTotal != null ? cyclesTotal : "N/A")
                        + ", Début: " + startDate + ", Fin prévue: " + endDate,
                null,
                treatment.getId(),
                "TREATMENT"
        );

        LOG.info("Treatment created for patient {}: type={}, protocol={}, cycles={}",
                patientId, type, protocol, cyclesTotal);

        return treatment;
    }

    /**
     * Mark a session as DONE — auto-increments treatment's currentCycle.
     */
    @Transactional
    public TreatmentSessionResponseDto markSessionDone(UUID doctorId, UUID sessionId, String notes, String sideEffects) {
        TreatmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Séance introuvable."));

        doctorPatientLinkGuardService.assertActiveLink(session.getTreatment().getPatient().getId(), doctorId);

        if (session.getStatus() == TreatmentSession.SessionStatus.DONE) {
            throw new BadRequestException("Cette séance est déjà marquée comme effectuée.");
        }

        session.setStatus(TreatmentSession.SessionStatus.DONE);
        session.setActualDate(LocalDate.now());
        if (notes != null) session.setNotes(notes);
        if (sideEffects != null) session.setSideEffects(sideEffects);
        sessionRepository.save(session);

        // Update treatment current cycle
        Treatment treatment = session.getTreatment();
        long doneCount = sessionRepository.findByTreatment_IdAndStatus(
                treatment.getId(), TreatmentSession.SessionStatus.DONE).size();
        treatment.setCurrentCycle((int) doneCount);
        treatment.refreshStatus();
        treatmentRepository.save(treatment);

        // Timeline
        timelineService.recordEvent(
                treatment.getPatient().getId(),
                MedicalEvent.EventType.SESSION_COMPLETED,
                "Séance " + session.getSessionNumber() + "/" + treatment.getCyclesTotal() + " effectuée",
                treatment.getTreatmentType().name() + " — " + (treatment.getProtocol() != null ? treatment.getProtocol() : ""),
                null,
                session.getId(),
                "TREATMENT_SESSION"
        );

        return toSessionDto(session);
    }

    /**
     * Mark a session as MISSED — generates an alert-worthy timeline event.
     */
    @Transactional
    public TreatmentSessionResponseDto markSessionMissed(UUID doctorId, UUID sessionId, String reason) {
        TreatmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Séance introuvable."));

        doctorPatientLinkGuardService.assertActiveLink(session.getTreatment().getPatient().getId(), doctorId);

        session.setStatus(TreatmentSession.SessionStatus.MISSED);
        if (reason != null) session.setNotes(reason);
        sessionRepository.save(session);

        Treatment treatment = session.getTreatment();

        timelineService.recordEvent(
                treatment.getPatient().getId(),
                MedicalEvent.EventType.SESSION_MISSED,
                "⚠ Séance " + session.getSessionNumber() + " manquée",
                treatment.getTreatmentType().name() + " — Raison: " + (reason != null ? reason : "Non spécifiée"),
                "WARNING",
                session.getId(),
                "TREATMENT_SESSION"
        );

        return toSessionDto(session);
    }

    @Transactional(readOnly = true)
    public List<TreatmentSessionResponseDto> getSessions(UUID doctorId, UUID treatmentId) {
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new NotFoundException("Treatment introuvable."));
        doctorPatientLinkGuardService.assertActiveLink(treatment.getPatient().getId(), doctorId);

        return sessionRepository.findByTreatment_IdOrderBySessionNumberAsc(treatmentId)
                .stream()
                .map(this::toSessionDto)
                .toList();
    }

    /**
     * Refresh all treatments for a patient (auto-calculate status).
     */
    @Transactional
    public void refreshAllTreatmentStatuses(UUID doctorId, UUID patientId) {
        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);

        List<Treatment> treatments = treatmentRepository.findByPatient_Id(patientId);
        for (Treatment t : treatments) {
            if (!t.isDeleted()) {
                t.refreshStatus();
                treatmentRepository.save(t);
            }
        }
    }

    @Transactional
    public void softDeleteTreatment(UUID doctorId, UUID treatmentId) {
        Treatment t = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new NotFoundException("Treatment introuvable."));
        doctorPatientLinkGuardService.assertActiveLink(t.getPatient().getId(), doctorId);
        t.setDeleted(true);
        treatmentRepository.save(t);

        timelineService.recordEvent(
                t.getPatient().getId(),
                MedicalEvent.EventType.TREATMENT_END,
                "Traitement arrêté : " + t.getTreatmentType().name(),
                t.getProtocol() != null ? t.getProtocol() : "",
                null,
                t.getId(),
                "TREATMENT"
        );
    }

    @Transactional(readOnly = true)
    public List<TreatmentResponseDto> listTreatmentsForLinkedPair(UUID doctorId, UUID patientId, String rawStatus) {
        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);
        Treatment.Status statusFilter = parseStatusFilter(rawStatus);

        return treatmentRepository.findByPatient_Id(patientId)
                .stream()
                .filter(t -> !t.isDeleted())
                .filter(t -> statusFilter == null || t.getComputedStatus() == statusFilter)
                .map(this::toTreatmentDto)
                .toList();
    }

    private Treatment.Status parseStatusFilter(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }

        try {
            return Treatment.Status.fromValue(rawStatus);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Statut traitement invalide. Utilisez: UPCOMING, ONGOING, ACTIVE, COMPLETED, STOPPED.");
        }
    }

    private TreatmentResponseDto toTreatmentDto(Treatment t) {
        TreatmentResponseDto dto = new TreatmentResponseDto();
        dto.setId(t.getId());
        dto.setPatientProfileId(t.getPatient() != null ? t.getPatient().getId() : null);
        dto.setTreatmentType(t.getTreatmentType());
        dto.setProtocol(t.getProtocol());
        dto.setMedicationName(t.getMedicationName());
        dto.setDosage(t.getDosage());
        dto.setStartDate(t.getStartDate());
        dto.setEndDate(t.getEndDate());
        dto.setCyclesTotal(t.getCyclesTotal());
        dto.setCurrentCycle(t.getCurrentCycle());
        dto.setStatus(t.getComputedStatus());
        dto.setNotes(t.getNotes());
        return dto;
    }

    private TreatmentSessionResponseDto toSessionDto(TreatmentSession s) {
        TreatmentSessionResponseDto dto = new TreatmentSessionResponseDto();
        dto.setId(s.getId());
        dto.setTreatmentId(s.getTreatment().getId());
        dto.setSessionNumber(s.getSessionNumber());
        dto.setScheduledDate(s.getScheduledDate());
        dto.setActualDate(s.getActualDate());
        dto.setStatus(s.getStatus());
        dto.setNotes(s.getNotes());
        dto.setSideEffects(s.getSideEffects());
        return dto;
    }
}
