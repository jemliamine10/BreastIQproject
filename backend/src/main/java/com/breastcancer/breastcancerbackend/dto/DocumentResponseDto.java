package com.breastcancer.breastcancerbackend.dto;

/**
 * DTO de réponse pour les documents médicaux.
 * ⚠️ Format STRICT — correspond EXACTEMENT au component frontend Angular.
 *
 * Exemple JSON:
 * {
 *   "id": "a1b2c3...",
 *   "name": "Bilan sanguin",
 *   "category": "bilan",
 *   "date": "2026-03-24",
 *   "doctor": "Dr. Jean Dupont",
 *   "size": "1.2 Mo",
 *   "pages": 3,
 *   "status": "validated"
 * }
 */
public class DocumentResponseDto {

    private String id;
    private String name;
    private String category;   // slug exact: compte-rendu, ordonnance, bilan, imagerie, autre
    private String date;       // yyyy-MM-dd
    private String doctor;     // nom complet du médecin
    private String size;       // ex: "1.2 Mo", "450 Ko"
    private Integer pages;
    private String status;     // lowercase: validated, pending, archived
    private String uploadedBy; // "patient" or "doctor"

    public DocumentResponseDto() {}

    // ===== Getters & Setters =====

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDoctor() { return doctor; }
    public void setDoctor(String doctor) { this.doctor = doctor; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public Integer getPages() { return pages; }
    public void setPages(Integer pages) { this.pages = pages; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}
