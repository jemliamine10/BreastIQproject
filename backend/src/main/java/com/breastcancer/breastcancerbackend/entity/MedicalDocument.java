package com.breastcancer.breastcancerbackend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "medical_documents",
        indexes = {
                @Index(name = "idx_doc_patient", columnList = "patient_id"),
                @Index(name = "idx_doc_doctor", columnList = "uploaded_by_doctor_id"),
                @Index(name = "idx_doc_category", columnList = "category"),
                @Index(name = "idx_doc_deleted", columnList = "deleted")
        }
)
public class MedicalDocument {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private DocumentCategory category;

    @Column(name = "upload_date", nullable = false)
    private LocalDate uploadDate;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "page_count")
    private Integer pageCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "visible_to_patient", nullable = false)
    private boolean visibleToPatient = true;

    @Column(name = "visible_to_doctor", nullable = false)
    private boolean visibleToDoctor = true;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // ===== Relations =====

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_doc_patient"))
    private PatientProfile patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_doctor_id",
            foreignKey = @ForeignKey(name = "fk_doc_doctor"))
    private DoctorProfile uploadedByDoctor;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // ===== Getters & Setters =====

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DocumentCategory getCategory() { return category; }
    public void setCategory(DocumentCategory category) { this.category = category; }

    public LocalDate getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDate uploadDate) { this.uploadDate = uploadDate; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public boolean isVisibleToPatient() { return visibleToPatient; }
    public void setVisibleToPatient(boolean visibleToPatient) { this.visibleToPatient = visibleToPatient; }

    public boolean isVisibleToDoctor() { return visibleToDoctor; }
    public void setVisibleToDoctor(boolean visibleToDoctor) { this.visibleToDoctor = visibleToDoctor; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public PatientProfile getPatient() { return patient; }
    public void setPatient(PatientProfile patient) { this.patient = patient; }

    public DoctorProfile getUploadedByDoctor() { return uploadedByDoctor; }
    public void setUploadedByDoctor(DoctorProfile uploadedByDoctor) { this.uploadedByDoctor = uploadedByDoctor; }
}
