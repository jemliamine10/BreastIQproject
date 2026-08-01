package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.PatientDoctorLink;
import com.breastcancer.breastcancerbackend.entity.PatientDoctorLink.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientDoctorLinkRepository extends JpaRepository<PatientDoctorLink, UUID> {

    // Empêcher les doublons patient-doctor
    boolean existsByPatient_IdAndDoctor_Id(UUID patientId, UUID doctorId);

    Optional<PatientDoctorLink> findByPatient_IdAndDoctor_Id(UUID patientId, UUID doctorId);

    // Liste des demandes d’un médecin
    List<PatientDoctorLink> findByDoctor_IdAndStatus(UUID doctorId, Status status);

    // Liste des médecins connectés (ACTIVE) d’une patiente
    List<PatientDoctorLink> findByPatient_IdAndStatus(UUID patientId, Status status);

    // Liste des patientes connectées (ACTIVE) d’un médecin
    List<PatientDoctorLink> findByDoctor_IdAndStatusOrderByActivatedAtDesc(UUID doctorId, Status status);

    // Accès rapide à un lien actif
    Optional<PatientDoctorLink> findByIdAndStatus(UUID id, Status status);
}
