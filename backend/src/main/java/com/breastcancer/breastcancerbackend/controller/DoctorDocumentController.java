package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.dto.DocumentResponseDto;
import com.breastcancer.breastcancerbackend.dto.DocumentUploadDto;
import com.breastcancer.breastcancerbackend.service.MedicalDocumentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoints documents côté MÉDECIN.
 * Toutes les opérations nécessitent un lien actif patient ↔ médecin.
 */
@RestController
@RequestMapping("/api/v1/doctor")
public class DoctorDocumentController {

    private final MedicalDocumentService documentService;

    public DoctorDocumentController(MedicalDocumentService documentService) {
        this.documentService = documentService;
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /api/v1/doctor/{doctorId}/patients/{patientId}/documents
    //  → Liste des documents d'un patient (côté médecin)
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{doctorId}/patients/{patientId}/documents")
    public Page<DocumentResponseDto> getPatientDocuments(
            @PathVariable UUID doctorId,
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return documentService.getPatientDocumentsForDoctor(doctorId, patientId, pageable);
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /api/v1/doctor/{doctorId}/patients/{patientId}/documents/counts
    //  → Nombre de documents par catégorie (côté médecin)
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{doctorId}/patients/{patientId}/documents/counts")
    public Map<String, Long> countByCategory(
            @PathVariable UUID doctorId,
            @PathVariable UUID patientId
    ) {
        return documentService.countByCategoryForDoctor(doctorId, patientId);
    }

    // ─────────────────────────────────────────────────────────────────
    //  POST /api/v1/doctor/{doctorId}/patients/{patientId}/documents/upload
    //  → Upload d'un document pour un patient (par le médecin)
    // ─────────────────────────────────────────────────────────────────
    @PostMapping("/{doctorId}/patients/{patientId}/documents/upload")
    public ResponseEntity<DocumentResponseDto> uploadForPatient(
            @PathVariable UUID doctorId,
            @PathVariable UUID patientId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") @Valid DocumentUploadDto metadata
    ) {
        DocumentResponseDto dto = documentService.uploadDocument(patientId, doctorId, file, metadata);
        return ResponseEntity.status(201).body(dto);
    }

    // ─────────────────────────────────────────────────────────────────
    //  PATCH /api/v1/doctor/{doctorId}/documents/{id}/status
    //  → Modifier le statut d'un document (médecin uniquement)
    // ─────────────────────────────────────────────────────────────────
    @PatchMapping("/{doctorId}/documents/{id}/status")
    public DocumentResponseDto updateStatus(
            @PathVariable UUID doctorId,
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Le champ 'status' est requis.");
        }
        return documentService.updateStatus(id, newStatus, doctorId);
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /api/v1/doctor/{doctorId}/documents/{id}/download
    //  → Télécharger un document (stream sécurisé)
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{doctorId}/documents/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadDocument(
            @PathVariable UUID doctorId,
            @PathVariable UUID id) {
        com.breastcancer.breastcancerbackend.entity.MedicalDocument doc = documentService.getDocumentEntity(id);
        org.springframework.core.io.Resource resource = documentService.downloadDocument(id);

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(doc.getContentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getName() + "\"")
                .body(resource);
    }

    // ─────────────────────────────────────────────────────────────────
    //  DELETE /api/v1/doctor/{doctorId}/documents/{id}
    //  → Soft delete d'un document
    // ─────────────────────────────────────────────────────────────────
    @DeleteMapping("/{doctorId}/documents/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID doctorId,
            @PathVariable UUID id
    ) {
        documentService.deleteDocument(id, doctorId);
        return ResponseEntity.noContent().build();
    }
}
