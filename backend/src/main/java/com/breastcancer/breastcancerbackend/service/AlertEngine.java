package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.AlertResponseDto;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.repository.AlertRepository;
import com.breastcancer.breastcancerbackend.repository.TrackerEntryRepository;
import com.breastcancer.breastcancerbackend.repository.TreatmentRepository;
import com.breastcancer.breastcancerbackend.repository.TreatmentSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Clinical Alert Engine — implements real medical logic for breast cancer monitoring.
 *
 * Rules:
 * 1. INFECTION RISK: temp ≥ 38.3 AND active chemo AND last session < 7 days → CRITICAL
 * 2. SEVERE PAIN:    pain ≥ 8 → CRITICAL
 * 3. RAPID WEIGHT LOSS: > 3kg loss in 10 days → CRITICAL
 * 4. COMBINED SYMPTOMS: fatigue ≥ 7 AND mood ≤ 3 AND appetite loss → WARNING
 * 5. HIGH TEMP:      temp ≥ 38.0 (not on chemo) → HIGH
 */
@Service
public class AlertEngine {

    private static final Logger LOG = LoggerFactory.getLogger(AlertEngine.class);

    private final AlertRepository alertRepository;
    private final TreatmentRepository treatmentRepository;
    private final TreatmentSessionRepository sessionRepository;
    private final TrackerEntryRepository trackerEntryRepository;
    private final TimelineService timelineService;
    private final NotificationService notificationService;
    private final DoctorPatientLinkGuardService doctorPatientLinkGuardService;

    public AlertEngine(AlertRepository alertRepository,
                       TreatmentRepository treatmentRepository,
                       TreatmentSessionRepository sessionRepository,
                       TrackerEntryRepository trackerEntryRepository,
                       TimelineService timelineService,
                       NotificationService notificationService,
                       DoctorPatientLinkGuardService doctorPatientLinkGuardService) {
        this.alertRepository = alertRepository;
        this.treatmentRepository = treatmentRepository;
        this.sessionRepository = sessionRepository;
        this.trackerEntryRepository = trackerEntryRepository;
        this.timelineService = timelineService;
        this.notificationService = notificationService;
        this.doctorPatientLinkGuardService = doctorPatientLinkGuardService;
    }

    /**
     * Evaluate all clinical rules against a new tracker entry.
     * Returns any alerts generated.
     */
    @Transactional
    public List<Alert> evaluate(PatientProfile patient, TrackerEntry entry) {
        List<Alert> alerts = new ArrayList<>();

        // Rule 1: Infection risk (febrile neutropenia pattern)
        checkInfectionRisk(patient, entry, alerts);

        // Rule 2: Severe pain
        checkSeverePain(patient, entry, alerts);

        // Rule 3: Rapid weight loss
        checkRapidWeightLoss(patient, entry, alerts);

        // Rule 4: Combined symptoms
        checkCombinedSymptoms(patient, entry, alerts);

        // Rule 5: High temperature (non-chemo)
        checkHighTemperature(patient, entry, alerts);

        // Persist alerts + create timeline events + send notifications
        for (Alert alert : alerts) {
            alertRepository.save(alert);

            timelineService.recordEvent(
                    patient.getId(),
                    MedicalEvent.EventType.ALERT_GENERATED,
                    "⚠ " + alert.getAlertType().name() + " — " + alert.getSeverity().name(),
                    alert.getMessage(),
                    alert.getSeverity().name(),
                    alert.getId(),
                    "ALERT"
            );

            // Push to doctor via WebSocket
            if (patient.getAssignedDoctor() != null) {
                notificationService.pushAlertToDoctor(
                        patient.getAssignedDoctor().getId(),
                        patient,
                        alert
                );
            }

            LOG.warn("ALERT GENERATED [{}] for patient {} : {}",
                    alert.getSeverity(), patient.getId(), alert.getMessage());
        }

        return alerts;
    }

    // ===== Rule implementations =====

    private void checkInfectionRisk(PatientProfile patient, TrackerEntry entry, List<Alert> alerts) {
        if (entry.getTemperature() == null || entry.getTemperature() < 38.3) return;

        // Check if patient has active chemotherapy
        List<Treatment> activeChemoTreatments = treatmentRepository.findByPatient_IdAndStatus(
                patient.getId(), Treatment.Status.ACTIVE);
        boolean hasActiveChemo = activeChemoTreatments.stream()
                .anyMatch(t -> t.getTreatmentType() == Treatment.TreatmentType.CHEMO);

        if (!hasActiveChemo) return;

        // Check if last chemo session was within 7 days
        boolean recentSession = false;
        for (Treatment t : activeChemoTreatments) {
            if (t.getTreatmentType() != Treatment.TreatmentType.CHEMO) continue;
            var lastDoneSession = sessionRepository
                    .findTopByTreatment_IdAndStatusOrderByScheduledDateDesc(
                            t.getId(), TreatmentSession.SessionStatus.DONE);
            if (lastDoneSession.isPresent()) {
                LocalDate sessionDate = lastDoneSession.get().getActualDate() != null
                        ? lastDoneSession.get().getActualDate()
                        : lastDoneSession.get().getScheduledDate();
                if (sessionDate != null && ChronoUnit.DAYS.between(sessionDate, LocalDate.now()) <= 7) {
                    recentSession = true;
                    break;
                }
            }
        }

        if (!recentSession) return;

        Alert alert = createAlert(patient, Alert.Severity.CRITICAL, Alert.AlertType.INFECTION_RISK,
                "🚨 RISQUE D'INFECTION : Température " + entry.getTemperature()
                        + "°C sous chimiothérapie active (dernière séance < 7 jours). "
                        + "Suspicion de neutropénie fébrile — intervention urgente recommandée.",
                "temp=" + entry.getTemperature() + ",chemo=active,recentSession=true");
        alerts.add(alert);
    }

    private void checkSeverePain(PatientProfile patient, TrackerEntry entry, List<Alert> alerts) {
        if (entry.getPainLevel() == null || entry.getPainLevel() < 8) return;

        Alert alert = createAlert(patient, Alert.Severity.CRITICAL, Alert.AlertType.SEVERE_PAIN,
                "🚨 DOULEUR SÉVÈRE : Niveau " + entry.getPainLevel() + "/10. "
                        + "Évaluation médicale urgente nécessaire — ajustement antalgique recommandé.",
                "painLevel=" + entry.getPainLevel());
        alerts.add(alert);
    }

    private void checkRapidWeightLoss(PatientProfile patient, TrackerEntry entry, List<Alert> alerts) {
        if (entry.getWeight() == null) return;

        // Look at tracker entries from the last 10 days
        Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
        List<TrackerEntry> recentEntries = trackerEntryRepository
                .findByPatient_IdAndRecordedAtAfterOrderByRecordedAtAsc(patient.getId(), tenDaysAgo);

        if (recentEntries.isEmpty()) return;

        // Find the earliest weight in the window
        Double earliestWeight = null;
        for (TrackerEntry te : recentEntries) {
            if (te.getWeight() != null) {
                earliestWeight = te.getWeight();
                break;
            }
        }

        if (earliestWeight == null) return;

        double weightLoss = earliestWeight - entry.getWeight();
        if (weightLoss > 3.0) {
            Alert alert = createAlert(patient, Alert.Severity.CRITICAL, Alert.AlertType.RAPID_WEIGHT_LOSS,
                    "🚨 PERTE DE POIDS RAPIDE : -" + String.format("%.1f", weightLoss) + " kg en 10 jours. "
                            + "Risque de dénutrition — bilan nutritionnel recommandé.",
                    "weightLoss=" + weightLoss + ",from=" + earliestWeight + ",to=" + entry.getWeight());
            alerts.add(alert);
        }
    }

    private void checkCombinedSymptoms(PatientProfile patient, TrackerEntry entry, List<Alert> alerts) {
        int fatigue = entry.getFatigueLevel() != null ? entry.getFatigueLevel() : 0;
        int mood = entry.getMoodLevel() != null ? entry.getMoodLevel() : 10;
        boolean appetiteLoss = entry.isAppetiteLoss();

        if (fatigue >= 7 && mood <= 3 && appetiteLoss) {
            Alert alert = createAlert(patient, Alert.Severity.HIGH, Alert.AlertType.COMBINED_SYMPTOMS,
                    "⚠ SYMPTÔMES COMBINÉS : Fatigue élevée (" + fatigue + "/10), "
                            + "moral bas (" + mood + "/10), perte d'appétit. "
                            + "Risque de détérioration — évaluation psycho-oncologique recommandée.",
                    "fatigue=" + fatigue + ",mood=" + mood + ",appetiteLoss=true");
            alerts.add(alert);
        }
    }

    private void checkHighTemperature(PatientProfile patient, TrackerEntry entry, List<Alert> alerts) {
        if (entry.getTemperature() == null || entry.getTemperature() < 38.0) return;

        // Don't duplicate with infection risk (which already fires at 38.3 with chemo)
        boolean alreadyHasInfectionAlert = alerts.stream()
                .anyMatch(a -> a.getAlertType() == Alert.AlertType.INFECTION_RISK);
        if (alreadyHasInfectionAlert) return;

        Alert alert = createAlert(patient, Alert.Severity.HIGH, Alert.AlertType.HIGH_TEMPERATURE,
                "⚠ TEMPÉRATURE ÉLEVÉE : " + entry.getTemperature() + "°C. Surveillance recommandée.",
                "temp=" + entry.getTemperature());
        alerts.add(alert);
    }

    // ===== Factory =====
    private Alert createAlert(PatientProfile patient, Alert.Severity severity,
                               Alert.AlertType type, String message, String triggerData) {
        Alert alert = new Alert();
        alert.setPatient(patient);
        alert.setSeverity(severity);
        alert.setAlertType(type);
        alert.setMessage(message);
        alert.setTriggerData(triggerData);
        return alert;
    }

    // ===== Query methods =====
    @Transactional(readOnly = true)
    public List<AlertResponseDto> getPatientAlerts(UUID patientId, UUID doctorId, boolean unresolvedOnly) {
        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);
        List<Alert> alerts = unresolvedOnly
                ? alertRepository.findByPatient_IdAndResolvedFalseOrderByCreatedAtDesc(patientId)
                : alertRepository.findByPatient_IdOrderByCreatedAtDesc(patientId);
        return alerts.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponseDto> getDoctorAlerts(UUID doctorId) {
        return alertRepository.findUnresolvedAlertsByDoctorId(doctorId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public AlertResponseDto resolveAlert(UUID alertId, UUID doctorUserId, String notes) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ServiceExceptions.NotFoundException("Alert introuvable."));

        doctorPatientLinkGuardService.assertActiveLink(alert.getPatient().getId(), doctorUserId);

        alert.resolve(doctorUserId, notes);
        alertRepository.save(alert);

        timelineService.recordEvent(
                alert.getPatient().getId(),
                MedicalEvent.EventType.STATUS_CHANGE,
                "✓ Alerte résolue : " + alert.getAlertType().name(),
                notes != null ? notes : "Alerte résolue par le médecin.",
                null,
                alert.getId(),
                "ALERT"
        );

        return toDto(alert);
    }

    private AlertResponseDto toDto(Alert a) {
        AlertResponseDto dto = new AlertResponseDto();
        dto.setId(a.getId());
        dto.setPatientId(a.getPatient().getId());
        if (a.getPatient().getUser() != null) {
            dto.setPatientName(a.getPatient().getUser().getFirstName() + " " + a.getPatient().getUser().getLastName());
        }
        dto.setSeverity(a.getSeverity());
        dto.setAlertType(a.getAlertType());
        dto.setMessage(a.getMessage());
        dto.setTriggerData(a.getTriggerData());
        dto.setResolved(a.isResolved());
        dto.setResolvedAt(a.getResolvedAt());
        dto.setResolvedBy(a.getResolvedBy());
        dto.setResolutionNotes(a.getResolutionNotes());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }
}
