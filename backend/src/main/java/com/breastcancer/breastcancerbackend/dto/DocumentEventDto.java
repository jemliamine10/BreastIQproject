package com.breastcancer.breastcancerbackend.dto;

/**
 * DTO pour les événements WebSocket temps réel.
 *
 * Exemple:
 * { "type": "DOCUMENT_ADDED", "document": { ...DocumentResponseDto } }
 */
public class DocumentEventDto {

    private String type;  // DOCUMENT_ADDED, DOCUMENT_DELETED, DOCUMENT_UPDATED
    private DocumentResponseDto document;

    public DocumentEventDto() {}

    public DocumentEventDto(String type, DocumentResponseDto document) {
        this.type = type;
        this.document = document;
    }

    // ===== Getters & Setters =====

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public DocumentResponseDto getDocument() { return document; }
    public void setDocument(DocumentResponseDto document) { this.document = document; }
}
