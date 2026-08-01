package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.TrackerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackerEntryRepository extends JpaRepository<TrackerEntry, UUID> {
    List<TrackerEntry> findByPatient_IdOrderByRecordedAtDesc(UUID patientId);
    Optional<TrackerEntry> findTopByPatient_IdOrderByRecordedAtDesc(UUID patientId);
    List<TrackerEntry> findByPatient_IdAndRecordedAtBetweenOrderByRecordedAtDesc(UUID patientId, Instant from, Instant to);
    List<TrackerEntry> findByPatient_IdAndRecordedAtAfterOrderByRecordedAtAsc(UUID patientId, Instant since);
}
