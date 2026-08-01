package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.ClinicalData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ClinicalDataRepository extends JpaRepository<ClinicalData, UUID> {
    Optional<ClinicalData> findByMedicalRecord_Id(UUID medicalRecordId);
}
