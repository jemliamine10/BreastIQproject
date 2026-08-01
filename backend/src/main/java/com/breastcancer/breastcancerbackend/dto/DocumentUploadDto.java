package com.breastcancer.breastcancerbackend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour les métadonnées d'upload de document.
 * Envoyé en tant que partie JSON dans une requête multipart.
 */
public class DocumentUploadDto {

    @NotBlank(message = "Le nom du document est requis")
    private String name;

    @NotBlank(message = "La catégorie est requise")
    private String category;  // slug: compte-rendu, ordonnance, bilan, imagerie, autre

    private Integer pageCount;

    public DocumentUploadDto() {}

    // ===== Getters & Setters =====

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
}
