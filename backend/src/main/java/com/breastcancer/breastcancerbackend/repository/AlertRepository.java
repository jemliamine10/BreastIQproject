package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByPatient_IdOrderByCreatedAtDesc(UUID patientId);
    List<Alert> findByPatient_IdAndResolvedFalseOrderByCreatedAtDesc(UUID patientId);
    List<Alert> findByPatient_IdAndSeverityAndResolvedFalse(UUID patientId, Alert.Severity severity);

    @Query("SELECT a FROM Alert a WHERE a.patient.assignedDoctor.id = :doctorId AND a.resolved = false ORDER BY a.createdAt DESC")
    List<Alert> findUnresolvedAlertsByDoctorId(@Param("doctorId") UUID doctorId);

    long countByPatient_IdAndResolvedFalse(UUID patientId);
}
