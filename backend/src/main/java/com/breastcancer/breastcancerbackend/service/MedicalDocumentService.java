package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.DocumentEventDto;
import com.breastcancer.breastcancerbackend.dto.DocumentResponseDto;
import com.breastcancer.breastcancerbackend.dto.DocumentUploadDto;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.repository.DoctorProfileRepository;
import com.breastcancer.breastcancerbackend.repository.MedicalDocumentRepository;
import com.breastcancer.breastcancerbackend.repository.PatientProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

@Service
public class MedicalDocumentService {

    private static final Logger LOG = LoggerFactory.getLogger(MedicalDocumentService.class);

    // Types MIME autorisés
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    // Taille max: 20 Mo
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;

    private final MedicalDocumentRepository documentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final StorageService storageService;
    private final DoctorPatientLinkGuardService linkGuard;
    private final SimpMessagingTemplate messagingTemplate;

    public MedicalDocumentService(MedicalDocumentRepository documentRepository,
                                  PatientProfileRepository patientProfileRepository,
                                  DoctorProfileRepository doctorProfileRepository,
                                  StorageService storageService,
                                  DoctorPatientLinkGuardService linkGuard,
                                  SimpMessagingTemplate messagingTemplate) {
        this.documentRepository = documentRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.storageService = storageService;
        this.linkGuard = linkGuard;
        this.messagingTemplate = messagingTemplate;
    }

    // ===================================================================
    //  1. UPLOAD DOCUMENT
    // ===================================================================
    @Transactional
    public DocumentResponseDto uploadDocument(UUID patientId,
                                              UUID doctorId,
                                              MultipartFile file,
                                              DocumentUploadDto dto) {
        // Validation fichier
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Aucun fichier fourni.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Taille du fichier dépasse la limite de 20 Mo.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Type de fichier non autorisé: " + contentType);
        }

        // Résoudre patient
        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));

        // Résoudre docteur (optionnel — si upload par patient, doctorId peut être null)
        DoctorProfile doctor = null;
        if (doctorId != null) {
            doctor = doctorProfileRepository.findById(doctorId)
                    .orElseThrow(() -> new NotFoundException("Médecin introuvable."));
            // Vérifier lien actif
            linkGuard.assertActiveLink(patientId, doctorId);
        } else {
            // Même côté patient, un document n'est autorisé que s'il existe au moins un lien actif.
            linkGuard.assertPatientHasAtLeastOneActiveLink(patientId);
        }

        // Résoudre catégorie depuis slug
        DocumentCategory category = DocumentCategory.fromSlug(dto.getCategory());

        // Générer clé de stockage unique
        String storageKey = UUID.randomUUID().toString() + "_" + sanitizeFilename(file.getOriginalFilename());

        // Stocker le fichier
        try {
            storageService.store(storageKey, file.getInputStream(), contentType);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier uploadé.", e);
        }

        // Créer l'entité
        MedicalDocument doc = new MedicalDocument();
        doc.setName(dto.getName());
        doc.setCategory(category);
        doc.setUploadDate(LocalDate.now());
        doc.setStorageKey(storageKey);
        doc.setContentType(contentType);
        doc.setFileSize(file.getSize());
        doc.setPageCount(dto.getPageCount());
        doc.setStatus(DocumentStatus.PENDING);
        doc.setPatient(patient);
        doc.setUploadedByDoctor(doctor);
        doc.setVisibleToPatient(true);
        // Upload patient -> partage explicite requis pour visibilité médecin.
        doc.setVisibleToDoctor(doctorId != null);

        doc = documentRepository.save(doc);
        LOG.info("Document uploadé: id={}, name={}, patient={}", doc.getId(), doc.getName(), patientId);

        DocumentResponseDto responseDto = toDto(doc);

        // WebSocket: notifier le patient
        sendEvent(patientId, "DOCUMENT_ADDED", responseDto);

        return responseDto;
    }

    // ===================================================================
    //  2. GET PATIENT DOCUMENTS (paginé)
    // ===================================================================
    @Transactional(readOnly = true)
    public Page<DocumentResponseDto> getPatientDocuments(UUID patientId, Pageable pageable) {
        // Vérifier que le patient existe
        if (!patientProfileRepository.existsById(patientId)) {
            throw new NotFoundException("Patient introuvable.");
        }
        linkGuard.assertPatientHasAtLeastOneActiveLink(patientId);
        return documentRepository
                .findByPatient_IdAndDeletedFalseOrderByUploadDateDesc(patientId, pageable)
                .map(this::toDto);
    }

    // ===================================================================
    //  3. GET PATIENT DOCUMENTS FOR DOCTOR (avec vérification lien)
    // ===================================================================
    @Transactional(readOnly = true)
    public Page<DocumentResponseDto> getPatientDocumentsForDoctor(UUID doctorId,
                                                                  UUID patientId,
                                                                  Pageable pageable) {
        linkGuard.assertActiveLink(patientId, doctorId);
        return documentRepository
                .findByPatient_IdAndDeletedFalseAndVisibleToDoctorTrueOrderByUploadDateDesc(patientId, pageable)
                .map(this::toDto);
    }

    // ===================================================================
    //  3bis. SHARE DOCUMENT (patient -> médecin)
    // ===================================================================
    @Transactional
    public DocumentResponseDto shareDocumentWithDoctor(UUID patientId, UUID documentId, UUID doctorId) {
        linkGuard.assertActiveLink(patientId, doctorId);

        MedicalDocument doc = documentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new NotFoundException("Document introuvable."));

        if (!doc.getPatient().getId().equals(patientId)) {
            throw new ForbiddenException("Ce document n'appartient pas à cette patiente.");
        }

        // Vérifie que le médecin ciblé existe (ou est mappable depuis userId).
        linkGuard.resolveDoctorProfileId(doctorId);

        if (!doc.isVisibleToDoctor()) {
            doc.setVisibleToDoctor(true);
            doc = documentRepository.save(doc);
        }

        LOG.info("Document partagé: documentId={}, patientId={}, doctorId={}", documentId, patientId, doctorId);

        DocumentResponseDto responseDto = toDto(doc);
        sendEvent(patientId, "DOCUMENT_SHARED", responseDto);
        sendEventToDoctor(doctorId, "DOCUMENT_SHARED", responseDto);
        return responseDto;
    }

    // ===================================================================
    //  4. DOWNLOAD DOCUMENT
    // ===================================================================
    @Transactional(readOnly = true)
    public Resource downloadDocument(UUID documentId) {
        MedicalDocument doc = documentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new NotFoundException("Document introuvable."));
        linkGuard.assertPatientHasAtLeastOneActiveLink(doc.getPatient().getId());
        return storageService.load(doc.getStorageKey());
    }

    /**
     * Récupérer l'entité pour les headers HTTP (contentType, nom).
     */
    @Transactional(readOnly = true)
    public MedicalDocument getDocumentEntity(UUID documentId) {
        MedicalDocument doc = documentRepository.findByIdAndDeletedFalse(documentId)
            .orElseThrow(() -> new NotFoundException("Document introuvable."));
        linkGuard.assertPatientHasAtLeastOneActiveLink(doc.getPatient().getId());
        return doc;
    }

    // ===================================================================
    //  5. DELETE DOCUMENT (soft delete)
    // ===================================================================
    @Transactional
    public void deleteDocument(UUID documentId, UUID requesterId) {
        MedicalDocument doc = documentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new NotFoundException("Document introuvable."));

        linkGuard.assertPatientHasAtLeastOneActiveLink(doc.getPatient().getId());

        doc.setDeleted(true);
        documentRepository.save(doc);

        LOG.info("Document supprimé (soft): id={}, requester={}", documentId, requesterId);

        // Supprimer physiquement le fichier
        storageService.delete(doc.getStorageKey());

        // WebSocket: notifier
        UUID patientId = doc.getPatient().getId();
        DocumentResponseDto responseDto = toDto(doc);
        sendEvent(patientId, "DOCUMENT_DELETED", responseDto);
    }

    // ===================================================================
    //  6. UPDATE STATUS (médecin)
    // ===================================================================
    @Transactional
    public DocumentResponseDto updateStatus(UUID documentId, String newStatus, UUID doctorId) {
        MedicalDocument doc = documentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new NotFoundException("Document introuvable."));

        // Vérifier lien actif entre médecin et patient du document
        linkGuard.assertActiveLink(doc.getPatient().getId(), doctorId);

        DocumentStatus status;
        try {
            status = DocumentStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut invalide: " + newStatus);
        }

        doc.setStatus(status);
        doc = documentRepository.save(doc);

        LOG.info("Statut document mis à jour: id={}, status={}, doctor={}", documentId, status, doctorId);

        DocumentResponseDto responseDto = toDto(doc);
        sendEvent(doc.getPatient().getId(), "DOCUMENT_UPDATED", responseDto);

        return responseDto;
    }

    // ===================================================================
    //  7. COUNT BY CATEGORY
    // ===================================================================
    @Transactional(readOnly = true)
    public Map<String, Long> countByCategory(UUID patientId) {
        if (!patientProfileRepository.existsById(patientId)) {
            throw new NotFoundException("Patient introuvable.");
        }

        linkGuard.assertPatientHasAtLeastOneActiveLink(patientId);

        return countByCategoryInternal(patientId);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countByCategoryForDoctor(UUID doctorId, UUID patientId) {
        linkGuard.assertActiveLink(patientId, doctorId);
        if (!patientProfileRepository.existsById(patientId)) {
            throw new NotFoundException("Patient introuvable.");
        }

        return countByCategoryInternal(documentRepository.countVisibleToDoctorByPatientGroupedByCategory(patientId));
    }

    private Map<String, Long> countByCategoryInternal(UUID patientId) {

        return countByCategoryInternal(documentRepository.countByPatientGroupedByCategory(patientId));
    }

    private Map<String, Long> countByCategoryInternal(List<Object[]> results) {

        // Initialiser toutes les catégories à 0
        Map<String, Long> counts = new LinkedHashMap<>();
        for (DocumentCategory cat : DocumentCategory.values()) {
            counts.put(cat.getSlug(), 0L);
        }

        // Remplir les compteurs
        for (Object[] row : results) {
            DocumentCategory cat = (DocumentCategory) row[0];
            Long count = (Long) row[1];
            counts.put(cat.getSlug(), count);
        }

        return counts;
    }

    // ===================================================================
    //  MAPPING Entity → DTO
    // ===================================================================
    private DocumentResponseDto toDto(MedicalDocument doc) {
        DocumentResponseDto dto = new DocumentResponseDto();
        dto.setId(doc.getId().toString());
        dto.setName(doc.getName());
        dto.setCategory(doc.getCategory().getSlug());
        dto.setDate(doc.getUploadDate().toString()); // yyyy-MM-dd
        dto.setPages(doc.getPageCount());
        dto.setStatus(doc.getStatus().name().toLowerCase());
        dto.setSize(formatFileSize(doc.getFileSize()));

        if (doc.getUploadedByDoctor() != null) {
            dto.setUploadedBy("doctor");
        } else {
            dto.setUploadedBy("patient");
        }

        // Nom complet du médecin
        if (doc.getUploadedByDoctor() != null && doc.getUploadedByDoctor().getUser() != null) {
            var user = doc.getUploadedByDoctor().getUser();
            dto.setDoctor("Dr. " + user.getFirstName() + " " + user.getLastName());
        } else if (doc.getPatient() != null && doc.getPatient().getAssignedDoctor() != null
                   && doc.getPatient().getAssignedDoctor().getUser() != null) {
            var user = doc.getPatient().getAssignedDoctor().getUser();
            dto.setDoctor("Dr. " + user.getFirstName() + " " + user.getLastName());
        } else {
            dto.setDoctor("—");
        }

        return dto;
    }

    // ===================================================================
    //  HELPERS
    // ===================================================================

    /**
     * Formatter la taille en Mo ou Ko.
     * Ex: 1258291 → "1.2 Mo", 512000 → "500 Ko"
     */
    private String formatFileSize(long bytes) {
        if (bytes >= 1_048_576) {
            double mo = bytes / 1_048_576.0;
            return String.format("%.1f Mo", mo);
        } else if (bytes >= 1024) {
            double ko = bytes / 1024.0;
            return String.format("%.0f Ko", ko);
        } else {
            return bytes + " o";
        }
    }

    /**
     * Nettoyer le nom de fichier pour le stockage.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Envoyer un événement WebSocket sur /topic/patient/{patientId}.
     */
    private void sendEvent(UUID patientId, String eventType, DocumentResponseDto doc) {
        DocumentEventDto event = new DocumentEventDto(eventType, doc);
        String destination = "/topic/patient/" + patientId;
        messagingTemplate.convertAndSend(destination, event);
        LOG.info("WebSocket event sent: type={}, destination={}", eventType, destination);
    }

    private void sendEventToDoctor(UUID doctorId, String eventType, DocumentResponseDto doc) {
        DocumentEventDto event = new DocumentEventDto(eventType, doc);
        String destination = "/topic/doctor/" + doctorId;
        messagingTemplate.convertAndSend(destination, event);
        LOG.info("WebSocket event sent: type={}, destination={}", eventType, destination);
    }
}
