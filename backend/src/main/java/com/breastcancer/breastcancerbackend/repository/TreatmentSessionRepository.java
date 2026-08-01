package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.TreatmentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TreatmentSessionRepository extends JpaRepository<TreatmentSession, UUID> {
    List<TreatmentSession> findByTreatment_IdOrderBySessionNumberAsc(UUID treatmentId);
    List<TreatmentSession> findByTreatment_IdAndStatus(UUID treatmentId, TreatmentSession.SessionStatus status);
    Optional<TreatmentSession> findTopByTreatment_IdAndStatusOrderByScheduledDateDesc(UUID treatmentId, TreatmentSession.SessionStatus status);
    List<TreatmentSession> findByTreatment_Patient_IdAndScheduledDateBetween(UUID patientId, LocalDate from, LocalDate to);
}
