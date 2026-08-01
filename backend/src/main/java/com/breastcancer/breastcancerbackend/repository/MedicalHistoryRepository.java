package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, UUID> {
    List<MedicalHistory> findByPatient_IdAndDeletedFalseOrderByEventDateDesc(UUID patientId);
    List<MedicalHistory> findByPatient_IdAndHistoryTypeAndDeletedFalse(UUID patientId, MedicalHistory.HistoryType type);
}
