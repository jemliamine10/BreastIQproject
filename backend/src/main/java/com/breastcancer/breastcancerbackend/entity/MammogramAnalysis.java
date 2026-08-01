package com.breastcancer.breastcancerbackend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "mammogram_analyses",
        indexes = {
                @Index(name = "idx_mammo_patient", columnList = "patient_id"),
                @Index(name = "idx_mammo_doctor", columnList = "doctor_id"),
                @Index(name = "idx_mammo_date", columnList = "analysis_date")
        }
)
public class MammogramAnalysis {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mammo_patient"))
    private PatientProfile patient;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_mammo_doctor"))
    private DoctorProfile doctor;

    @Column(name = "analysis_date", nullable = false)
    private Instant analysisDate = Instant.now();

    /** "Normal", "Bénin", "Malin", "Mixte" */
    @Column(name = "global_verdict", length = 30)
    private String globalVerdict;

    /** 0.0 – 1.0 */
    @Column(name = "global_confidence")
    private Double globalConfidence;

    @Column(name = "detections_count")
    private Integer detectionsCount = 0;

    /** Serialized JSON of individual predictions (without base64 images) */
    @Lob
    @Column(name = "predictions_json", columnDefinition = "TEXT")
    private String predictionsJson;

    // ── Image paths on disk ──

    @Column(name = "original_image_path", length = 500)
    private String originalImagePath;

    @Column(name = "annotated_image_path", length = 500)
    private String annotatedImagePath;

    @Column(name = "segmentation_image_path", length = 500)
    private String segmentationImagePath;

    // ── AI Report ──

    @Lob
    @Column(name = "ai_report", columnDefinition = "TEXT")
    private String aiReport;

    @Column(name = "report_generated_at")
    private Instant reportGeneratedAt;

    // ===== Getters & Setters =====

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PatientProfile getPatient() { return patient; }
    public void setPatient(PatientProfile patient) { this.patient = patient; }

    public DoctorProfile getDoctor() { return doctor; }
    public void setDoctor(DoctorProfile doctor) { this.doctor = doctor; }

    public Instant getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(Instant analysisDate) { this.analysisDate = analysisDate; }

    public String getGlobalVerdict() { return globalVerdict; }
    public void setGlobalVerdict(String globalVerdict) { this.globalVerdict = globalVerdict; }

    public Double getGlobalConfidence() { return globalConfidence; }
    public void setGlobalConfidence(Double globalConfidence) { this.globalConfidence = globalConfidence; }

    public Integer getDetectionsCount() { return detectionsCount; }
    public void setDetectionsCount(Integer detectionsCount) { this.detectionsCount = detectionsCount; }

    public String getPredictionsJson() { return predictionsJson; }
    public void setPredictionsJson(String predictionsJson) { this.predictionsJson = predictionsJson; }

    public String getOriginalImagePath() { return originalImagePath; }
    public void setOriginalImagePath(String originalImagePath) { this.originalImagePath = originalImagePath; }

    public String getAnnotatedImagePath() { return annotatedImagePath; }
    public void setAnnotatedImagePath(String annotatedImagePath) { this.annotatedImagePath = annotatedImagePath; }

    public String getSegmentationImagePath() { return segmentationImagePath; }
    public void setSegmentationImagePath(String segmentationImagePath) { this.segmentationImagePath = segmentationImagePath; }

    public String getAiReport() { return aiReport; }
    public void setAiReport(String aiReport) { this.aiReport = aiReport; }

    public Instant getReportGeneratedAt() { return reportGeneratedAt; }
    public void setReportGeneratedAt(Instant reportGeneratedAt) { this.reportGeneratedAt = reportGeneratedAt; }
}
