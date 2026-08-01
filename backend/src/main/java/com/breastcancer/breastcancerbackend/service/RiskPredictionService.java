package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.RiskPredictionRequestDto;
import com.breastcancer.breastcancerbackend.dto.RiskPredictionResponseDto;
import com.breastcancer.breastcancerbackend.entity.ClinicalData;
import com.breastcancer.breastcancerbackend.entity.MedicalRecord;
import com.breastcancer.breastcancerbackend.entity.PatientProfile;
import com.breastcancer.breastcancerbackend.entity.Treatment;
import com.breastcancer.breastcancerbackend.repository.MedicalRecordRepository;
import com.breastcancer.breastcancerbackend.repository.PatientProfileRepository;
import com.breastcancer.breastcancerbackend.repository.TreatmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

/**
 * Communicates with the Python FastAPI microservice on port 8002.
 * Can auto-populate features from existing patient clinical data.
 */
@Service
@Transactional(readOnly = true)
public class RiskPredictionService {

    private static final Logger LOG = LoggerFactory.getLogger(RiskPredictionService.class);

    @Value("${risk.prediction.api.url:http://localhost:8002}")
    private String riskApiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PatientProfileRepository patientProfileRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final TreatmentRepository treatmentRepository;

    public RiskPredictionService(
            ObjectMapper objectMapper,
            PatientProfileRepository patientProfileRepository,
            MedicalRecordRepository medicalRecordRepository,
            TreatmentRepository treatmentRepository
    ) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.patientProfileRepository = patientProfileRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.treatmentRepository = treatmentRepository;
    }

    /**
     * Check if the risk prediction AI service is available.
     */
    public boolean isRiskServiceHealthy() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(riskApiUrl + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            LOG.warn("Risk prediction service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Direct prediction: send all 22 features to the Python API.
     */
    public RiskPredictionResponseDto predict(RiskPredictionRequestDto request) {
        try {
            LOG.info("=== START RISK PREDICTION API CALL ===");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonBody = objectMapper.writeValueAsString(request);
            LOG.info("Payload sent to port 8002: {}", jsonBody);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<RiskPredictionResponseDto> response = restTemplate.exchange(
                    riskApiUrl + "/predict-risk",
                    HttpMethod.POST,
                    entity,
                    RiskPredictionResponseDto.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                LOG.info("Prediction successful! Score: {}", response.getBody().getProbabilityPercent());
                return response.getBody();
            }

            throw new RuntimeException("AI service error: " + response.getStatusCode());
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            LOG.error("AI service returned HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("L'IA (port 8002) a retourné une erreur: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            LOG.error("CRITICAL ERROR during risk prediction call: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur de communication avec le service IA (8002). " + e.getMessage(), e);
        }
    }

    /**
     * Predict for a known patient — auto-populate features from the database,
     * then merge with any manually-provided overrides.
     */
    public RiskPredictionResponseDto predictForPatient(UUID patientProfileId, RiskPredictionRequestDto overrides) {
        LOG.info("--- PREDICT FOR PATIENT START: id={} ---", patientProfileId);
        try {
            PatientProfile patient = patientProfileRepository.findById(patientProfileId)
                    .orElseThrow(() -> new NotFoundException("Patient introuvable: " + patientProfileId));
            LOG.info("Found patient: {} {}", patient.getUser().getFirstName(), patient.getUser().getLastName());

            // Find medical record
            Optional<MedicalRecord> recordOpt = medicalRecordRepository.findByPatient_Id(patientProfileId);
            LOG.info("Medical record found: {}", recordOpt.isPresent());

            // Fetch treatments
            List<Treatment> treatments = treatmentRepository.findByPatient_Id(patientProfileId);
            LOG.info("Treatments count: {}", treatments.size());

            // Build the request with auto-populated fields
            LOG.info("Building base request from DB data...");
            RiskPredictionRequestDto request = buildFromPatientData(patient, recordOpt.orElse(null), treatments);

            // Merge manual overrides (non-null values from overrides take precedence)
            LOG.info("Merging with UI overrides...");
            mergeOverrides(request, overrides);

            LOG.info("Final request ready. Calling Python API...");
            return predict(request);
        } catch (Exception e) {
            LOG.error("Failed in predictForPatient: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Build a request DTO from existing patient data.
     */
    private RiskPredictionRequestDto buildFromPatientData(PatientProfile patient, MedicalRecord record, List<Treatment> treatments) {
        RiskPredictionRequestDto dto = new RiskPredictionRequestDto();

        // 1. Age at diagnosis
        if (patient.getUser() != null && patient.getUser().getDateOfBirth() != null) {
            LocalDate dob = patient.getUser().getDateOfBirth();
            double age = Period.between(dob, LocalDate.now()).getYears();
            dto.setAgeAtDiagnosis(age);
        } else {
            LOG.debug("Date of birth missing for patient {}, defaulting age to 60", patient.getId());
            dto.setAgeAtDiagnosis(60.0); // Default
        }

        // 2. Clinical Data from Medical Record
        if (record != null) {
            ClinicalData clinical = record.getClinicalData();
            if (clinical != null) {
                // Tumor size
                if (clinical.getTumorSize() != null) {
                    dto.setTumorSize(clinical.getTumorSize());
                }

                // Grade
                if (clinical.getGrade() != null) {
                    dto.setNeoplasmHistologicGrade(clinical.getGrade().doubleValue());
                }

                // Lymph nodes
                if (clinical.getLymphNodesInvolved() != null) {
                    dto.setLymphNodesPositive(clinical.getLymphNodesInvolved().doubleValue());
                }

                // ER Status
                if (clinical.getEstrogenReceptor() != null) {
                    dto.setErStatus(mapReceptorStatus(clinical.getEstrogenReceptor()));
                    dto.setErStatusIhc(mapReceptorStatusIhc(clinical.getEstrogenReceptor()));
                }

                // PR Status
                if (clinical.getProgesteroneReceptor() != null) {
                    dto.setPrStatus(mapReceptorStatus(clinical.getProgesteroneReceptor()));
                }

                // HER2 Status
                if (clinical.getHer2Status() != null) {
                    dto.setHer2Status(mapReceptorStatus(clinical.getHer2Status()));
                }
            }

            // Tumor stage from cancer stage enum
            if (record.getCancerStage() != null) {
                dto.setTumorStage(mapCancerStageToNumber(record.getCancerStage().name()));
            }
        }

        // 3. Detect therapies from treatments
        if (treatments != null && !treatments.isEmpty()) {
            for (Treatment t : treatments) {
                if (t.getTreatmentType() != null) {
                    String tName = t.getTreatmentType().name().toUpperCase();
                    if (tName.contains("CHEMO")) dto.setChemotherapy("Yes");
                    if (tName.contains("RADIO")) dto.setRadioTherapy("Yes");
                    if (tName.contains("HORMONAL") || tName.contains("HORMONE")) dto.setHormoneTherapy("Yes");
                }
            }
        }

        return dto;
    }

    /**
     * Merge non-null overrides into the base request.
     */
    private void mergeOverrides(RiskPredictionRequestDto base, RiskPredictionRequestDto overrides) {
        if (overrides == null) return;

        if (overrides.getAgeAtDiagnosis() != null) base.setAgeAtDiagnosis(overrides.getAgeAtDiagnosis());
        if (overrides.getTypeOfBreastSurgery() != null && !"Unknown".equals(overrides.getTypeOfBreastSurgery()))
            base.setTypeOfBreastSurgery(overrides.getTypeOfBreastSurgery());
        if (overrides.getCellularity() != null && !"Unknown".equals(overrides.getCellularity()))
            base.setCellularity(overrides.getCellularity());
        if (overrides.getChemotherapy() != null && !"Unknown".equals(overrides.getChemotherapy()))
            base.setChemotherapy(overrides.getChemotherapy());
        if (overrides.getPam50SubType() != null && !"Unknown".equals(overrides.getPam50SubType()))
            base.setPam50SubType(overrides.getPam50SubType());
        if (overrides.getErStatusIhc() != null && !"Unknown".equals(overrides.getErStatusIhc()))
            base.setErStatusIhc(overrides.getErStatusIhc());
        if (overrides.getErStatus() != null && !"Unknown".equals(overrides.getErStatus()))
            base.setErStatus(overrides.getErStatus());
        if (overrides.getNeoplasmHistologicGrade() != null)
            base.setNeoplasmHistologicGrade(overrides.getNeoplasmHistologicGrade());
        if (overrides.getHer2StatusSnp6() != null && !"Unknown".equals(overrides.getHer2StatusSnp6()))
            base.setHer2StatusSnp6(overrides.getHer2StatusSnp6());
        if (overrides.getHer2Status() != null && !"Unknown".equals(overrides.getHer2Status()))
            base.setHer2Status(overrides.getHer2Status());
        if (overrides.getTumorHistologicSubtype() != null && !"Unknown".equals(overrides.getTumorHistologicSubtype()))
            base.setTumorHistologicSubtype(overrides.getTumorHistologicSubtype());
        if (overrides.getHormoneTherapy() != null && !"Unknown".equals(overrides.getHormoneTherapy()))
            base.setHormoneTherapy(overrides.getHormoneTherapy());
        if (overrides.getMenopausalState() != null && !"Unknown".equals(overrides.getMenopausalState()))
            base.setMenopausalState(overrides.getMenopausalState());
        if (overrides.getIntegrativeCluster() != null && !"Unknown".equals(overrides.getIntegrativeCluster()))
            base.setIntegrativeCluster(overrides.getIntegrativeCluster());
        if (overrides.getTumorLaterality() != null && !"Unknown".equals(overrides.getTumorLaterality()))
            base.setTumorLaterality(overrides.getTumorLaterality());
        if (overrides.getLymphNodesPositive() != null) base.setLymphNodesPositive(overrides.getLymphNodesPositive());
        if (overrides.getMutationCount() != null) base.setMutationCount(overrides.getMutationCount());
        if (overrides.getNottinghamIndex() != null) base.setNottinghamIndex(overrides.getNottinghamIndex());
        if (overrides.getPrStatus() != null && !"Unknown".equals(overrides.getPrStatus()))
            base.setPrStatus(overrides.getPrStatus());
        if (overrides.getRadioTherapy() != null && !"Unknown".equals(overrides.getRadioTherapy()))
            base.setRadioTherapy(overrides.getRadioTherapy());
        if (overrides.getThreeGeneSubtype() != null && !"Unknown".equals(overrides.getThreeGeneSubtype()))
            base.setThreeGeneSubtype(overrides.getThreeGeneSubtype());
        if (overrides.getTumorSize() != null) base.setTumorSize(overrides.getTumorSize());
        if (overrides.getTumorStage() != null) base.setTumorStage(overrides.getTumorStage());
    }

    // ── Enum Mappers ──

    private String mapReceptorStatus(ClinicalData.ReceptorStatus status) {
        return switch (status) {
            case POSITIVE -> "Positive";
            case NEGATIVE -> "Negative";
            case UNKNOWN -> "Unknown";
        };
    }

    private String mapReceptorStatusIhc(ClinicalData.ReceptorStatus status) {
        return switch (status) {
            case POSITIVE -> "Positve"; // Note: typo preserved from METABRIC dataset
            case NEGATIVE -> "Negative";
            case UNKNOWN -> "Unknown";
        };
    }

    private double mapCancerStageToNumber(String stage) {
        if (stage == null) return 2.0;
        return switch (stage.toUpperCase()) {
            case "STAGE_0" -> 0.0;
            case "STAGE_I", "STAGE_IA", "STAGE_IB" -> 1.0;
            case "STAGE_II", "STAGE_IIA", "STAGE_IIB" -> 2.0;
            case "STAGE_III", "STAGE_IIIA", "STAGE_IIIB", "STAGE_IIIC" -> 3.0;
            case "STAGE_IV" -> 4.0;
            default -> 2.0;
        };
    }
}
