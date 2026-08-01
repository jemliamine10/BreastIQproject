package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.MedicalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MedicalEventRepository extends JpaRepository<MedicalEvent, UUID> {
    List<MedicalEvent> findByPatient_IdOrderByEventDateDesc(UUID patientId);
    List<MedicalEvent> findByPatient_IdAndEventTypeOrderByEventDateDesc(UUID patientId, MedicalEvent.EventType eventType);
}
