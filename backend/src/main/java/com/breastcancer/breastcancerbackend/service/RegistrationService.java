package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;


import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;

    public RegistrationService(
            UserRepository userRepository,
            DoctorProfileRepository doctorProfileRepository,
            PatientProfileRepository patientProfileRepository
    ) {
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
    }

    public boolean checkEmailAvailable(String email) {
        if (email == null || email.isBlank()) throw new BadRequestException("Email requis.");
        return !userRepository.existsByEmailIgnoreCase(email.trim());
    }

    public List<DoctorProfile.DoctorType> listDoctorTypes() {
        return Arrays.asList(DoctorProfile.DoctorType.values());
    }

    public List<User.Gender> listGenders() {
        return Arrays.asList(User.Gender.values());
    }

    @Transactional
    public DoctorProfileResponseDto registerDoctor(UserCreateRequestDto userDto, DoctorProfileCreateRequestDto doctorDto) {
        if (userDto == null || doctorDto == null) throw new BadRequestException("Payload requis.");

        // ✅ rôle doit être DOCTOR
        if (userDto.getRole() != User.Role.DOCTOR) {
            throw new BadRequestException("Role invalide: expected DOCTOR.");
        }

        String email = userDto.getEmail() != null ? userDto.getEmail().trim() : null;
        if (email == null || email.isBlank()) throw new BadRequestException("Email requis.");

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email déjà utilisé.");
        }
        if (doctorProfileRepository.existsByLicenseNumber(doctorDto.getLicenseNumber())) {
            throw new ConflictException("Numéro de licence déjà utilisé.");
        }

        User user = new User();
        user.setEmail(email);

        // ✅ pour l’instant: stockage brut dans passwordHash (à remplacer par BCrypt)
        user.setPasswordHash(userDto.getPassword());

        user.setRole(User.Role.DOCTOR);
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setPhone(userDto.getPhone());
        user.setGender(userDto.getGender());
        user.setDateOfBirth(userDto.getDateOfBirth());
        user.setProfilePhotoUrl(userDto.getProfilePhotoUrl());
        user.setAddressText(userDto.getAddressText());
        user.setCity(userDto.getCity());
        user.setCountry(userDto.getCountry());
        user.setActive(true);
        user.setEmailVerified(false);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        user = userRepository.save(user);

        DoctorProfile profile = new DoctorProfile();
        profile.setUser(user);
        profile.setDoctorType(doctorDto.getDoctorType());
        profile.setSpeciality(doctorDto.getSpeciality());
        profile.setLicenseNumber(doctorDto.getLicenseNumber());
        profile.setClinicName(doctorDto.getClinicName());
        profile.setBio(doctorDto.getBio());
        profile.setYearsOfExperience(doctorDto.getYearsOfExperience());
        profile.setLanguages(doctorDto.getLanguages());
        profile.setConsultationMode(doctorDto.getConsultationMode());
        profile.setConsultationFee(doctorDto.getConsultationFee());
        profile.setAddressText(doctorDto.getAddressText());
        profile.setLatitude(doctorDto.getLatitude());
        profile.setLongitude(doctorDto.getLongitude());
        if (doctorDto.getTimezone() != null && !doctorDto.getTimezone().isBlank()) {
            profile.setTimezone(doctorDto.getTimezone());
        }
        profile.setVerified(false);
        profile.setVerifiedAt(null);

        profile = doctorProfileRepository.save(profile);
        return toDoctorResponse(profile);
    }

    @Transactional
    public PatientProfileResponseDto registerPatient(UserCreateRequestDto userDto, PatientProfileCreateRequestDto patientDto) {
        if (userDto == null || patientDto == null) throw new BadRequestException("Payload requis.");

        // ✅ rôle doit être PATIENT
        if (userDto.getRole() != User.Role.PATIENT) {
            throw new BadRequestException("Role invalide: expected PATIENT.");
        }

        String email = userDto.getEmail() != null ? userDto.getEmail().trim() : null;
        if (email == null || email.isBlank()) throw new BadRequestException("Email requis.");

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email déjà utilisé.");
        }

        User user = new User();
        user.setEmail(email);

        // ✅ pour l’instant: stockage brut dans passwordHash (à remplacer par BCrypt)
        user.setPasswordHash(userDto.getPassword());

        user.setRole(User.Role.PATIENT);
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setPhone(userDto.getPhone());
        user.setGender(userDto.getGender());
        user.setDateOfBirth(userDto.getDateOfBirth());
        user.setProfilePhotoUrl(userDto.getProfilePhotoUrl());
        user.setAddressText(userDto.getAddressText());
        user.setCity(userDto.getCity());
        user.setCountry(userDto.getCountry());
        user.setActive(true);
        user.setEmailVerified(false);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        user = userRepository.save(user);

        PatientProfile p = new PatientProfile();
        p.setUser(user);
        p.setMedicalRecordNumber(patientDto.getMedicalRecordNumber());
        p.setEmergencyContactName(patientDto.getEmergencyContactName());
        p.setEmergencyContactPhone(patientDto.getEmergencyContactPhone());
        p.setHeightCm(patientDto.getHeightCm());
        p.setWeightKg(patientDto.getWeightKg());

        p.setMedicalConsent(patientDto.isMedicalConsent());
        if (patientDto.isMedicalConsent()) {
            p.setConsentTimestamp(Instant.now()); // plus cohérent que recevoir du front
        } else {
            p.setConsentTimestamp(null);
        }

        p.setLastKnownLatitude(patientDto.getLastKnownLatitude());
        p.setLastKnownLongitude(patientDto.getLastKnownLongitude());

        p = patientProfileRepository.save(p);
        return toPatientResponse(p);
    }

    // ====== Mappers ======

    private DoctorProfileResponseDto toDoctorResponse(DoctorProfile dp) {
        DoctorProfileResponseDto dto = new DoctorProfileResponseDto();
        dto.setId(dp.getId());
        dto.setUserId(dp.getUser() != null ? dp.getUser().getId() : null);
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
        dto.setTimezone(dp.getTimezone());
        return dto;
    }

    private PatientProfileResponseDto toPatientResponse(PatientProfile p) {
        PatientProfileResponseDto dto = new PatientProfileResponseDto();
        dto.setId(p.getId());
        dto.setUserId(p.getUser() != null ? p.getUser().getId() : null);

        dto.setAssignedDoctorProfileId(
                p.getAssignedDoctor() != null ? p.getAssignedDoctor().getId() : null
        );

        dto.setMedicalRecordNumber(p.getMedicalRecordNumber());
        dto.setEmergencyContactName(p.getEmergencyContactName());
        dto.setEmergencyContactPhone(p.getEmergencyContactPhone());
        dto.setHeightCm(p.getHeightCm());
        dto.setWeightKg(p.getWeightKg());

        dto.setMedicalConsent(p.isMedicalConsent());
        dto.setConsentTimestamp(p.getConsentTimestamp());

        dto.setLastKnownLatitude(p.getLastKnownLatitude());
        dto.setLastKnownLongitude(p.getLastKnownLongitude());

        dto.setAllergies(null);
        dto.setTreatments(null);
        return dto;
    }
}
