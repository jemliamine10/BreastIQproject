package com.breastcancer.breastcancerbackend.controller;

import com.breastcancer.breastcancerbackend.entity.MedicalDocument;
import com.breastcancer.breastcancerbackend.service.MedicalDocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints partagés (Patient & Médecin) pour les documents médicaux.
 */
@RestController
@RequestMapping("/api/v1/documents")
public class SharedDocumentController {

    private final MedicalDocumentService documentService;

    public SharedDocumentController(MedicalDocumentService documentService) {
        this.documentService = documentService;
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /api/v1/documents/{id}/download
    //  → Télécharger un document (stream sécurisé)
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{id}/download")
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
    //  DELETE /api/v1/documents/{id}?requesterId=...
    //  → Soft delete d'un document
    // ─────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID id,
            @RequestParam UUID requesterId
    ) {
        documentService.deleteDocument(id, requesterId);
        return ResponseEntity.noContent().build();
    }
}
