package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID>, JpaSpecificationExecutor<PatientProfile> {

    Optional<PatientProfile> findByUser_Id(UUID userId);

    Optional<PatientProfile> findByMedicalRecordNumber(String medicalRecordNumber);
}
