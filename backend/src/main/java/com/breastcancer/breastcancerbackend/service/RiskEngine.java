package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.RiskAssessmentDto;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.repository.TrackerEntryRepository;
import com.breastcancer.breastcancerbackend.repository.TreatmentRepository;
import com.breastcancer.breastcancerbackend.repository.TreatmentSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Risk Engine — computes a risk score (0-100) and categorizes into LOW / MEDIUM / HIGH.
 *
 * Scoring factors:
 * - Recent chemo session (< 14 days):       +25 points
 * - Elevated temperature (> 37.5):           +20 points
 * - High fatigue (≥ 7):                      +15 points
 * - High pain (≥ 6):                         +15 points
 * - Low mood (≤ 3):                          +10 points
 * - GI symptoms (vomiting/diarrhea/appetite):+5 each (max 15)
 *
 * Risk levels:
 * - 0-30:  LOW
 * - 31-60: MEDIUM
 * - 61+:   HIGH
 */
@Service
public class RiskEngine {

    private final TreatmentRepository treatmentRepository;
    private final TreatmentSessionRepository sessionRepository;
    private final TrackerEntryRepository trackerEntryRepository;

    public RiskEngine(TreatmentRepository treatmentRepository,
                      TreatmentSessionRepository sessionRepository,
                      TrackerEntryRepository trackerEntryRepository) {
        this.treatmentRepository = treatmentRepository;
        this.sessionRepository = sessionRepository;
        this.trackerEntryRepository = trackerEntryRepository;
    }

    @Transactional(readOnly = true)
    public RiskAssessmentDto assess(UUID patientId, TrackerEntry latestEntry) {
        int score = 0;
        StringBuilder summary = new StringBuilder();

        // Factor 1: Recent chemo session
        boolean recentChemo = hasRecentChemoSession(patientId, 14);
        if (recentChemo) {
            score += 25;
            summary.append("Chimiothérapie récente (+25). ");
        }

        // Factor 2: Temperature
        if (latestEntry.getTemperature() != null) {
            if (latestEntry.getTemperature() >= 38.3) {
                score += 20;
                summary.append("Fièvre élevée ").append(latestEntry.getTemperature()).append("°C (+20). ");
            } else if (latestEntry.getTemperature() > 37.5) {
                score += 10;
                summary.append("Température sub-fébrile (+10). ");
            }
        }

        // Factor 3: Fatigue
        int fatigue = latestEntry.getFatigueLevel() != null ? latestEntry.getFatigueLevel() : 0;
        if (fatigue >= 7) {
            score += 15;
            summary.append("Fatigue élevée ").append(fatigue).append("/10 (+15). ");
        } else if (fatigue >= 5) {
            score += 8;
            summary.append("Fatigue modérée (+8). ");
        }

        // Factor 4: Pain
        int pain = latestEntry.getPainLevel() != null ? latestEntry.getPainLevel() : 0;
        if (pain >= 6) {
            score += 15;
            summary.append("Douleur élevée ").append(pain).append("/10 (+15). ");
        } else if (pain >= 4) {
            score += 8;
            summary.append("Douleur modérée (+8). ");
        }

        // Factor 5: Mood
        int mood = latestEntry.getMoodLevel() != null ? latestEntry.getMoodLevel() : 10;
        if (mood <= 3) {
            score += 10;
            summary.append("Moral bas ").append(mood).append("/10 (+10). ");
        }

        // Factor 6: GI symptoms
        if (latestEntry.isVomiting()) { score += 5; summary.append("Vomissements (+5). "); }
        if (latestEntry.isDiarrhea()) { score += 5; summary.append("Diarrhée (+5). "); }
        if (latestEntry.isAppetiteLoss()) { score += 5; summary.append("Perte d'appétit (+5). "); }

        score = Math.min(100, score);

        RiskAssessmentDto dto = new RiskAssessmentDto();
        dto.setPatientId(patientId);
        dto.setRiskScore(score);
        dto.setRiskLevel(categorize(score));
        dto.setSummary(summary.length() > 0 ? summary.toString().trim() : "Aucun facteur de risque identifié.");
        return dto;
    }

    private boolean hasRecentChemoSession(UUID patientId, int withinDays) {
        List<Treatment> chemoTreatments = treatmentRepository.findByPatient_IdAndStatus(
                patientId, Treatment.Status.ACTIVE);

        for (Treatment t : chemoTreatments) {
            if (t.getTreatmentType() != Treatment.TreatmentType.CHEMO) continue;
            var lastDone = sessionRepository
                    .findTopByTreatment_IdAndStatusOrderByScheduledDateDesc(
                            t.getId(), TreatmentSession.SessionStatus.DONE);
            if (lastDone.isPresent()) {
                LocalDate sessionDate = lastDone.get().getActualDate() != null
                        ? lastDone.get().getActualDate()
                        : lastDone.get().getScheduledDate();
                if (sessionDate != null && ChronoUnit.DAYS.between(sessionDate, LocalDate.now()) <= withinDays) {
                    return true;
                }
            }
        }
        return false;
    }

    private RiskAssessmentDto.RiskLevel categorize(int score) {
        if (score <= 30) return RiskAssessmentDto.RiskLevel.LOW;
        if (score <= 60) return RiskAssessmentDto.RiskLevel.MEDIUM;
        return RiskAssessmentDto.RiskLevel.HIGH;
    }
}
