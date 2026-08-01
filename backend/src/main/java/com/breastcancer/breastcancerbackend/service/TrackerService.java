package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.RiskAssessmentDto;
import com.breastcancer.breastcancerbackend.dto.TrackerEntryCreateDto;
import com.breastcancer.breastcancerbackend.dto.TrackerEntryResponseDto;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.repository.PatientProfileRepository;
import com.breastcancer.breastcancerbackend.repository.TrackerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

/**
 * TrackerService — the critical orchestration service.
 *
 * When a tracker entry is submitted, the following chain executes:
 * 1. Save the entry
 * 2. Trigger AlertEngine (clinical rules)
 * 3. Trigger RiskEngine (scoring)
 * 4. Update HealthScore + Patient Status
 * 5. Create Timeline event
 * 6. Send WebSocket notification for status changes
 */
@Service
public class TrackerService {

    private static final Logger LOG = LoggerFactory.getLogger(TrackerService.class);

    private final TrackerEntryRepository trackerEntryRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AlertEngine alertEngine;
    private final RiskEngine riskEngine;
    private final HealthScoreService healthScoreService;
    private final TimelineService timelineService;
    private final NotificationService notificationService;
    private final DoctorPatientLinkGuardService doctorPatientLinkGuardService;

    public TrackerService(TrackerEntryRepository trackerEntryRepository,
                          PatientProfileRepository patientProfileRepository,
                          AlertEngine alertEngine,
                          RiskEngine riskEngine,
                          HealthScoreService healthScoreService,
                          TimelineService timelineService,
                          NotificationService notificationService,
                          DoctorPatientLinkGuardService doctorPatientLinkGuardService) {
        this.trackerEntryRepository = trackerEntryRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.alertEngine = alertEngine;
        this.riskEngine = riskEngine;
        this.healthScoreService = healthScoreService;
        this.timelineService = timelineService;
        this.notificationService = notificationService;
        this.doctorPatientLinkGuardService = doctorPatientLinkGuardService;
    }

    /**
     * Submit a daily tracker entry — this is the MAIN entry point.
     * It orchestrates the entire monitoring pipeline.
     */
    @Transactional
    public TrackerEntryResponseDto submitEntry(TrackerEntryCreateDto dto, UUID doctorId) {
        if (dto == null) throw new BadRequestException("Payload requis.");
        if (dto.getPatientId() == null) throw new BadRequestException("patientId requis.");

        if (doctorId != null) {
            doctorPatientLinkGuardService.assertActiveLink(dto.getPatientId(), doctorId);
        } else {
            doctorPatientLinkGuardService.assertPatientHasAtLeastOneActiveLink(dto.getPatientId());
        }

        PatientProfile patient = patientProfileRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));

        // ===== STEP 1: Save the tracker entry =====
        TrackerEntry entry = new TrackerEntry();
        entry.setPatient(patient);
        entry.setPainLevel(dto.getPainLevel());
        entry.setFatigueLevel(dto.getFatigueLevel());
        entry.setMoodLevel(dto.getMoodLevel());
        entry.setTemperature(dto.getTemperature());
        entry.setWeight(dto.getWeight());
        entry.setVomiting(dto.isVomiting());
        entry.setDiarrhea(dto.isDiarrhea());
        entry.setAppetiteLoss(dto.isAppetiteLoss());
        entry.setNotes(dto.getNotes());
        entry.setRecordedAt(Instant.now());

        entry = trackerEntryRepository.save(entry);
        LOG.info("Tracker entry saved for patient {}", patient.getId());

        // ===== STEP 2: Trigger AlertEngine =====
        List<Alert> generatedAlerts = alertEngine.evaluate(patient, entry);
        LOG.info("AlertEngine evaluated: {} alerts generated", generatedAlerts.size());

        // ===== STEP 3: Trigger RiskEngine =====
        RiskAssessmentDto riskAssessment = riskEngine.assess(patient.getId(), entry);
        LOG.info("RiskEngine: score={}, level={}", riskAssessment.getRiskScore(), riskAssessment.getRiskLevel());

        // ===== STEP 4: Update Health Score + Patient Status =====
        PatientProfile.PatientStatus previousStatus = patient.getPatientStatus();
        int newHealthScore = healthScoreService.computeAndUpdateHealthScore(patient.getId(), entry);
        PatientProfile.PatientStatus newStatus = healthScoreService.deriveStatus(newHealthScore);

        // ===== STEP 5: Create Timeline event =====
        String timelineTitle = "Suivi quotidien enregistré";
        String timelineDesc = String.format(
                "Douleur: %d/10, Fatigue: %d/10, Moral: %d/10, Temp: %.1f°C — Score santé: %d/100",
                dto.getPainLevel() != null ? dto.getPainLevel() : 0,
                dto.getFatigueLevel() != null ? dto.getFatigueLevel() : 0,
                dto.getMoodLevel() != null ? dto.getMoodLevel() : 5,
                dto.getTemperature() != null ? dto.getTemperature() : 37.0,
                newHealthScore
        );

        timelineService.recordEvent(
                patient.getId(),
                MedicalEvent.EventType.TRACKER_ENTRY,
                timelineTitle,
                timelineDesc,
                riskAssessment.getRiskLevel().name(),
                entry.getId(),
                "TRACKER_ENTRY"
        );

        // ===== STEP 6: Notify status change via WebSocket =====
        if (previousStatus != newStatus) {
            notificationService.pushStatusUpdate(patient.getId(), newStatus.name(), newHealthScore);

            timelineService.recordEvent(
                    patient.getId(),
                    MedicalEvent.EventType.STATUS_CHANGE,
                    "Changement de statut : " + previousStatus + " → " + newStatus,
                    "Score santé: " + newHealthScore + "/100",
                    newStatus == PatientProfile.PatientStatus.CRITICAL ? "CRITICAL" : "WARNING",
                    entry.getId(),
                    "STATUS_CHANGE"
            );
        }

        // ===== Build response =====
        TrackerEntryResponseDto response = toDto(entry);
        response.setHealthScore(newHealthScore);
        response.setRiskLevel(riskAssessment.getRiskLevel().name());
        return response;
    }

    @Transactional(readOnly = true)
    public List<TrackerEntryResponseDto> getHistory(UUID patientId, UUID doctorId) {
        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);
        return trackerEntryRepository.findByPatient_IdOrderByRecordedAtDesc(patientId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrackerEntryResponseDto getLatest(UUID patientId, UUID doctorId) {
        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);
        TrackerEntry entry = trackerEntryRepository.findTopByPatient_IdOrderByRecordedAtDesc(patientId)
                .orElseThrow(() -> new NotFoundException("Aucun suivi trouvé pour ce patient."));
        return toDto(entry);
    }

    @Transactional(readOnly = true)
    public List<TrackerEntryResponseDto> getHistoryForPatient(UUID patientId) {
        return trackerEntryRepository.findByPatient_IdOrderByRecordedAtDesc(patientId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrackerEntryResponseDto getLatestForPatient(UUID patientId) {
        TrackerEntry entry = trackerEntryRepository.findTopByPatient_IdOrderByRecordedAtDesc(patientId)
                .orElseThrow(() -> new NotFoundException("Aucun suivi trouvé pour ce patient."));
        return toDto(entry);
    }

    @Transactional(readOnly = true)
    public List<TrackerEntryResponseDto> getHistoryInRange(UUID patientId, UUID doctorId, Instant from, Instant to) {
        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);
        return trackerEntryRepository
                .findByPatient_IdAndRecordedAtBetweenOrderByRecordedAtDesc(patientId, from, to)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private TrackerEntryResponseDto toDto(TrackerEntry e) {
        TrackerEntryResponseDto dto = new TrackerEntryResponseDto();
        dto.setId(e.getId());
        dto.setPatientId(e.getPatient().getId());
        dto.setPainLevel(e.getPainLevel());
        dto.setFatigueLevel(e.getFatigueLevel());
        dto.setMoodLevel(e.getMoodLevel());
        dto.setTemperature(e.getTemperature());
        dto.setWeight(e.getWeight());
        dto.setVomiting(e.isVomiting());
        dto.setDiarrhea(e.isDiarrhea());
        dto.setAppetiteLoss(e.isAppetiteLoss());
        dto.setNotes(e.getNotes());
        dto.setRecordedAt(e.getRecordedAt());
        return dto;
    }
}
