package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.entity.DoctorProfile;
import com.breastcancer.breastcancerbackend.entity.PatientDoctorLink;
import com.breastcancer.breastcancerbackend.entity.PatientProfile;
import com.breastcancer.breastcancerbackend.repository.DoctorProfileRepository;
import com.breastcancer.breastcancerbackend.repository.PatientDoctorLinkRepository;
import com.breastcancer.breastcancerbackend.repository.PatientProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

@Service
public class DoctorPatientLinkGuardService {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorPatientLinkGuardService.class);

    private final PatientDoctorLinkRepository linkRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    public DoctorPatientLinkGuardService(PatientDoctorLinkRepository linkRepository,
                                         PatientProfileRepository patientProfileRepository,
                                         DoctorProfileRepository doctorProfileRepository) {
        this.linkRepository = linkRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    public void assertActiveLink(UUID patientIdOrUserId, UUID doctorIdOrUserId) {
        LOG.debug("assertActiveLink called with patientIdOrUserId={}, doctorIdOrUserId={}", patientIdOrUserId, doctorIdOrUserId);

        UUID patientProfileId = resolvePatientProfileId(patientIdOrUserId);
        UUID doctorProfileId = resolveDoctorProfileId(doctorIdOrUserId);

        LOG.debug("Resolved IDs for active-link check: patientProfileId={}, doctorProfileId={}", patientProfileId, doctorProfileId);

        var linkOpt = linkRepository.findByPatient_IdAndDoctor_Id(patientProfileId, doctorProfileId);
        boolean isActive = linkOpt
                .map(link -> link.getStatus() == PatientDoctorLink.Status.ACTIVE)
                .orElse(false);

        if (linkOpt.isPresent()) {
            LOG.debug("Link found for patientProfileId={} and doctorProfileId={}: linkId={}, status={}",
                    patientProfileId,
                    doctorProfileId,
                    linkOpt.get().getId(),
                    linkOpt.get().getStatus());
        } else {
            LOG.debug("No link found for patientProfileId={} and doctorProfileId={}", patientProfileId, doctorProfileId);
        }

        if (!isActive) {
            LOG.warn("Active-link guard denied action: patientProfileId={}, doctorProfileId={}, linkExists={}, resolvedStatus={}",
                    patientProfileId,
                    doctorProfileId,
                    linkOpt.isPresent(),
                    linkOpt.map(PatientDoctorLink::getStatus).orElse(null));
            throw new ForbiddenException("Action autorisée uniquement avec un lien patient-médecin ACTIVE.");
        }

        LOG.debug("Active-link guard passed for patientProfileId={} and doctorProfileId={}", patientProfileId, doctorProfileId);
    }

    public void assertPatientHasAtLeastOneActiveLink(UUID patientIdOrUserId) {
        UUID patientProfileId = resolvePatientProfileId(patientIdOrUserId);
        boolean hasActiveLink = !linkRepository.findByPatient_IdAndStatus(patientProfileId, PatientDoctorLink.Status.ACTIVE).isEmpty();
        if (!hasActiveLink) {
            throw new ForbiddenException("Aucun lien patient-médecin ACTIVE trouvé pour cette patiente.");
        }
    }

    public UUID resolvePatientProfileId(UUID patientIdOrUserId) {
        if (patientIdOrUserId == null) {
            throw new BadRequestException("patientId requis.");
        }

        if (patientProfileRepository.existsById(patientIdOrUserId)) {
            LOG.debug("resolvePatientProfileId: input {} already matches a patient profile ID", patientIdOrUserId);
            return patientIdOrUserId;
        }

        UUID resolved = patientProfileRepository.findByUser_Id(patientIdOrUserId)
                .map(PatientProfile::getId)
                .orElseThrow(() -> new NotFoundException("Patient introuvable."));
        LOG.debug("resolvePatientProfileId: mapped user ID {} to patient profile ID {}", patientIdOrUserId, resolved);
        return resolved;
    }

    public UUID resolveDoctorProfileId(UUID doctorIdOrUserId) {
        if (doctorIdOrUserId == null) {
            throw new BadRequestException("doctorId requis.");
        }

        if (doctorProfileRepository.existsById(doctorIdOrUserId)) {
            LOG.debug("resolveDoctorProfileId: input {} already matches a doctor profile ID", doctorIdOrUserId);
            return doctorIdOrUserId;
        }

        UUID resolved = doctorProfileRepository.findByUser_Id(doctorIdOrUserId)
                .map(DoctorProfile::getId)
                .orElseThrow(() -> new NotFoundException("Doctor introuvable."));
        LOG.debug("resolveDoctorProfileId: mapped user ID {} to doctor profile ID {}", doctorIdOrUserId, resolved);
        return resolved;
    }
}