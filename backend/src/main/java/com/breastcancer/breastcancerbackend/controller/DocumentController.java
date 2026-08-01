package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.DocumentResponseDto;
import com.breastcancer.breastcancerbackend.dto.DocumentUploadDto;
import com.breastcancer.breastcancerbackend.entity.MedicalDocument;
import com.breastcancer.breastcancerbackend.service.MedicalDocumentService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoints documents côté PATIENT.
 */
@RestController
@RequestMapping("/api/v1/patient")
public class DocumentController {

    private final MedicalDocumentService documentService;

    public DocumentController(MedicalDocumentService documentService) {
        this.documentService = documentService;
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /api/v1/patient/{patientId}/documents?page=0&size=20
    //  → Liste paginée des documents du patient
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{patientId}/documents")
    public Page<DocumentResponseDto> getDocuments(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return documentService.getPatientDocuments(patientId, pageable);
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /api/v1/patient/{patientId}/documents/counts
    //  → Nombre de documents par catégorie
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{patientId}/documents/counts")
    public Map<String, Long> countByCategory(@PathVariable UUID patientId) {
        return documentService.countByCategory(patientId);
    }

    // ─────────────────────────────────────────────────────────────────
    //  POST /api/v1/patient/{patientId}/documents/upload
    //  → Upload un document (multipart: file + métadonnées)
    // ─────────────────────────────────────────────────────────────────
    @PostMapping("/{patientId}/documents/upload")
    public ResponseEntity<DocumentResponseDto> uploadDocument(
            @PathVariable UUID patientId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") @Valid DocumentUploadDto metadata
    ) {
        DocumentResponseDto dto = documentService.uploadDocument(patientId, null, file, metadata);
        return ResponseEntity.status(201).body(dto);
    }

    // ─────────────────────────────────────────────────────────────────
    //  POST /api/v1/patient/{patientId}/documents/{id}/share?doctorId=...
    //  → Partager un document du patient avec un médecin lié (lien ACTIVE requis)
    // ─────────────────────────────────────────────────────────────────
    @PostMapping("/{patientId}/documents/{id}/share")
    public DocumentResponseDto shareDocument(
            @PathVariable UUID patientId,
            @PathVariable UUID id,
            @RequestParam UUID doctorId
    ) {
        return documentService.shareDocumentWithDoctor(patientId, id, doctorId);
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /api/v1/patient/documents/{id}/download
    //  → Télécharger un document (stream sécurisé)
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID id) {
        MedicalDocument doc = documentService.getDocumentEntity(id);
        Resource resource = documentService.downloadDocument(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getName() + "\"")
                .body(resource);
    }

    // ─────────────────────────────────────────────────────────────────
    //  DELETE /api/v1/patient/documents/{id}?requesterId=...
    //  → Soft delete d'un document
    // ─────────────────────────────────────────────────────────────────
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID id,
            @RequestParam UUID requesterId
    ) {
        documentService.deleteDocument(id, requesterId);
        return ResponseEntity.noContent().build();
    }
}
