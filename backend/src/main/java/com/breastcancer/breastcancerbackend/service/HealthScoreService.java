package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.entity.PatientProfile;
import com.breastcancer.breastcancerbackend.entity.TrackerEntry;
import com.breastcancer.breastcancerbackend.repository.PatientProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

/**
 * Computes a dynamic health score (0-100) from patient vitals.
 * Updates the patient's status (STABLE / WARNING / CRITICAL) accordingly.
 *
 * Score breakdown:
 * - Pain:        30 points (pain 0 = 30pts, pain 10 = 0pts)
 * - Fatigue:     25 points
 * - Temperature: 25 points (normal 36-37.5 = 25pts, fever = reduced)
 * - Mood:        20 points (mood 10 = 20pts, mood 0 = 0pts)
 */
@Service
public class HealthScoreService {

    private final PatientProfileRepository patientProfileRepository;

    public HealthScoreService(PatientProfileRepository patientProfileRepository) {
        this.patientProfileRepository = patientProfileRepository;
    }

    /**
     * Calculate health score from a tracker entry and update patient profile.
     * Returns the computed score.
     */
    @Transactional
    public int computeAndUpdateHealthScore(UUID patientId, TrackerEntry entry) {
        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));

        int score = computeScore(entry);
        patient.setHealthScore(score);
        patient.setPatientStatus(deriveStatus(score));
        patientProfileRepository.save(patient);

        return score;
    }

    public int computeScore(TrackerEntry entry) {
        int score = 0;

        // Pain component (30 pts): lower pain = higher score
        int pain = entry.getPainLevel() != null ? entry.getPainLevel() : 0;
        score += Math.max(0, 30 - (pain * 3));

        // Fatigue component (25 pts)
        int fatigue = entry.getFatigueLevel() != null ? entry.getFatigueLevel() : 0;
        score += Math.max(0, 25 - (int)(fatigue * 2.5));

        // Temperature component (25 pts)
        double temp = entry.getTemperature() != null ? entry.getTemperature() : 37.0;
        if (temp >= 36.0 && temp <= 37.5) {
            score += 25; // normal
        } else if (temp > 37.5 && temp < 38.0) {
            score += 15; // low-grade fever
        } else if (temp >= 38.0 && temp < 38.5) {
            score += 8;  // moderate fever
        } else if (temp >= 38.5) {
            score += 0;  // high fever
        } else {
            score += 20; // hypothermia (slight concern)
        }

        // Mood component (20 pts): higher mood = higher score
        int mood = entry.getMoodLevel() != null ? entry.getMoodLevel() : 5;
        score += mood * 2;

        // Penalty for GI symptoms
        if (entry.isVomiting()) score -= 5;
        if (entry.isDiarrhea()) score -= 3;
        if (entry.isAppetiteLoss()) score -= 3;

        return Math.max(0, Math.min(100, score));
    }

    public PatientProfile.PatientStatus deriveStatus(int healthScore) {
        if (healthScore >= 60) return PatientProfile.PatientStatus.STABLE;
        if (healthScore >= 35) return PatientProfile.PatientStatus.WARNING;
        return PatientProfile.PatientStatus.CRITICAL;
    }
}
