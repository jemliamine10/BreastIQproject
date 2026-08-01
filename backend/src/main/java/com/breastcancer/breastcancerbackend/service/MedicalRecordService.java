package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

/**
 * MedicalRecordService — manages the full medical record lifecycle.
 * Can create, update sections independently, and return a fully aggregated DTO.
 */
@Service
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final ClinicalDataRepository clinicalDataRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AllergyRepository allergyRepository;
    private final TimelineService timelineService;
    private final DoctorPatientLinkGuardService doctorPatientLinkGuardService;
    private final TreatmentManagementService treatmentManagementService;
    private final TreatmentRepository treatmentRepository;
    private final CancerStagingEngine cancerStagingEngine;

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository,
                                ClinicalDataRepository clinicalDataRepository,
                                MedicalHistoryRepository medicalHistoryRepository,
                                PatientProfileRepository patientProfileRepository,
                                AllergyRepository allergyRepository,
                                TimelineService timelineService,
                                DoctorPatientLinkGuardService doctorPatientLinkGuardService,
                                TreatmentManagementService treatmentManagementService,
                                TreatmentRepository treatmentRepository,
                                CancerStagingEngine cancerStagingEngine) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.clinicalDataRepository = clinicalDataRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.allergyRepository = allergyRepository;
        this.timelineService = timelineService;
        this.doctorPatientLinkGuardService = doctorPatientLinkGuardService;
        this.treatmentManagementService = treatmentManagementService;
        this.treatmentRepository = treatmentRepository;
        this.cancerStagingEngine = cancerStagingEngine;
    }

    @Transactional
    public MedicalRecordResponseDto createFullRecord(MedicalRecordCreateDto dto, UUID doctorId) {
        if (dto.getPatientId() == null) throw new BadRequestException("patientId requis.");
        doctorPatientLinkGuardService.assertActiveLink(dto.getPatientId(), doctorId);

        PatientProfile patient = patientProfileRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));

        if (medicalRecordRepository.existsByPatient_Id(patient.getId())) {
            throw new ConflictException("Un dossier médical existe déjà pour cette patiente.");
        }

        // Update patient profile with health info
        if (dto.getBloodType() != null) patient.setBloodType(dto.getBloodType());
        if (dto.getHeightCm() != null) patient.setHeightCm(dto.getHeightCm());
        if (dto.getWeightKg() != null) patient.setWeightKg(dto.getWeightKg());
        patientProfileRepository.save(patient);

        // Create medical record
        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setDiagnosis(dto.getDiagnosis());
        record.setCancerStage(dto.getCancerStage());
        record.setTumorType(dto.getTumorType());
        record.setConsentGiven(dto.isConsentGiven());
        record.setNotes(dto.getNotes());
        record = medicalRecordRepository.save(record);

        // Create clinical data if provided
        if (dto.getClinicalData() != null) {
            ClinicalData cd = toClinicalDataEntity(dto.getClinicalData(), record);
            clinicalDataRepository.save(cd);
            record.setClinicalData(cd);

            // Auto-compute stage from clinical data
            CancerStagingEngine.TnmResult tnm = cancerStagingEngine.compute(cd);
            if (tnm != null) {
                record.setCancerStage(tnm.getStage());
                record.setTnmClassification(tnm.getClassification());
                record.setStageAutoComputed(true);
                record = medicalRecordRepository.save(record);
            }
        }

        // Timeline event
        timelineService.recordEvent(
                patient.getId(),
                MedicalEvent.EventType.DIAGNOSIS,
                "Dossier médical créé — " + (dto.getDiagnosis() != null ? dto.getDiagnosis() : "Diagnostic en cours"),
                "Stade: " + dto.getCancerStage() + ", Type: " + dto.getTumorType(),
                null,
                record.getId(),
                "MEDICAL_RECORD"
        );

        return toAggregatedDto(record, patient, doctorId);
    }

    private MedicalRecord getOrCreateRecordEntity(PatientProfile patient) {
        return medicalRecordRepository.findByPatient_Id(patient.getId())
                .orElseGet(() -> {
                    MedicalRecord record = new MedicalRecord();
                    record.setPatient(patient);
                    record.setDiagnosis("Diagnostic en cours"); // Valeur par défaut
                    return medicalRecordRepository.save(record);
                });
    }

    @Transactional
    public MedicalRecordResponseDto updateDiagnosis(UUID patientId, UUID doctorId, String diagnosis,
                                                      MedicalRecord.CancerStage cancerStage,
                                                      MedicalRecord.TumorType tumorType) {
        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);

        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));
        
        MedicalRecord record = getOrCreateRecordEntity(patient);


        if (diagnosis != null) record.setDiagnosis(diagnosis);
        if (cancerStage != null) record.setCancerStage(cancerStage);
        if (tumorType != null) record.setTumorType(tumorType);

        record = medicalRecordRepository.save(record);
        return toAggregatedDto(record, record.getPatient(), doctorId);
    }

    @Transactional
    public ClinicalDataDto updateClinicalData(UUID patientId, UUID doctorId, ClinicalDataDto dto) {
        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);

        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));

        MedicalRecord record = getOrCreateRecordEntity(patient);

        ClinicalData cd = record.getClinicalData();
        if (cd == null) {
            cd = toClinicalDataEntity(dto, record);
        } else {
            if (dto.getEstrogenReceptor() != null) cd.setEstrogenReceptor(dto.getEstrogenReceptor());
            if (dto.getProgesteroneReceptor() != null) cd.setProgesteroneReceptor(dto.getProgesteroneReceptor());
            if (dto.getHer2Status() != null) cd.setHer2Status(dto.getHer2Status());
            if (dto.getKi67() != null) cd.setKi67(dto.getKi67());
            if (dto.getTumorSize() != null) cd.setTumorSize(dto.getTumorSize());
            if (dto.getLymphNodesInvolved() != null) cd.setLymphNodesInvolved(dto.getLymphNodesInvolved());
            cd.setMetastasis(dto.isMetastasis());
            if (dto.getGrade() != null) cd.setGrade(dto.getGrade());
            if (dto.getNotes() != null) cd.setNotes(dto.getNotes());
        }
        clinicalDataRepository.save(cd);

        // Auto-compute stage from updated clinical data
        CancerStagingEngine.TnmResult tnm = cancerStagingEngine.compute(cd);
        if (tnm != null) {
            record.setCancerStage(tnm.getStage());
            record.setTnmClassification(tnm.getClassification());
            record.setStageAutoComputed(true);
            medicalRecordRepository.save(record);
        }

        return toClinicalDataDto(cd);
    }

    @Transactional
    public MedicalHistoryDto addMedicalHistory(UUID patientId, UUID doctorId, MedicalHistoryDto dto) {
        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);

        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));

        MedicalHistory h = new MedicalHistory();
        h.setPatient(patient);
        h.setHistoryType(dto.getHistoryType());
        h.setTitle(dto.getTitle());
        h.setDescription(dto.getDescription());
        h.setEventDate(dto.getEventDate());
        h = medicalHistoryRepository.save(h);

        return toMedicalHistoryDto(h);
    }

    @Transactional
    public void deleteMedicalHistory(UUID historyId, UUID doctorId) {
        MedicalHistory h = medicalHistoryRepository.findById(historyId)
                .orElseThrow(() -> new NotFoundException("Antécédent introuvable."));
        doctorPatientLinkGuardService.assertActiveLink(h.getPatient().getId(), doctorId);
        h.softDelete();
        medicalHistoryRepository.save(h);
    }

    @Transactional
    public MedicalRecordResponseDto getFullRecord(UUID patientId, UUID doctorId) {
        doctorPatientLinkGuardService.assertActiveLink(patientId, doctorId);

        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));

        MedicalRecord record = getOrCreateRecordEntity(patient);
        return toAggregatedDto(record, patient, doctorId);
    }

    @Transactional
    public MedicalRecordResponseDto getFullRecordForPatient(UUID patientId) {
        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));

        MedicalRecord record = getOrCreateRecordEntity(patient);
        return toAggregatedDto(record, patient, null);
    }

    @Transactional
    public MedicalHistoryDto addMedicalHistoryForPatient(UUID patientId, MedicalHistoryDto dto) {
        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));

        MedicalHistory h = new MedicalHistory();
        h.setPatient(patient);
        h.setHistoryType(dto.getHistoryType());
        h.setTitle(dto.getTitle());
        h.setDescription(dto.getDescription());
        h.setEventDate(dto.getEventDate());
        h = medicalHistoryRepository.save(h);

        return toMedicalHistoryDto(h);
    }

    @Transactional
    public MedicalHistoryDto updateMedicalHistoryForPatient(UUID historyId, MedicalHistoryDto dto) {
        MedicalHistory h = medicalHistoryRepository.findById(historyId)
                .orElseThrow(() -> new NotFoundException("Antécédent introuvable."));

        if (dto.getHistoryType() != null) h.setHistoryType(dto.getHistoryType());
        if (dto.getTitle() != null) h.setTitle(dto.getTitle());
        if (dto.getDescription() != null) h.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) h.setEventDate(dto.getEventDate());

        h = medicalHistoryRepository.save(h);
        return toMedicalHistoryDto(h);
    }

    @Transactional
    public void deleteMedicalHistoryForPatient(UUID historyId) {
        MedicalHistory h = medicalHistoryRepository.findById(historyId)
                .orElseThrow(() -> new NotFoundException("Antécédent introuvable."));
        h.softDelete();
        medicalHistoryRepository.save(h);
    }

    // ===== Mappers =====
    private MedicalRecordResponseDto toAggregatedDto(MedicalRecord record, PatientProfile patient, UUID doctorId) {
        MedicalRecordResponseDto dto = new MedicalRecordResponseDto();
        dto.setId(record.getId());
        dto.setPatientId(patient.getId());
        dto.setDiagnosis(record.getDiagnosis());
        dto.setCancerStage(record.getCancerStage());
        dto.setTumorType(record.getTumorType());
        dto.setConsentGiven(record.isConsentGiven());
        dto.setNotes(record.getNotes());

        // From patient profile
        dto.setBloodType(patient.getBloodType() != null ? patient.getBloodType().name() : null);
        dto.setHeightCm(patient.getHeightCm());
        dto.setWeightKg(patient.getWeightKg());
        dto.setBmi(patient.getBmi());

        // Clinical data
        if (record.getClinicalData() != null) {
            dto.setClinicalData(toClinicalDataDto(record.getClinicalData()));
        }

        // Medical histories (non-deleted)
        List<MedicalHistoryDto> histories = medicalHistoryRepository
                .findByPatient_IdAndDeletedFalseOrderByEventDateDesc(patient.getId())
                .stream().map(this::toMedicalHistoryDto).toList();
        dto.setMedicalHistories(histories);

        // Allergies (non-deleted)
        List<AllergyResponseDto> allergies = allergyRepository.findByPatient_Id(patient.getId())
                .stream()
                .filter(a -> !a.isDeleted())
                .map(this::toAllergyDto)
                .toList();
        dto.setAllergies(allergies);

        // Treatments
        if (doctorId != null) {
            dto.setTreatments(treatmentManagementService.listTreatmentsForLinkedPair(doctorId, patient.getId(), null));
        } else {
            // Internal call logic for patient or simplified
            dto.setTreatments(treatmentRepository.findByPatient_Id(patient.getId())
                    .stream()
                    .filter(t -> !t.isDeleted())
                    .map(this::toTreatmentDto)
                    .toList());
        }

        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());

        // TNM staging info
        dto.setTnmClassification(record.getTnmClassification());
        dto.setStageAutoComputed(record.isStageAutoComputed());
        if (record.getCancerStage() != null) {
            String tnm = record.getTnmClassification() != null ? record.getTnmClassification() : "";
            String label;
            switch (record.getCancerStage()) {
                case STAGE_0:   label = "Stade 0"; break;
                case STAGE_I:   label = "Stade I"; break;
                case STAGE_II:  label = "Stade II"; break;
                case STAGE_III: label = "Stade III"; break;
                case STAGE_IV:  label = "Stade IV"; break;
                default:        label = "Non déterminé";
            }
            dto.setComputedStageLabel(label + (tnm.isEmpty() ? "" : " — " + tnm));
        }

        return dto;
    }


    private TreatmentResponseDto toTreatmentDto(Treatment t) {
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
        dto.setStatus(t.getComputedStatus());
        dto.setNotes(t.getNotes());
        return dto;
    }

    private ClinicalData toClinicalDataEntity(ClinicalDataDto dto, MedicalRecord record) {
        ClinicalData cd = new ClinicalData();
        cd.setMedicalRecord(record);
        cd.setEstrogenReceptor(dto.getEstrogenReceptor());
        cd.setProgesteroneReceptor(dto.getProgesteroneReceptor());
        cd.setHer2Status(dto.getHer2Status());
        cd.setKi67(dto.getKi67());
        cd.setTumorSize(dto.getTumorSize());
        cd.setLymphNodesInvolved(dto.getLymphNodesInvolved());
        cd.setMetastasis(dto.isMetastasis());
        cd.setGrade(dto.getGrade());
        cd.setNotes(dto.getNotes());
        return cd;
    }

    private ClinicalDataDto toClinicalDataDto(ClinicalData cd) {
        ClinicalDataDto dto = new ClinicalDataDto();
        dto.setId(cd.getId());
        dto.setEstrogenReceptor(cd.getEstrogenReceptor());
        dto.setProgesteroneReceptor(cd.getProgesteroneReceptor());
        dto.setHer2Status(cd.getHer2Status());
        dto.setKi67(cd.getKi67());
        dto.setTumorSize(cd.getTumorSize());
        dto.setLymphNodesInvolved(cd.getLymphNodesInvolved());
        dto.setMetastasis(cd.isMetastasis());
        dto.setGrade(cd.getGrade());
        dto.setNotes(cd.getNotes());
        return dto;
    }

    private MedicalHistoryDto toMedicalHistoryDto(MedicalHistory h) {
        MedicalHistoryDto dto = new MedicalHistoryDto();
        dto.setId(h.getId());
        dto.setPatientId(h.getPatient().getId());
        dto.setHistoryType(h.getHistoryType());
        dto.setTitle(h.getTitle());
        dto.setDescription(h.getDescription());
        dto.setEventDate(h.getEventDate());
        return dto;
    }

    private AllergyResponseDto toAllergyDto(Allergy a) {
        AllergyResponseDto dto = new AllergyResponseDto();
        dto.setId(a.getId());
        dto.setPatientProfileId(a.getPatient().getId());
        dto.setSubstance(a.getSubstance());
        dto.setReaction(a.getReaction());
        dto.setSeverity(a.getSeverity());
        return dto;
    }
}
