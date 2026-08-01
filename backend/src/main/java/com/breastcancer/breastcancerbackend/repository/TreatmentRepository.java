package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.Treatment;
import com.breastcancer.breastcancerbackend.entity.Treatment.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TreatmentRepository extends JpaRepository<Treatment, UUID> {

    List<Treatment> findByPatient_Id(UUID patientId);

    List<Treatment> findByPatient_IdAndStatus(UUID patientId, Status status);

    long deleteByPatient_Id(UUID patientId);
}
