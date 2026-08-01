package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.repository.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;

    public UserService(UserRepository userRepository,
                       DoctorProfileRepository doctorProfileRepository,
                       PatientProfileRepository patientProfileRepository) {
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
    }

    // ======================== GET ALL USERS ========================

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream().map(this::toUserResponse).collect(Collectors.toList());
    }

    // ======================== GET BY ID ========================

    public UserResponseDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + id));
        return toUserResponse(user);
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable pour cet email."));
        return toUserResponse(user);
    }

    // ======================== GET BY ROLE ========================

    public List<UserResponseDto> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role)
                .stream().map(this::toUserResponse).collect(Collectors.toList());
    }

    // ======================== FILTERED USERS ========================

    public List<UserResponseDto> filterUsers(UserFilterDto filter) {
        Specification<User> spec = buildUserSpec(filter);
        return userRepository.findAll(spec)
                .stream().map(this::toUserResponse).collect(Collectors.toList());
    }

    // ======================== FILTERED DOCTORS ========================

    public List<DoctorFullResponseDto> getAllDoctors() {
        return doctorProfileRepository.findAll()
                .stream().map(this::toDoctorFullResponse).collect(Collectors.toList());
    }

    public List<DoctorFullResponseDto> filterDoctors(DoctorFilterDto filter) {
        Specification<DoctorProfile> spec = buildDoctorSpec(filter);
        return doctorProfileRepository.findAll(spec)
                .stream().map(this::toDoctorFullResponse).collect(Collectors.toList());
    }

    public DoctorFullResponseDto getDoctorByUserId(UUID userId) {
        DoctorProfile dp = doctorProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Profil médecin introuvable pour userId : " + userId));
        return toDoctorFullResponse(dp);
    }

    // ======================== FILTERED PATIENTS ========================

    public List<PatientFullResponseDto> getAllPatients() {
        return patientProfileRepository.findAll()
                .stream().map(this::toPatientFullResponse).collect(Collectors.toList());
    }

    public List<PatientFullResponseDto> filterPatients(PatientFilterDto filter) {
        Specification<PatientProfile> spec = buildPatientSpec(filter);
        return patientProfileRepository.findAll(spec)
                .stream().map(this::toPatientFullResponse).collect(Collectors.toList());
    }

    public PatientFullResponseDto getPatientByUserId(UUID userId) {
        PatientProfile pp = patientProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Profil patient introuvable pour userId : " + userId));
        return toPatientFullResponse(pp);
    }

    // ============================================================
    //                    SPECIFICATIONS
    // ============================================================

    private Specification<User> buildUserSpec(UserFilterDto f) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
                String kw = "%" + f.getKeyword().trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("email")), kw),
                        cb.like(cb.lower(root.get("firstName")), kw),
                        cb.like(cb.lower(root.get("lastName")), kw)
                ));
            }
            if (f.getRole() != null)           preds.add(cb.equal(root.get("role"), f.getRole()));
            if (f.getGender() != null)         preds.add(cb.equal(root.get("gender"), f.getGender()));
            if (f.getCity() != null)           preds.add(cb.like(cb.lower(root.get("city")), "%" + f.getCity().trim().toLowerCase() + "%"));
            if (f.getCountry() != null)        preds.add(cb.like(cb.lower(root.get("country")), "%" + f.getCountry().trim().toLowerCase() + "%"));
            if (f.getActive() != null)         preds.add(cb.equal(root.get("active"), f.getActive()));
            if (f.getEmailVerified() != null)  preds.add(cb.equal(root.get("emailVerified"), f.getEmailVerified()));

            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    private Specification<DoctorProfile> buildDoctorSpec(DoctorFilterDto f) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            Join<DoctorProfile, User> user = root.join("user", JoinType.INNER);

            // filtres User
            if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
                String kw = "%" + f.getKeyword().trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(user.get("email")), kw),
                        cb.like(cb.lower(user.get("firstName")), kw),
                        cb.like(cb.lower(user.get("lastName")), kw)
                ));
            }
            if (f.getCity() != null)    preds.add(cb.like(cb.lower(user.get("city")), "%" + f.getCity().trim().toLowerCase() + "%"));
            if (f.getCountry() != null) preds.add(cb.like(cb.lower(user.get("country")), "%" + f.getCountry().trim().toLowerCase() + "%"));
            if (f.getActive() != null)  preds.add(cb.equal(user.get("active"), f.getActive()));

            // filtres DoctorProfile
            if (f.getDoctorType() != null)       preds.add(cb.equal(root.get("doctorType"), f.getDoctorType()));
            if (f.getSpeciality() != null)       preds.add(cb.like(cb.lower(root.get("speciality")), "%" + f.getSpeciality().trim().toLowerCase() + "%"));
            if (f.getConsultationMode() != null) preds.add(cb.equal(root.get("consultationMode"), f.getConsultationMode()));
            if (f.getVerified() != null)         preds.add(cb.equal(root.get("verified"), f.getVerified()));
            if (f.getClinicName() != null)       preds.add(cb.like(cb.lower(root.get("clinicName")), "%" + f.getClinicName().trim().toLowerCase() + "%"));
            if (f.getMinYearsOfExperience() != null) preds.add(cb.greaterThanOrEqualTo(root.get("yearsOfExperience"), f.getMinYearsOfExperience()));
            if (f.getMaxConsultationFee() != null)   preds.add(cb.lessThanOrEqualTo(root.get("consultationFee"), f.getMaxConsultationFee()));
            if (f.getLanguage() != null)         preds.add(cb.like(cb.lower(root.get("languages")), "%" + f.getLanguage().trim().toLowerCase() + "%"));

            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    private Specification<PatientProfile> buildPatientSpec(PatientFilterDto f) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            Join<PatientProfile, User> user = root.join("user", JoinType.INNER);

            // filtres User
            if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
                String kw = "%" + f.getKeyword().trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(user.get("email")), kw),
                        cb.like(cb.lower(user.get("firstName")), kw),
                        cb.like(cb.lower(user.get("lastName")), kw)
                ));
            }
            if (f.getCity() != null)    preds.add(cb.like(cb.lower(user.get("city")), "%" + f.getCity().trim().toLowerCase() + "%"));
            if (f.getCountry() != null) preds.add(cb.like(cb.lower(user.get("country")), "%" + f.getCountry().trim().toLowerCase() + "%"));
            if (f.getActive() != null)  preds.add(cb.equal(user.get("active"), f.getActive()));

            // filtres PatientProfile
            if (f.getMedicalRecordNumber() != null) preds.add(cb.equal(root.get("medicalRecordNumber"), f.getMedicalRecordNumber().trim()));
            if (f.getMedicalConsent() != null)      preds.add(cb.equal(root.get("medicalConsent"), f.getMedicalConsent()));
            if (f.getHasAssignedDoctor() != null) {
                if (f.getHasAssignedDoctor()) {
                    preds.add(cb.isNotNull(root.get("assignedDoctor")));
                } else {
                    preds.add(cb.isNull(root.get("assignedDoctor")));
                }
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };
    }

    // ============================================================
    //                    MAPPERS
    // ============================================================

    private UserResponseDto toUserResponse(User u) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(u.getId());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setPhone(u.getPhone());
        dto.setGender(u.getGender());
        dto.setDateOfBirth(u.getDateOfBirth());
        dto.setProfilePhotoUrl(u.getProfilePhotoUrl());
        dto.setAddressText(u.getAddressText());
        dto.setCity(u.getCity());
        dto.setCountry(u.getCountry());
        dto.setActive(u.isActive());
        dto.setEmailVerified(u.isEmailVerified());
        dto.setCreatedAt(u.getCreatedAt());
        dto.setUpdatedAt(u.getUpdatedAt());
        dto.setLastLoginAt(u.getLastLoginAt());
        return dto;
    }

    private DoctorFullResponseDto toDoctorFullResponse(DoctorProfile dp) {
        DoctorFullResponseDto dto = new DoctorFullResponseDto();
        User u = dp.getUser();

        // user info
        dto.setUserId(u.getId());
        dto.setEmail(u.getEmail());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setPhone(u.getPhone());
        dto.setGender(u.getGender());
        dto.setDateOfBirth(u.getDateOfBirth());
        dto.setProfilePhotoUrl(u.getProfilePhotoUrl());
        dto.setCity(u.getCity());
        dto.setCountry(u.getCountry());
        dto.setActive(u.isActive());

        // doctor info
        dto.setDoctorProfileId(dp.getId());
        dto.setDoctorType(dp.getDoctorType());
        dto.setSpeciality(dp.getSpeciality());
        dto.setLicenseNumber(dp.getLicenseNumber());
        dto.setClinicName(dp.getClinicName());
        dto.setBio(dp.getBio());
        dto.setYearsOfExperience(dp.getYearsOfExperience());
        dto.setLanguages(dp.getLanguages());
        dto.setConsultationMode(dp.getConsultationMode());
        dto.setConsultationFee(dp.getConsultationFee());
        dto.setVerified(dp.isVerified());
        dto.setVerifiedAt(dp.getVerifiedAt());
        dto.setAddressText(dp.getAddressText());
        dto.setLatitude(dp.getLatitude());
        dto.setLongitude(dp.getLongitude());

        return dto;
    }

    private PatientFullResponseDto toPatientFullResponse(PatientProfile pp) {
        PatientFullResponseDto dto = new PatientFullResponseDto();
        User u = pp.getUser();

        // user info
        dto.setUserId(u.getId());
        dto.setEmail(u.getEmail());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setPhone(u.getPhone());
        dto.setGender(u.getGender());
        dto.setDateOfBirth(u.getDateOfBirth());
        dto.setProfilePhotoUrl(u.getProfilePhotoUrl());
        dto.setCity(u.getCity());
        dto.setCountry(u.getCountry());
        dto.setActive(u.isActive());

        // patient info
        dto.setPatientProfileId(pp.getId());
        dto.setAssignedDoctorProfileId(pp.getAssignedDoctor() != null ? pp.getAssignedDoctor().getId() : null);
        dto.setMedicalRecordNumber(pp.getMedicalRecordNumber());
        dto.setEmergencyContactName(pp.getEmergencyContactName());
        dto.setEmergencyContactPhone(pp.getEmergencyContactPhone());
        dto.setHeightCm(pp.getHeightCm());
        dto.setWeightKg(pp.getWeightKg());
        dto.setMedicalConsent(pp.isMedicalConsent());
        dto.setConsentTimestamp(pp.getConsentTimestamp());
        dto.setLastKnownLatitude(pp.getLastKnownLatitude());
        dto.setLastKnownLongitude(pp.getLastKnownLongitude());

        return dto;
    }
}
