package com.breastcancer.breastcancerbackend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "clinical_data",
        indexes = {
                @Index(name = "idx_clindata_medrec", columnList = "medical_record_id")
        }
)
public class ClinicalData {

    public enum ReceptorStatus {
        POSITIVE,
        NEGATIVE,
        UNKNOWN
    }

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_record_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_clindata_medrec"))
    private MedicalRecord medicalRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "estrogen_receptor", length = 20)
    private ReceptorStatus estrogenReceptor;

    @Enumerated(EnumType.STRING)
    @Column(name = "progesterone_receptor", length = 20)
    private ReceptorStatus progesteroneReceptor;

    @Enumerated(EnumType.STRING)
    @Column(name = "her2_status", length = 20)
    private ReceptorStatus her2Status;

    @Column(name = "ki67")
    private Double ki67; // percentage 0-100

    @Column(name = "tumor_size_mm")
    private Double tumorSize; // in mm

    @Column(name = "lymph_nodes_involved")
    private Integer lymphNodesInvolved;

    @Column(name = "metastasis", nullable = false)
    private boolean metastasis = false;

    @Column(name = "grade")
    private Integer grade; // 1, 2, or 3

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    // ===== Getters & Setters =====
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public MedicalRecord getMedicalRecord() { return medicalRecord; }
    public void setMedicalRecord(MedicalRecord medicalRecord) { this.medicalRecord = medicalRecord; }

    public ReceptorStatus getEstrogenReceptor() { return estrogenReceptor; }
    public void setEstrogenReceptor(ReceptorStatus estrogenReceptor) { this.estrogenReceptor = estrogenReceptor; }

    public ReceptorStatus getProgesteroneReceptor() { return progesteroneReceptor; }
    public void setProgesteroneReceptor(ReceptorStatus progesteroneReceptor) { this.progesteroneReceptor = progesteroneReceptor; }

    public ReceptorStatus getHer2Status() { return her2Status; }
    public void setHer2Status(ReceptorStatus her2Status) { this.her2Status = her2Status; }

    public Double getKi67() { return ki67; }
    public void setKi67(Double ki67) { this.ki67 = ki67; }

    public Double getTumorSize() { return tumorSize; }
    public void setTumorSize(Double tumorSize) { this.tumorSize = tumorSize; }

    public Integer getLymphNodesInvolved() { return lymphNodesInvolved; }
    public void setLymphNodesInvolved(Integer lymphNodesInvolved) { this.lymphNodesInvolved = lymphNodesInvolved; }

    public boolean isMetastasis() { return metastasis; }
    public void setMetastasis(boolean metastasis) { this.metastasis = metastasis; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
