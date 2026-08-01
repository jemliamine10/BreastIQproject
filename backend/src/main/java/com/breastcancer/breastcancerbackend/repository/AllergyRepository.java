package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AllergyRepository extends JpaRepository<Allergy, UUID> {

    List<Allergy> findByPatient_Id(UUID patientId);

    long deleteByPatient_Id(UUID patientId);
}
