package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.DoctorProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID>, JpaSpecificationExecutor<DoctorProfile> {

    Optional<DoctorProfile> findByUser_Id(UUID userId);

    Optional<DoctorProfile> findByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumber(String licenseNumber);

    // Listing médecins (ex: écran "Choisir un médecin")
    List<DoctorProfile> findByVerifiedTrue();

    @EntityGraph(attributePaths = {"user"})
    List<DoctorProfile> findTop6ByVerifiedTrueOrderByVerifiedAtDesc();

    @EntityGraph(attributePaths = {"user"})
    List<DoctorProfile> findTop6ByDoctorTypeAndVerifiedTrueOrderByVerifiedAtDesc(DoctorProfile.DoctorType doctorType);

    // Maps
    List<DoctorProfile> findByLatitudeIsNotNullAndLongitudeIsNotNull();

    // ✅ NEW : Filtrage par type
    List<DoctorProfile> findByDoctorType(DoctorProfile.DoctorType doctorType);

    List<DoctorProfile> findByDoctorTypeAndVerifiedTrue(DoctorProfile.DoctorType doctorType);

    // ✅ NEW : Filtrage type + maps
    List<DoctorProfile> findByDoctorTypeAndLatitudeIsNotNullAndLongitudeIsNotNull(DoctorProfile.DoctorType doctorType);

    // ✅ Chatbot fallback: all doctors (verified or not), eager-load user
    @EntityGraph(attributePaths = {"user"})
    List<DoctorProfile> findTop6ByDoctorTypeOrderByIdDesc(DoctorProfile.DoctorType doctorType);

    @EntityGraph(attributePaths = {"user"})
    List<DoctorProfile> findTop6ByOrderByIdDesc();
}
