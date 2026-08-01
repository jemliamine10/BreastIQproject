package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.DoctorProfile;
import com.breastcancer.breastcancerbackend.entity.User;
import com.breastcancer.breastcancerbackend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ======================== TOUS LES UTILISATEURS ========================

    /**
     * GET /api/users
     * Retourne tous les utilisateurs (sans filtre).
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * GET /api/users/{id}
     * Retourne un utilisateur par son ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * GET /api/users/by-email?email=...
     * Retourne un utilisateur par son email.
     */
    @GetMapping("/by-email")
    public ResponseEntity<UserResponseDto> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    /**
     * GET /api/users/by-role?role=DOCTOR|PATIENT|ADMIN
     * Retourne les utilisateurs par rôle.
     */
    @GetMapping("/by-role")
    public ResponseEntity<List<UserResponseDto>> getUsersByRole(@RequestParam User.Role role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    // ======================== FILTRAGE UTILISATEURS ========================

    /**
     * GET /api/users/filter?keyword=...&role=...&gender=...&city=...&country=...&active=...&emailVerified=...
     * Filtre dynamique sur les utilisateurs. Tous les paramètres sont optionnels.
     */
    @GetMapping("/filter")
    public ResponseEntity<List<UserResponseDto>> filterUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) User.Gender gender,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean emailVerified
    ) {
        UserFilterDto filter = new UserFilterDto();
        filter.setKeyword(keyword);
        filter.setRole(role);
        filter.setGender(gender);
        filter.setCity(city);
        filter.setCountry(country);
        filter.setActive(active);
        filter.setEmailVerified(emailVerified);
        return ResponseEntity.ok(userService.filterUsers(filter));
    }

    // ======================== MÉDECINS ========================

    /**
     * GET /api/users/doctors
     * Retourne tous les médecins (User + DoctorProfile combinés).
     */
    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorFullResponseDto>> getAllDoctors() {
        return ResponseEntity.ok(userService.getAllDoctors());
    }

    /**
     * GET /api/users/doctors/by-user/{userId}
     * Retourne le profil médecin complet par userId.
     */
    @GetMapping("/doctors/by-user/{userId}")
    public ResponseEntity<DoctorFullResponseDto> getDoctorByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getDoctorByUserId(userId));
    }

    /**
     * GET /api/users/doctors/filter?keyword=...&city=...&country=...&active=...
     *     &doctorType=...&speciality=...&consultationMode=...&verified=...
     *     &clinicName=...&minYearsOfExperience=...&maxConsultationFee=...&language=...
     *
     * Filtre dynamique sur les médecins. Tous les paramètres sont optionnels.
     */
    @GetMapping("/doctors/filter")
    public ResponseEntity<List<DoctorFullResponseDto>> filterDoctors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) DoctorProfile.DoctorType doctorType,
            @RequestParam(required = false) String speciality,
            @RequestParam(required = false) DoctorProfile.ConsultationMode consultationMode,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String clinicName,
            @RequestParam(required = false) Integer minYearsOfExperience,
            @RequestParam(required = false) BigDecimal maxConsultationFee,
            @RequestParam(required = false) String language
    ) {
        DoctorFilterDto filter = new DoctorFilterDto();
        filter.setKeyword(keyword);
        filter.setCity(city);
        filter.setCountry(country);
        filter.setActive(active);
        filter.setDoctorType(doctorType);
        filter.setSpeciality(speciality);
        filter.setConsultationMode(consultationMode);
        filter.setVerified(verified);
        filter.setClinicName(clinicName);
        filter.setMinYearsOfExperience(minYearsOfExperience);
        filter.setMaxConsultationFee(maxConsultationFee);
        filter.setLanguage(language);
        return ResponseEntity.ok(userService.filterDoctors(filter));
    }

    // ======================== PATIENTS ========================

    /**
     * GET /api/users/patients
     * Retourne tous les patients (User + PatientProfile combinés).
     */
    @GetMapping("/patients")
    public ResponseEntity<List<PatientFullResponseDto>> getAllPatients() {
        return ResponseEntity.ok(userService.getAllPatients());
    }

    /**
     * GET /api/users/patients/by-user/{userId}
     * Retourne le profil patient complet par userId.
     */
    @GetMapping("/patients/by-user/{userId}")
    public ResponseEntity<PatientFullResponseDto> getPatientByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getPatientByUserId(userId));
    }

    /**
     * GET /api/users/patients/filter?keyword=...&city=...&country=...&active=...
     *     &medicalRecordNumber=...&medicalConsent=...&hasAssignedDoctor=...
     *
     * Filtre dynamique sur les patients. Tous les paramètres sont optionnels.
     */
    @GetMapping("/patients/filter")
    public ResponseEntity<List<PatientFullResponseDto>> filterPatients(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String medicalRecordNumber,
            @RequestParam(required = false) Boolean medicalConsent,
            @RequestParam(required = false) Boolean hasAssignedDoctor
    ) {
        PatientFilterDto filter = new PatientFilterDto();
        filter.setKeyword(keyword);
        filter.setCity(city);
        filter.setCountry(country);
        filter.setActive(active);
        filter.setMedicalRecordNumber(medicalRecordNumber);
        filter.setMedicalConsent(medicalConsent);
        filter.setHasAssignedDoctor(hasAssignedDoctor);
        return ResponseEntity.ok(userService.filterPatients(filter));
    }
}
