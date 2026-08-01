package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

@Service
public class LinkService {

    private static final Logger LOG = LoggerFactory.getLogger(LinkService.class);

    private final PatientDoctorLinkRepository linkRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;

    public LinkService(
            PatientDoctorLinkRepository linkRepository,
            PatientProfileRepository patientProfileRepository,
            DoctorProfileRepository doctorProfileRepository,
            UserRepository userRepository
    ) {
        this.linkRepository = linkRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LinkResponseDto requestLink(LinkRequestCreateDto dto) {
        UUID patientId = dto.getPatientProfileId() != null ? dto.getPatientProfileId() : dto.getPatientId();
        UUID doctorId = dto.getDoctorProfileId() != null ? dto.getDoctorProfileId() : dto.getDoctorId();

        if (patientId == null || doctorId == null) {
            throw new BadRequestException("patientId/doctorId requis.");
        }

        UUID resolvedPatientProfileId = resolvePatientProfileId(patientId);
        UUID resolvedDoctorProfileId = resolveDoctorProfileId(doctorId);

        PatientProfile patient = patientProfileRepository.findById(resolvedPatientProfileId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));
        DoctorProfile doctor = doctorProfileRepository.findById(resolvedDoctorProfileId)
                .orElseThrow(() -> new NotFoundException("Doctor introuvable."));

        if (linkRepository.existsByPatient_IdAndDoctor_Id(patient.getId(), doctor.getId())) {
            throw new ConflictException("Lien déjà existant entre cette patiente et ce médecin.");
        }

        PatientDoctorLink link = new PatientDoctorLink();
        link.setPatient(patient);
        link.setDoctor(doctor);

        link.setStatus(PatientDoctorLink.Status.REQUESTED);
        link.setRequestedAt(Instant.now());

        // Champs venant du DTO
        link.setRequestedBy(dto.getRequestedBy());
        link.setRequestNote(dto.getRequestNote());

        // lastUpdatedAt est géré aussi par @PreUpdate, mais on le set à la création
        link.setLastUpdatedAt(Instant.now());

        link = linkRepository.save(link);
        return toResponse(link);
    }

    public List<LinkResponseDto> listPending(String actorType, UUID actorId) {
        return listByActorAndStatus(actorType, actorId, PatientDoctorLink.Status.REQUESTED);
    }

    public List<LinkResponseDto> listConnected(String actorType, UUID actorId) {
        return listByActorAndStatus(actorType, actorId, PatientDoctorLink.Status.ACTIVE);
    }

    @Transactional
    public LinkResponseDto approve(LinkActionRequestDto dto) {
        LinkResponseDto response = accept(dto.getLinkId());

        if (dto.getDecisionByUserId() != null) {
            PatientDoctorLink link = linkRepository.findById(dto.getLinkId())
                    .orElseThrow(() -> new NotFoundException("Link introuvable."));
            User decisionUser = userRepository.findById(dto.getDecisionByUserId())
                    .orElseThrow(() -> new NotFoundException("Utilisateur décisionnaire introuvable."));
            link.setDecisionByUser(decisionUser);
            link.setLastUpdatedAt(Instant.now());
            link = linkRepository.save(link);
            response = toResponse(link);
        }

        return response;
    }

    @Transactional
    public LinkResponseDto refuse(LinkActionRequestDto dto) {
        LinkResponseDto response = reject(dto.getLinkId());

        PatientDoctorLink link = linkRepository.findById(dto.getLinkId())
                .orElseThrow(() -> new NotFoundException("Link introuvable."));

        if (dto.getDecisionByUserId() != null) {
            User decisionUser = userRepository.findById(dto.getDecisionByUserId())
                    .orElseThrow(() -> new NotFoundException("Utilisateur décisionnaire introuvable."));
            link.setDecisionByUser(decisionUser);
        }

        if (dto.getRejectionReason() != null && !dto.getRejectionReason().isBlank()) {
            link.setRejectionReason(dto.getRejectionReason());
        }

        link.setLastUpdatedAt(Instant.now());
        link = linkRepository.save(link);
        return toResponse(link);
    }

    private List<LinkResponseDto> listByActorAndStatus(String actorType, UUID actorId, PatientDoctorLink.Status status) {
        if (actorId == null) {
            throw new BadRequestException("actorId requis.");
        }

        if (actorType == null || actorType.isBlank()) {
            throw new BadRequestException("actorType requis: patient|doctor.");
        }

        String normalized = actorType.trim().toLowerCase();
        if ("doctor".equals(normalized)) {
            UUID resolvedDoctorProfileId = resolveDoctorProfileId(actorId);
            return linkRepository.findByDoctor_IdAndStatus(resolvedDoctorProfileId, status)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        if ("patient".equals(normalized)) {
            UUID resolvedPatientProfileId = resolvePatientProfileId(actorId);
            return linkRepository.findByPatient_IdAndStatus(resolvedPatientProfileId, status)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        throw new BadRequestException("actorType invalide. Valeurs: patient|doctor.");
    }

    @Transactional
    public LinkResponseDto accept(UUID linkId) {
        PatientDoctorLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NotFoundException("Link introuvable."));
        if (link.getStatus() != PatientDoctorLink.Status.REQUESTED) {
            throw new BadRequestException("Seules les demandes REQUESTED peuvent être acceptées.");
        }

        link.setStatus(PatientDoctorLink.Status.ACTIVE);
        link.setActivatedAt(Instant.now());
        link.setLastUpdatedAt(Instant.now());

        link = linkRepository.save(link);
        return toResponse(link);
    }

    @Transactional
    public LinkResponseDto reject(UUID linkId) {
        PatientDoctorLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NotFoundException("Link introuvable."));
        if (link.getStatus() != PatientDoctorLink.Status.REQUESTED) {
            throw new BadRequestException("Seules les demandes REQUESTED peuvent être rejetées.");
        }

        link.setStatus(PatientDoctorLink.Status.REJECTED);
        link.setLastUpdatedAt(Instant.now());

        link = linkRepository.save(link);
        return toResponse(link);
    }

    @Transactional
    public LinkResponseDto block(UUID linkId) {
        PatientDoctorLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NotFoundException("Link introuvable."));

        link.setStatus(PatientDoctorLink.Status.BLOCKED);
        link.setLastUpdatedAt(Instant.now());

        link = linkRepository.save(link);
        return toResponse(link);
    }

    @Transactional
    public LinkResponseDto end(UUID linkId) {
        PatientDoctorLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NotFoundException("Link introuvable."));
        if (link.getStatus() != PatientDoctorLink.Status.ACTIVE) {
            throw new BadRequestException("Seuls les liens ACTIVE peuvent être terminés.");
        }

        link.setStatus(PatientDoctorLink.Status.ENDED);
        link.setEndedAt(Instant.now());
        link.setLastUpdatedAt(Instant.now());

        link = linkRepository.save(link);
        return toResponse(link);
    }

    public boolean isLinkActive(UUID patientId, UUID doctorId) {
        LOG.debug("isLinkActive called with patientId={}, doctorId={}", patientId, doctorId);

        UUID resolvedPatientProfileId = resolvePatientProfileId(patientId);
        UUID resolvedDoctorProfileId = resolveDoctorProfileId(doctorId);

        LOG.debug("isLinkActive resolved IDs: patientProfileId={}, doctorProfileId={}", resolvedPatientProfileId, resolvedDoctorProfileId);

        var linkOpt = linkRepository.findByPatient_IdAndDoctor_Id(resolvedPatientProfileId, resolvedDoctorProfileId);
        if (linkOpt.isPresent()) {
            LOG.debug("isLinkActive found link: linkId={}, status={}", linkOpt.get().getId(), linkOpt.get().getStatus());
        } else {
            LOG.debug("isLinkActive found no link for patientProfileId={}, doctorProfileId={}", resolvedPatientProfileId, resolvedDoctorProfileId);
        }

        return linkOpt
                .map(l -> l.getStatus() == PatientDoctorLink.Status.ACTIVE)
                .orElse(false);
    }

    public LinkResponseDto getByPair(UUID patientId, UUID doctorId) {
        LOG.debug("getByPair called with patientId={}, doctorId={}", patientId, doctorId);

        UUID resolvedPatientProfileId = resolvePatientProfileId(patientId);
        UUID resolvedDoctorProfileId = resolveDoctorProfileId(doctorId);

        LOG.debug("getByPair resolved IDs: patientProfileId={}, doctorProfileId={}", resolvedPatientProfileId, resolvedDoctorProfileId);

        PatientDoctorLink link = linkRepository.findByPatient_IdAndDoctor_Id(resolvedPatientProfileId, resolvedDoctorProfileId)
                .orElseThrow(() -> new NotFoundException("Link introuvable pour ce couple patient/médecin."));

        LOG.debug("getByPair found link: linkId={}, status={}", link.getId(), link.getStatus());
        return toResponse(link);
    }

    public List<LinkResponseDto> listDoctorLinks(UUID doctorId, PatientDoctorLink.Status status) {
        UUID resolvedDoctorProfileId = resolveDoctorProfileId(doctorId);
        return linkRepository.findByDoctor_IdAndStatus(resolvedDoctorProfileId, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<LinkResponseDto> listPatientLinks(UUID patientId, PatientDoctorLink.Status status) {
        UUID resolvedPatientProfileId = resolvePatientProfileId(patientId);
        return linkRepository.findByPatient_IdAndStatus(resolvedPatientProfileId, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UUID resolveDoctorProfileId(UUID doctorIdOrUserId) {
        if (doctorIdOrUserId == null) {
            throw new BadRequestException("doctorId requis.");
        }

        if (doctorProfileRepository.existsById(doctorIdOrUserId)) {
            return doctorIdOrUserId;
        }

        return doctorProfileRepository.findByUser_Id(doctorIdOrUserId)
                .map(DoctorProfile::getId)
                .orElseThrow(() -> new NotFoundException("Doctor introuvable."));
    }

    private UUID resolvePatientProfileId(UUID patientIdOrUserId) {
        if (patientIdOrUserId == null) {
            throw new BadRequestException("patientId requis.");
        }

        if (patientProfileRepository.existsById(patientIdOrUserId)) {
            return patientIdOrUserId;
        }

        return patientProfileRepository.findByUser_Id(patientIdOrUserId)
                .map(PatientProfile::getId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));
    }

    private LinkResponseDto toResponse(PatientDoctorLink link) {
        LinkResponseDto dto = new LinkResponseDto();

        dto.setId(link.getId());

        // Align avec LinkResponseDto (patientProfileId/doctorProfileId)
        dto.setPatientProfileId(link.getPatient() != null ? link.getPatient().getId() : null);
        dto.setDoctorProfileId(link.getDoctor() != null ? link.getDoctor().getId() : null);

        dto.setStatus(link.getStatus());

        dto.setRequestedBy(link.getRequestedBy());
        dto.setRequestNote(link.getRequestNote());

        // ✅ CORRECTION ICI : decisionByUser est un User (pas UUID direct)
        dto.setDecisionByUserId(
                link.getDecisionByUser() != null ? link.getDecisionByUser().getId() : null
        );

        dto.setRejectionReason(link.getRejectionReason());

        dto.setRequestedAt(link.getRequestedAt());
        dto.setActivatedAt(link.getActivatedAt());
        dto.setEndedAt(link.getEndedAt());
        dto.setLastUpdatedAt(link.getLastUpdatedAt());

        return dto;
    }
}
