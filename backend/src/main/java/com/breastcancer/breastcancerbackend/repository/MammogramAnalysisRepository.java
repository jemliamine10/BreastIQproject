package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.MammogramAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MammogramAnalysisRepository extends JpaRepository<MammogramAnalysis, UUID> {

    /**
     * Fetch analysis with patient + patient.user eagerly loaded (avoids LazyInitializationException).
     */
    @Query("SELECT a FROM MammogramAnalysis a " +
           "JOIN FETCH a.patient p JOIN FETCH p.user " +
           "JOIN FETCH a.doctor d JOIN FETCH d.user " +
           "WHERE a.patient.id = :patientId " +
           "ORDER BY a.analysisDate DESC")
    List<MammogramAnalysis> findByPatientIdWithUser(@Param("patientId") UUID patientId);

    @Query("SELECT a FROM MammogramAnalysis a " +
           "JOIN FETCH a.patient p JOIN FETCH p.user " +
           "JOIN FETCH a.doctor d JOIN FETCH d.user " +
           "WHERE a.doctor.id = :doctorId " +
           "ORDER BY a.analysisDate DESC")
    List<MammogramAnalysis> findByDoctorIdWithUser(@Param("doctorId") UUID doctorId);

    @Query("SELECT a FROM MammogramAnalysis a " +
           "JOIN FETCH a.patient p JOIN FETCH p.user " +
           "JOIN FETCH a.doctor d JOIN FETCH d.user " +
           "WHERE a.patient.id = :patientId AND a.doctor.id = :doctorId " +
           "ORDER BY a.analysisDate DESC")
    List<MammogramAnalysis> findByPatientIdAndDoctorIdWithUser(
            @Param("patientId") UUID patientId,
            @Param("doctorId") UUID doctorId
    );

    @Query("SELECT a FROM MammogramAnalysis a " +
           "JOIN FETCH a.patient p JOIN FETCH p.user " +
           "JOIN FETCH a.doctor d JOIN FETCH d.user " +
           "WHERE a.id = :id")
    Optional<MammogramAnalysis> findByIdWithUser(@Param("id") UUID id);
}
