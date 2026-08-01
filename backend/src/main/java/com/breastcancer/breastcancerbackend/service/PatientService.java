package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

@Service
public class PatientService {

    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AllergyRepository allergyRepository;
    private final TreatmentRepository treatmentRepository;

    public PatientService(
            PatientProfileRepository patientProfileRepository,
            DoctorProfileRepository doctorProfileRepository,
            AllergyRepository allergyRepository,
            TreatmentRepository treatmentRepository
    ) {
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.allergyRepository = allergyRepository;
        this.treatmentRepository = treatmentRepository;
    }

    // =========================
    // Patient Profile
    // =========================
    public PatientProfileResponseDto getById(UUID patientProfileId) {
        PatientProfile p = patientProfileRepository.findById(patientProfileId)
                .orElseThrow(() -> new NotFoundException("PatientProfile introuvable."));
        return toResponse(p);
    }

    public PatientProfileResponseDto getByUserId(UUID userId) {
        PatientProfile p = patientProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("PatientProfile introuvable pour userId."));
        return toResponse(p);
    }

    @Transactional
    public PatientProfileResponseDto update(UUID patientProfileId, PatientProfileUpdateRequestDto dto) {
        if (dto == null) throw new BadRequestException("Payload requis.");

        PatientProfile p = patientProfileRepository.findById(patientProfileId)
                .orElseThrow(() -> new NotFoundException("PatientProfile introuvable."));

        if (dto.getMedicalRecordNumber() != null) p.setMedicalRecordNumber(dto.getMedicalRecordNumber());
        if (dto.getEmergencyContactName() != null) p.setEmergencyContactName(dto.getEmergencyContactName());
        if (dto.getEmergencyContactPhone() != null) p.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        if (dto.getHeightCm() != null) p.setHeightCm(dto.getHeightCm());
        if (dto.getWeightKg() != null) p.setWeightKg(dto.getWeightKg());

        if (dto.getMedicalConsent() != null) {
            boolean consent = dto.getMedicalConsent();
            p.setMedicalConsent(consent);
            p.setConsentTimestamp(consent ? Instant.now() : null);
        }

        if (dto.getLastKnownLatitude() != null || dto.getLastKnownLongitude() != null) {
            Double lat = dto.getLastKnownLatitude();
            Double lon = dto.getLastKnownLongitude();
            if (!GeoUtils.isValidLatLon(lat, lon)) throw new BadRequestException("Coordonnées invalides.");
            p.setLastKnownLatitude(lat);
            p.setLastKnownLongitude(lon);
        }

        if (dto.getAssignedDoctorProfileId() != null) {
            DoctorProfile d = doctorProfileRepository.findById(dto.getAssignedDoctorProfileId())
                    .orElseThrow(() -> new NotFoundException("DoctorProfile introuvable."));
            p.setAssignedDoctor(d);
        }

        p = patientProfileRepository.save(p);
        return toResponse(p);
    }

    @Transactional
    public PatientProfileResponseDto updateLocation(UUID patientProfileId, LocationUpdateRequestDto dto) {
        if (dto == null) throw new BadRequestException("Payload requis.");

        PatientProfile p = patientProfileRepository.findById(patientProfileId)
                .orElseThrow(() -> new NotFoundException("PatientProfile introuvable."));

        if (!GeoUtils.isValidLatLon(dto.getLatitude(), dto.getLongitude())) {
            throw new BadRequestException("Coordonnées invalides.");
        }

        p.setLastKnownLatitude(dto.getLatitude());
        p.setLastKnownLongitude(dto.getLongitude());

        p = patientProfileRepository.save(p);
        return toResponse(p);
    }

    @Transactional
    public PatientProfileResponseDto setMedicalConsent(UUID patientProfileId, boolean consent) {
        PatientProfile p = patientProfileRepository.findById(patientProfileId)
                .orElseThrow(() -> new NotFoundException("PatientProfile introuvable."));
        p.setMedicalConsent(consent);
        p.setConsentTimestamp(consent ? Instant.now() : null);
        p = patientProfileRepository.save(p);
        return toResponse(p);
    }

    // =========================
    // Allergies
    // =========================
    @Transactional
    public AllergyResponseDto addAllergy(AllergyCreateRequestDto dto) {
        if (dto == null) throw new BadRequestException("Payload requis.");

        PatientProfile p = patientProfileRepository.findById(dto.getPatientProfileId())
                .orElseThrow(() -> new NotFoundException("PatientProfile introuvable."));

        Allergy a = new Allergy();
        a.setPatient(p);
        a.setSubstance(dto.getSubstance());
        a.setReaction(dto.getReaction());
        a.setSeverity(dto.getSeverity());

        a = allergyRepository.save(a);
        return toAllergyResponse(a);
    }

    public List<AllergyResponseDto> listAllergies(UUID patientProfileId) {
        return allergyRepository.findByPatient_Id(patientProfileId)
                .stream()
                .filter(a -> !a.isDeleted()) // respect soft delete
                .map(this::toAllergyResponse)
                .toList();
    }

    @Transactional
    public AllergyResponseDto updateAllergy(UUID allergyId, AllergyUpdateRequestDto dto) {
        if (dto == null) throw new BadRequestException("Payload requis.");

        Allergy a = allergyRepository.findById(allergyId)
                .orElseThrow(() -> new NotFoundException("Allergy introuvable."));

        if (dto.getSubstance() != null) a.setSubstance(dto.getSubstance());
        if (dto.getReaction() != null) a.setReaction(dto.getReaction());
        if (dto.getSeverity() != null) a.setSeverity(dto.getSeverity());

        a = allergyRepository.save(a);
        return toAllergyResponse(a);
    }

    @Transactional
    public void deleteAllergy(UUID allergyId) {
        Allergy a = allergyRepository.findById(allergyId)
                .orElseThrow(() -> new NotFoundException("Allergy introuvable."));
        a.softDelete(); // soft delete instead of hard delete
        allergyRepository.save(a);
    }

    // =========================
    // Treatments
    // =========================
    @Transactional
    public TreatmentResponseDto addTreatment(TreatmentCreateRequestDto dto) {
        if (dto == null) throw new BadRequestException("Payload requis.");

        PatientProfile p = patientProfileRepository.findById(dto.getPatientProfileId())
                .orElseThrow(() -> new NotFoundException("PatientProfile introuvable."));

        Treatment t = new Treatment();
        t.setPatient(p);
        t.setTreatmentType(dto.getTreatmentType());
        t.setProtocol(dto.getProtocol());
        t.setMedicationName(dto.getMedicationName());
        t.setDosage(dto.getDosage());
        t.setStartDate(dto.getStartDate());
        t.setEndDate(dto.getEndDate());
        t.setCyclesTotal(dto.getCyclesTotal());
        t.setStatus(dto.getStatus());
        t.setNotes(dto.getNotes());

        t = treatmentRepository.save(t);
        return toTreatmentResponse(t);
    }

    public List<TreatmentResponseDto> listTreatments(UUID patientProfileId) {
        return treatmentRepository.findByPatient_Id(patientProfileId)
                .stream()
                .filter(t -> !t.isDeleted()) // respect soft delete
                .map(this::toTreatmentResponse)
                .toList();
    }

    public List<TreatmentResponseDto> listTreatmentsByStatus(UUID patientProfileId, Treatment.Status status) {
        return treatmentRepository.findByPatient_IdAndStatus(patientProfileId, status)
                .stream()
                .filter(t -> !t.isDeleted())
                .map(this::toTreatmentResponse)
                .toList();
    }

    @Transactional
    public TreatmentResponseDto updateTreatment(UUID treatmentId, TreatmentUpdateRequestDto dto) {
        if (dto == null) throw new BadRequestException("Payload requis.");

        Treatment t = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new NotFoundException("Treatment introuvable."));

        if (dto.getTreatmentType() != null) t.setTreatmentType(dto.getTreatmentType());
        if (dto.getProtocol() != null) t.setProtocol(dto.getProtocol());
        if (dto.getMedicationName() != null) t.setMedicationName(dto.getMedicationName());
        if (dto.getDosage() != null) t.setDosage(dto.getDosage());
        if (dto.getStartDate() != null) t.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) t.setEndDate(dto.getEndDate());
        if (dto.getCyclesTotal() != null) t.setCyclesTotal(dto.getCyclesTotal());
        if (dto.getStatus() != null) t.setStatus(dto.getStatus());
        if (dto.getNotes() != null) t.setNotes(dto.getNotes());

        t = treatmentRepository.save(t);
        return toTreatmentResponse(t);
    }

    @Transactional
    public void deleteTreatment(UUID treatmentId) {
        Treatment t = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new NotFoundException("Treatment introuvable."));
        t.setDeleted(true); // soft delete
        treatmentRepository.save(t);
    }

    // =========================
    // Mappers
    // =========================
    private PatientProfileResponseDto toResponse(PatientProfile p) {
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
        dto.setBmi(p.getBmi());

        // Clinical monitoring
        dto.setBloodType(p.getBloodType() != null ? p.getBloodType().name() : null);
        dto.setHealthScore(p.getHealthScore());
        dto.setPatientStatus(p.getPatientStatus());

        dto.setMedicalConsent(p.isMedicalConsent());
        dto.setConsentTimestamp(p.getConsentTimestamp());

        dto.setLastKnownLatitude(p.getLastKnownLatitude());
        dto.setLastKnownLongitude(p.getLastKnownLongitude());

        // Allergy list (filter soft-deleted)
        List<AllergyResponseDto> allergies = allergyRepository.findByPatient_Id(p.getId())
                .stream()
                .filter(a -> !a.isDeleted())
                .map(this::toAllergyResponse)
                .toList();
        dto.setAllergies(allergies);

        // Treatment list (filter soft-deleted)
        List<TreatmentResponseDto> treatments = treatmentRepository.findByPatient_Id(p.getId())
                .stream()
                .filter(t -> !t.isDeleted())
                .map(this::toTreatmentResponse)
                .toList();
        dto.setTreatments(treatments);

        return dto;
    }

    private AllergyResponseDto toAllergyResponse(Allergy a) {
        AllergyResponseDto dto = new AllergyResponseDto();
        dto.setId(a.getId());
        dto.setPatientProfileId(a.getPatient() != null ? a.getPatient().getId() : null);
        dto.setSubstance(a.getSubstance());
        dto.setReaction(a.getReaction());
        dto.setSeverity(a.getSeverity());
        return dto;
    }

    private TreatmentResponseDto toTreatmentResponse(Treatment t) {
        TreatmentResponseDto dto = new TreatmentResponseDto();
        dto.setId(t.getId());
        dto.setPatientProfileId(t.getPatient() != null ? t.getPatient().getId() : null);
        dto.setTreatmentType(t.getTreatmentType());
        dto.setProtocol(t.getProtocol());
        dto.setMedicationName(t.getMedicationName());
        dto.setDosage(t.getDosage());
        dto.setStartDate(t.getStartDate());
        dto.setEndDate(t.getEndDate());
        dto.setCyclesTotal(t.getCyclesTotal());
        dto.setCurrentCycle(t.getCurrentCycle());
        dto.setStatus(t.getStatus());
        dto.setNotes(t.getNotes());
        return dto;
    }
}
