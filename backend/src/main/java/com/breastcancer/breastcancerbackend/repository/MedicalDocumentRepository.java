package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.DocumentCategory;
import com.breastcancer.breastcancerbackend.entity.MedicalDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, UUID> {

    // Documents d'un patient (non supprimés), triés par date desc
    Page<MedicalDocument> findByPatient_IdAndDeletedFalseOrderByUploadDateDesc(UUID patientId, Pageable pageable);

    // Documents visibles côté médecin (partagés), triés par date desc
    Page<MedicalDocument> findByPatient_IdAndDeletedFalseAndVisibleToDoctorTrueOrderByUploadDateDesc(UUID patientId, Pageable pageable);

    // Liste non paginée (utile pour export/count)
    List<MedicalDocument> findByPatient_IdAndDeletedFalseOrderByUploadDateDesc(UUID patientId);

    // Filtrage par catégorie
    List<MedicalDocument> findByPatient_IdAndCategoryAndDeletedFalse(UUID patientId, DocumentCategory category);

    // Un document non supprimé par ID
    Optional<MedicalDocument> findByIdAndDeletedFalse(UUID id);

    // Compter par catégorie pour un patient
    @Query("SELECT d.category, COUNT(d) FROM MedicalDocument d " +
           "WHERE d.patient.id = :patientId AND d.deleted = false " +
           "GROUP BY d.category")
    List<Object[]> countByPatientGroupedByCategory(@Param("patientId") UUID patientId);

        @Query("SELECT d.category, COUNT(d) FROM MedicalDocument d " +
            "WHERE d.patient.id = :patientId AND d.deleted = false AND d.visibleToDoctor = true " +
            "GROUP BY d.category")
        List<Object[]> countVisibleToDoctorByPatientGroupedByCategory(@Param("patientId") UUID patientId);
}
