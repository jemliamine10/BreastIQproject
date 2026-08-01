package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.MammogramAnalysisDetailDto;
import com.breastcancer.breastcancerbackend.dto.MammogramAnalysisHistoryDto;
import com.breastcancer.breastcancerbackend.dto.MammogramAnalysisResponseDto;
import com.breastcancer.breastcancerbackend.entity.DoctorProfile;
import com.breastcancer.breastcancerbackend.entity.MammogramAnalysis;
import com.breastcancer.breastcancerbackend.entity.PatientProfile;
import com.breastcancer.breastcancerbackend.repository.DoctorProfileRepository;
import com.breastcancer.breastcancerbackend.repository.MammogramAnalysisRepository;
import com.breastcancer.breastcancerbackend.repository.PatientProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

/**
 * Service responsible for communicating with the Python FastAPI AI prediction service,
 * persisting analysis results linked to patients, and generating AI reports.
 */
@Service
public class MammogramAnalysisService {

    private static final Logger LOG = LoggerFactory.getLogger(MammogramAnalysisService.class);

    @Value("${ai.api.url:http://localhost:8000}")
    private String aiApiUrl;

    @Value("${app.storage.mammogram-analysis.path:./uploads/mammogram-analysis}")
    private String storagePath;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MammogramAnalysisRepository analysisRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    public MammogramAnalysisService(
            ObjectMapper objectMapper,
            MammogramAnalysisRepository analysisRepository,
            PatientProfileRepository patientProfileRepository,
            DoctorProfileRepository doctorProfileRepository
    ) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.analysisRepository = analysisRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    /**
     * Check if the AI service is available.
     */
    public boolean isAiServiceHealthy() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(aiApiUrl + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Analyze a mammogram image, persist the result linked to a patient.
     */
    @Transactional
    public MammogramAnalysisResponseDto analyzeMammogram(
            MultipartFile file,
            String pixelSpacing,
            UUID patientProfileId,
            UUID doctorProfileId
    ) throws IOException {

        // Resolve patient and doctor
        PatientProfile patient = patientProfileRepository.findById(patientProfileId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable: " + patientProfileId));
        DoctorProfile doctor = doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new NotFoundException("Médecin introuvable: " + doctorProfileId));

        // Call AI service
        MammogramAnalysisResponseDto aiResult = callAiPredict(file, pixelSpacing);

        // Create analysis entity
        MammogramAnalysis analysis = new MammogramAnalysis();
        analysis.setPatient(patient);
        analysis.setDoctor(doctor);
        analysis.setAnalysisDate(Instant.now());
        analysis.setGlobalVerdict(aiResult.getGlobalVerdict());
        analysis.setGlobalConfidence(aiResult.getGlobalConfidence());
        analysis.setDetectionsCount(
                aiResult.getIndividualPredictions() != null ? aiResult.getIndividualPredictions().size() : 0
        );

        // Serialize predictions without base64 images (too large for DB)
        if (aiResult.getIndividualPredictions() != null) {
            List<Map<String, Object>> lightPredictions = new ArrayList<>();
            for (MammogramAnalysisResponseDto.PredictionDto pred : aiResult.getIndividualPredictions()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("label", pred.getLabel());
                m.put("classification", pred.getClassification());
                m.put("score", pred.getScore());
                m.put("features", pred.getFeatures());
                lightPredictions.add(m);
            }
            analysis.setPredictionsJson(objectMapper.writeValueAsString(lightPredictions));
        }

        // Generate a unique folder for storage
        UUID storageHash = UUID.randomUUID();
        Path folder = Paths.get(storagePath, storageHash.toString());
        Files.createDirectories(folder);

        if (aiResult.getFullNormalImage() != null) {
            Path originalPath = folder.resolve("original.jpg");
            Files.write(originalPath, Base64.getDecoder().decode(aiResult.getFullNormalImage()));
            analysis.setOriginalImagePath(originalPath.toString());
        }
        if (aiResult.getFullImage() != null) {
            Path annotatedPath = folder.resolve("annotated.jpg");
            Files.write(annotatedPath, Base64.getDecoder().decode(aiResult.getFullImage()));
            analysis.setAnnotatedImagePath(annotatedPath.toString());
        }
        if (aiResult.getSegmentationImage() != null) {
            Path segPath = folder.resolve("segmentation.jpg");
            Files.write(segPath, Base64.getDecoder().decode(aiResult.getSegmentationImage()));
            analysis.setSegmentationImagePath(segPath.toString());
        }

        // Save crop images for each prediction
        if (aiResult.getIndividualPredictions() != null) {
            for (int i = 0; i < aiResult.getIndividualPredictions().size(); i++) {
                MammogramAnalysisResponseDto.PredictionDto pred = aiResult.getIndividualPredictions().get(i);
                if (pred.getCrop() != null) {
                    Path cropPath = folder.resolve("crop_" + i + ".jpg");
                    Files.write(cropPath, Base64.getDecoder().decode(pred.getCrop()));
                }
                if (pred.getImage() != null) {
                    Path predImgPath = folder.resolve("pred_" + i + ".jpg");
                    Files.write(predImgPath, Base64.getDecoder().decode(pred.getImage()));
                }
            }
        }

        // Save the entity to DB
        analysis = analysisRepository.save(analysis);
        LOG.info("Mammogram analysis saved: id={}, storage={}, patient={}, verdict={}",
                analysis.getId(), storageHash, patientProfileId, analysis.getGlobalVerdict());

        // Add analysisId and patient info to response for frontend
        aiResult.setAnalysisId(analysis.getId().toString());
        aiResult.setPatientFirstName(patient.getUser().getFirstName());
        aiResult.setPatientLastName(patient.getUser().getLastName());
        
        return aiResult;
    }

    /**
     * Generate AI report for an existing analysis using the /conclusion endpoint.
     */
    @Transactional
    public String generateReport(UUID analysisId) {
        MammogramAnalysis analysis = analysisRepository.findByIdWithUser(analysisId)
                .orElseThrow(() -> new NotFoundException("Analyse introuvable: " + analysisId));

        // Build prompt from analysis data
        String prompt = buildReportPrompt(analysis);

        // Call AI /conclusion endpoint
        String report;
        try {
            Map<String, String> requestBody = Map.of("prompt", prompt);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    aiApiUrl + "/conclusion", request, String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                report = root.has("conclusion") ? root.get("conclusion").asText() : "Rapport non disponible.";
            } else {
                report = generateFallbackReport(analysis);
            }
        } catch (Exception e) {
            LOG.warn("Failed to call AI /conclusion endpoint: {}", e.getMessage());
            report = generateFallbackReport(analysis);
        }

        analysis.setAiReport(report);
        analysis.setReportGeneratedAt(Instant.now());
        analysisRepository.save(analysis);

        return report;
    }

    /**
     * Get analysis history for a doctor (all patients).
     */
    @Transactional(readOnly = true)
    public List<MammogramAnalysisHistoryDto> getHistoryByDoctor(UUID doctorProfileId) {
        return analysisRepository.findByDoctorIdWithUser(doctorProfileId)
                .stream().map(this::toHistoryDto).toList();
    }

    /**
     * Get analysis history for a specific patient under a doctor.
     */
    @Transactional(readOnly = true)
    public List<MammogramAnalysisHistoryDto> getHistoryByPatientAndDoctor(UUID patientProfileId, UUID doctorProfileId) {
        return analysisRepository.findByPatientIdAndDoctorIdWithUser(patientProfileId, doctorProfileId)
                .stream().map(this::toHistoryDto).toList();
    }

    /**
     * Get detailed analysis with images reloaded from disk.
     */
    @Transactional(readOnly = true)
    public MammogramAnalysisDetailDto getAnalysisDetail(UUID analysisId) {
        MammogramAnalysis analysis = analysisRepository.findByIdWithUser(analysisId)
                .orElseThrow(() -> new NotFoundException("Analyse introuvable: " + analysisId));
        return toDetailDto(analysis);
    }

    // ═══════════ PRIVATE HELPERS ═══════════

    private MammogramAnalysisResponseDto callAiPredict(MultipartFile file, String pixelSpacing) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        body.add("file", fileResource);
        body.add("pixel_spacing", pixelSpacing != null ? pixelSpacing : "0.1");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                aiApiUrl + "/predict", HttpMethod.POST, requestEntity, String.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("AI service returned error: " + response.getStatusCode());
        }

        return parseAiResponse(response.getBody());
    }

    private MammogramAnalysisResponseDto parseAiResponse(String rawJson) throws IOException {
        ObjectMapper safeMapper = objectMapper.copy();
        safeMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);

        JsonNode root = safeMapper.readTree(rawJson);

        MammogramAnalysisResponseDto dto = new MammogramAnalysisResponseDto();
        boolean hasDetections = root.has("detections") && root.get("detections").asBoolean(false);
        dto.setDetections(hasDetections);

        dto.setFullImage(getTextOrNull(root, "full_image"));
        dto.setFullNormalImage(getTextOrNull(root, "full_Normal_image"));
        dto.setSegmentationImage(getTextOrNull(root, "segmentation_image"));

        List<MammogramAnalysisResponseDto.PredictionDto> predictions = new ArrayList<>();
        if (hasDetections && root.has("individual_predictions")) {
            for (JsonNode predNode : root.get("individual_predictions")) {
                MammogramAnalysisResponseDto.PredictionDto pred = new MammogramAnalysisResponseDto.PredictionDto();
                pred.setImage(getTextOrNull(predNode, "image"));
                pred.setCrop(getTextOrNull(predNode, "crop"));
                pred.setLabel(getTextOrNull(predNode, "label"));
                pred.setClassification(getTextOrNull(predNode, "classification"));
                pred.setScore(predNode.has("score") ? predNode.get("score").asDouble() : 0.0);

                if (predNode.has("features")) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> features = safeMapper.convertValue(predNode.get("features"), Map.class);
                        sanitizeMap(features);
                        pred.setFeatures(features);
                    } catch (Exception e) {
                        pred.setFeatures(new HashMap<>());
                    }
                }
                predictions.add(pred);
            }
        }
        dto.setIndividualPredictions(predictions);

        if (!hasDetections || predictions.isEmpty()) {
            dto.setGlobalConfidence(1.0);
            dto.setGlobalVerdict("Normal");
        } else {
            double avgScore = predictions.stream()
                    .mapToDouble(MammogramAnalysisResponseDto.PredictionDto::getScore)
                    .average().orElse(0.0);
            dto.setGlobalConfidence(avgScore);

            boolean hasMalignant = predictions.stream()
                    .anyMatch(p -> "Malignant".equalsIgnoreCase(p.getClassification()));
            boolean hasBenign = predictions.stream()
                    .anyMatch(p -> "Benign".equalsIgnoreCase(p.getClassification()));

            if (hasMalignant && hasBenign) dto.setGlobalVerdict("Mixte");
            else if (hasMalignant) dto.setGlobalVerdict("Malin");
            else if (hasBenign) dto.setGlobalVerdict("Bénin");
            else dto.setGlobalVerdict("Indéterminé");
        }

        return dto;
    }

    private String buildReportPrompt(MammogramAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a medical AI assistant specializing in breast cancer mammography analysis. ");
        sb.append("Generate a detailed radiology report in French based on the following AI analysis results.\n\n");

        sb.append("--- PATIENT INFO ---\n");
        sb.append("Nom: ").append(analysis.getPatient().getUser().getFirstName())
          .append(" ").append(analysis.getPatient().getUser().getLastName()).append("\n");
        sb.append("Date d'analyse: ").append(analysis.getAnalysisDate()).append("\n");
        sb.append("--- AI RESULTS ---\n");
        sb.append("Verdict global: ").append(analysis.getGlobalVerdict()).append("\n");
        sb.append("Indice de confiance: ").append(String.format("%.1f%%", analysis.getGlobalConfidence() * 100)).append("\n");
        sb.append("Detections: ").append(analysis.getDetectionsCount()).append("\n\n");

        if (analysis.getPredictionsJson() != null && !analysis.getPredictionsJson().isBlank()) {
            sb.append("Détails des détections:\n");
            try {
                List<Map<String, Object>> predictions = objectMapper.readValue(
                        analysis.getPredictionsJson(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                for (int i = 0; i < predictions.size(); i++) {
                    Map<String, Object> pred = predictions.get(i);
                    sb.append("  Détection ").append(i + 1).append(": ");
                    sb.append("Type=").append(pred.get("label"));
                    sb.append(", Classification=").append(pred.get("classification"));
                    sb.append(", Score=").append(pred.get("score")).append("\n");

                    if (pred.containsKey("features") && pred.get("features") != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> features = (Map<String, Object>) pred.get("features");
                        sb.append("    Caractéristiques: ").append(features).append("\n");
                    }
                }
            } catch (Exception e) {
                sb.append("  [Détails non disponibles]\n");
            }
        }

        sb.append("\nPlease generate:\n");
        sb.append("1. RÉSUMÉ — A summary paragraph\n");
        sb.append("2. OBSERVATIONS — Detailed findings for each detection\n");
        sb.append("3. CLASSIFICATION BI-RADS — A BI-RADS assessment\n");
        sb.append("4. RECOMMANDATIONS — Clinical recommendations\n");
        sb.append("5. CONCLUSION — Final conclusion\n");
        sb.append("\nThe report MUST be in French. Be precise and clinically relevant.");

        return sb.toString();
    }

    private String generateFallbackReport(MammogramAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("# RAPPORT D'ANALYSE MAMMOGRAPHIQUE\n");
        sb.append("*Généré par SafeScan AI (Algorithme de Détection Local)*\n\n");

        sb.append("## 1. SYNTHÈSE CLINIQUE\n");
        sb.append("L'analyse automatisée de la dose radiographique a été effectuée à l'aide d'architectures de réseaux de neurones convolutionnels (Mask R-CNN avec backbone ConvNeXt).\n\n");
        
        sb.append("> **VERDICT FINAL : ").append(analysis.getGlobalVerdict().toUpperCase()).append("**\n");
        sb.append("> **INDICE DE CONFIANCE : ").append(String.format("%.1f%%", analysis.getGlobalConfidence() * 100)).append("**\n\n");

        sb.append("## 2. DÉTAILS DE L'INSPECTION\n");
        sb.append("| Type | Classification | Confiance | Statut |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");

        if (analysis.getPredictionsJson() != null) {
            try {
                List<Map<String, Object>> predictions = objectMapper.readValue(
                        analysis.getPredictionsJson(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                for (Map<String, Object> pred : predictions) {
                    String label = String.valueOf(pred.get("label"));
                    String type = "mass".equals(label) ? "Masse" : "Calcification";
                    String classif = String.valueOf(pred.get("classification"));
                    double conf = ((Number) pred.get("score")).doubleValue() * 100;
                    String icon = "Malin".equalsIgnoreCase(classif) ? "🔴 Suspect" : "🟡 À surveiller";
                    if ("Normal".equalsIgnoreCase(classif)) icon = "🟢 Bénin";
                    
                    sb.append("| ").append(type).append(" | ").append(classif).append(" | ")
                      .append(String.format("%.0f%%", conf)).append(" | ").append(icon).append(" |\n");
                }
            } catch (Exception e) {
                sb.append("| Info indisponible | - | - | - |\n");
            }
        } else {
            sb.append("| Aucune anomalie | - | 100% | Stable |\n");
        }

        sb.append("\n## 3. RECOMMANDATIONS MÉDICALES\n");
        switch (analysis.getGlobalVerdict()) {
            case "Normal":
                sb.append("✅ **Examen normal.** Poursuivre le dépistage standard selon l'âge de la patiente.\n");
                break;
            case "Bénin":
                sb.append("ℹ️ **Findings bénins.** Suivi radiologique à 6 ou 12 mois pour vérifier la stabilité.\n");
                break;
            case "Malin":
                sb.append("⚠️ **Suspicion de malignité.** Une confrontation anatomo-pathologique (Biopsie) est impérative dans les plus brefs délais.\n");
                break;
            case "Mixte":
                sb.append("🔍 **Résultats complexes.** Biopsie recommandée pour les zones les plus suspectes. Évaluation clinique approfondie.\n");
                break;
            default:
                sb.append("Veuillez consulter un spécialiste pour une évaluation manuelle approfondie.\n");
        }

        sb.append("\n---\n*Ce document est une aide à la décision clinique générée par IA. Il ne remplace pas la validation finale par un radiologue agréé.*");

        return sb.toString();
    }

    private MammogramAnalysisHistoryDto toHistoryDto(MammogramAnalysis analysis) {
        MammogramAnalysisHistoryDto dto = new MammogramAnalysisHistoryDto();
        dto.setId(analysis.getId());
        dto.setPatientProfileId(analysis.getPatient().getId());
        dto.setPatientFirstName(analysis.getPatient().getUser().getFirstName());
        dto.setPatientLastName(analysis.getPatient().getUser().getLastName());
        dto.setAnalysisDate(analysis.getAnalysisDate());
        dto.setGlobalVerdict(analysis.getGlobalVerdict());
        dto.setGlobalConfidence(analysis.getGlobalConfidence() != null ? analysis.getGlobalConfidence() : 0.0);
        dto.setDetectionsCount(analysis.getDetectionsCount() != null ? analysis.getDetectionsCount() : 0);
        dto.setHasReport(analysis.getAiReport() != null && !analysis.getAiReport().isBlank());
        return dto;
    }

    private MammogramAnalysisDetailDto toDetailDto(MammogramAnalysis analysis) {
        MammogramAnalysisDetailDto dto = new MammogramAnalysisDetailDto();
        dto.setId(analysis.getId());
        dto.setPatientProfileId(analysis.getPatient().getId());
        dto.setPatientFirstName(analysis.getPatient().getUser().getFirstName());
        dto.setPatientLastName(analysis.getPatient().getUser().getLastName());
        dto.setDoctorProfileId(analysis.getDoctor().getId());
        dto.setAnalysisDate(analysis.getAnalysisDate());
        dto.setGlobalVerdict(analysis.getGlobalVerdict());
        dto.setGlobalConfidence(analysis.getGlobalConfidence() != null ? analysis.getGlobalConfidence() : 0.0);
        dto.setDetectionsCount(analysis.getDetectionsCount() != null ? analysis.getDetectionsCount() : 0);

        // Reload images from disk
        dto.setFullNormalImage(loadImageAsBase64(analysis.getOriginalImagePath()));
        dto.setFullImage(loadImageAsBase64(analysis.getAnnotatedImagePath()));
        dto.setSegmentationImage(loadImageAsBase64(analysis.getSegmentationImagePath()));

        // Parse predictions JSON and reload crop images
        if (analysis.getPredictionsJson() != null) {
            try {
                ObjectMapper safeMapper = objectMapper.copy();
                safeMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);

                List<Map<String, Object>> rawPredictions = safeMapper.readValue(
                        analysis.getPredictionsJson(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                List<MammogramAnalysisResponseDto.PredictionDto> predictions = new ArrayList<>();
                Path folder = Paths.get(storagePath, analysis.getId().toString());

                for (int i = 0; i < rawPredictions.size(); i++) {
                    Map<String, Object> raw = rawPredictions.get(i);
                    MammogramAnalysisResponseDto.PredictionDto pred = new MammogramAnalysisResponseDto.PredictionDto();
                    pred.setLabel(raw.get("label") != null ? raw.get("label").toString() : null);
                    pred.setClassification(raw.get("classification") != null ? raw.get("classification").toString() : null);
                    pred.setScore(raw.get("score") != null ? ((Number) raw.get("score")).doubleValue() : 0.0);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> features = raw.get("features") instanceof Map ?
                            (Map<String, Object>) raw.get("features") : new HashMap<>();
                    pred.setFeatures(features);

                    // Load crop and prediction images from disk
                    Path cropPath = folder.resolve("crop_" + i + ".jpg");
                    pred.setCrop(loadImageAsBase64(cropPath.toString()));
                    Path predImgPath = folder.resolve("pred_" + i + ".jpg");
                    pred.setImage(loadImageAsBase64(predImgPath.toString()));

                    predictions.add(pred);
                }
                dto.setIndividualPredictions(predictions);
            } catch (Exception e) {
                LOG.warn("Failed to parse predictions JSON for analysis {}: {}", analysis.getId(), e.getMessage());
                dto.setIndividualPredictions(new ArrayList<>());
            }
        }

        dto.setAiReport(analysis.getAiReport());
        dto.setReportGeneratedAt(analysis.getReportGeneratedAt());

        return dto;
    }

    private String loadImageAsBase64(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            Path filePath = Paths.get(path);
            if (Files.exists(filePath)) {
                byte[] bytes = Files.readAllBytes(filePath);
                return Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            LOG.warn("Failed to load image from disk: {}", path);
        }
        return null;
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    @SuppressWarnings("unchecked")
    private void sanitizeMap(Map<String, Object> map) {
        if (map == null) return;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                sanitizeMap((Map<String, Object>) value);
            } else if (value instanceof Double) {
                Double d = (Double) value;
                if (d.isNaN() || d.isInfinite()) entry.setValue(null);
            } else if (value instanceof Float) {
                Float f = (Float) value;
                if (f.isNaN() || f.isInfinite()) entry.setValue(null);
            }
        }
    }
}
