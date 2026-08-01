package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.*;

@Service
public class AppointmentService {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentService.class);

    // Constraint DB observée: UPCOMING non supporté au persist dans la table appointments.
    private static final Set<Appointment.Status> DB_ALLOWED_STATUSES = EnumSet.of(
        Appointment.Status.REQUESTED,
        Appointment.Status.CONFIRMED,
        Appointment.Status.CANCELLED,
        Appointment.Status.COMPLETED,
        Appointment.Status.NO_SHOW
    );

    private static final Map<Appointment.AppointmentType, Set<Appointment.Status>> ALLOWED_STATUS_BY_TYPE = Map.of(
        Appointment.AppointmentType.CONSULTATION, DB_ALLOWED_STATUSES,
        Appointment.AppointmentType.EXAM, DB_ALLOWED_STATUSES,
        Appointment.AppointmentType.TREATMENT, DB_ALLOWED_STATUSES,
        Appointment.AppointmentType.FOLLOW_UP, DB_ALLOWED_STATUSES,
        Appointment.AppointmentType.OTHER, DB_ALLOWED_STATUSES
    );

    private final AppointmentRepository appointmentRepository;
    private final PatientDoctorLinkRepository linkRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final CalendarService calendarService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            PatientDoctorLinkRepository linkRepository,
            PatientProfileRepository patientProfileRepository,
            DoctorProfileRepository doctorProfileRepository,
            CalendarService calendarService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.linkRepository = linkRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.calendarService = calendarService;
    }

    @Transactional
    public AppointmentResponseDto create(AppointmentCreateRequestDto dto) {
        if (dto.getLinkId() == null) throw new BadRequestException("linkId requis.");
        if (dto.getStartAt() == null || dto.getEndAt() == null) throw new BadRequestException("startAt/endAt requis.");
        if (!dto.getStartAt().isBefore(dto.getEndAt())) throw new BadRequestException("startAt doit être avant endAt.");
        validateFutureStart(dto.getStartAt());

        LOG.debug("create appointment called with linkId={}", dto.getLinkId());

        PatientDoctorLink link = linkRepository.findById(dto.getLinkId())
                .orElseThrow(() -> new NotFoundException("Link introuvable."));

        LOG.debug("create appointment resolved link: linkId={}, patientProfileId={}, doctorProfileId={}, status={}",
            link.getId(),
            link.getPatient() != null ? link.getPatient().getId() : null,
            link.getDoctor() != null ? link.getDoctor().getId() : null,
            link.getStatus());

        if (link.getStatus() != PatientDoctorLink.Status.ACTIVE) {
            LOG.warn("create appointment denied because link is not ACTIVE: linkId={}, status={}", link.getId(), link.getStatus());
            throw new BadRequestException("RDV autorisé uniquement si le lien est ACTIVE.");
        }

        UUID doctorId = link.getDoctor().getId();
        calendarService.assertSlotBookable(doctorId, dto.getStartAt(), dto.getEndAt(), null);

        Appointment a = new Appointment();
        a.setLink(link);
        a.setStartAt(dto.getStartAt());
        a.setEndAt(dto.getEndAt());
        a.setType(Appointment.AppointmentType.CONSULTATION);
        a.setTitle(dto.getReason());
        a.setDescription(dto.getReason());
        a.setMode(dto.getMode());
        a.setStatus(Appointment.Status.REQUESTED);
        a.setReason(dto.getReason());
        a.setPatientNotes(dto.getPatientNotes());
        a.setDoctorNotes(null);
        a.setVideoRoomUrl(dto.getVideoRoomUrl());
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());

        normalizeAndValidateForDatabase(a, "create");

        a = appointmentRepository.save(a);
        return toResponse(a);
    }

    @Transactional
    public AppointmentResponseDto update(UUID appointmentId, AppointmentUpdateRequestDto dto) {
        Appointment a = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment introuvable."));

        if (a.getStatus() == Appointment.Status.CANCELLED || a.getStatus() == Appointment.Status.COMPLETED) {
            throw new BadRequestException("RDV finalisé : modification interdite.");
        }

        // ✅ pour éviter le problème "effectively final" dans le lambda
        UUID currentAppointmentId = a.getId();

        if (dto.getStartAt() != null || dto.getEndAt() != null) {
            Instant start = dto.getStartAt() != null ? dto.getStartAt() : a.getStartAt();
            Instant end = dto.getEndAt() != null ? dto.getEndAt() : a.getEndAt();
            if (!start.isBefore(end)) throw new BadRequestException("startAt doit être avant endAt.");
            validateFutureStart(start);

            UUID doctorId = a.getLink().getDoctor().getId();
                calendarService.assertSlotBookable(doctorId, start, end, currentAppointmentId);

            a.setStartAt(start);
            a.setEndAt(end);
        }

        if (dto.getMode() != null) a.setMode(dto.getMode());
        if (dto.getReason() != null) {
            a.setReason(dto.getReason());
            if (a.getTitle() == null || a.getTitle().isBlank()) {
                a.setTitle(dto.getReason());
            }
            if (a.getDescription() == null || a.getDescription().isBlank()) {
                a.setDescription(dto.getReason());
            }
        }
        if (dto.getPatientNotes() != null) a.setPatientNotes(dto.getPatientNotes());
        if (dto.getDoctorNotes() != null) a.setDoctorNotes(dto.getDoctorNotes());
        if (dto.getVideoRoomUrl() != null) a.setVideoRoomUrl(dto.getVideoRoomUrl());

        a.setUpdatedAt(Instant.now());
        a = appointmentRepository.save(a);
        return toResponse(a);
    }

    @Transactional
    public AppointmentResponseDto updateStatus(UUID appointmentId, AppointmentStatusUpdateDto dto) {
        Appointment a = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment introuvable."));

        if (dto.getStatus() == null) throw new BadRequestException("status requis.");

        // transitions simples
        if (a.getStatus() == Appointment.Status.CANCELLED || a.getStatus() == Appointment.Status.COMPLETED) {
            throw new BadRequestException("RDV finalisé : changement de statut interdit.");
        }

        Appointment.Status normalizedStatus = normalizeStatusForDatabase(dto.getStatus());
        validateStatusTypeCombination(a.getType(), normalizedStatus);
        a.setStatus(normalizedStatus);
        a.setUpdatedAt(Instant.now());
        a = appointmentRepository.save(a);
        return toResponse(a);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> listByLink(UUID linkId) {
        return appointmentRepository.findByLink_IdOrderByStartAtDesc(linkId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> listDoctorCalendar(UUID doctorId, Instant from, Instant to) {
        UUID resolvedDoctorProfileId = resolveDoctorProfileId(doctorId);
        return appointmentRepository.findByLink_Doctor_IdAndStartAtBetween(resolvedDoctorProfileId, from, to)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> listPatientCalendar(UUID patientId, Instant from, Instant to) {
        return appointmentRepository.findByLink_Patient_IdAndStartAtBetween(patientId, from, to)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentAvailabilityResponseDto checkAvailability(
            UUID doctorId,
            LocalDate date,
            LocalTime heure,
            Integer durationMinutes
    ) {
        if (doctorId == null) throw new BadRequestException("doctorId requis.");
        if (date == null) throw new BadRequestException("date requise.");
        if (heure == null) throw new BadRequestException("heure requise.");

        int duration = durationMinutes == null || durationMinutes <= 0 ? 30 : durationMinutes;
        ZoneId zoneId = ZoneOffset.UTC;
        UUID resolvedDoctorProfileId = resolveDoctorProfileId(doctorId);
        Optional<DoctorProfile> doctor = doctorProfileRepository.findById(resolvedDoctorProfileId);
        if (doctor.isPresent() && doctor.get().getTimezone() != null && !doctor.get().getTimezone().isBlank()) {
            try {
                zoneId = ZoneId.of(doctor.get().getTimezone());
            } catch (Exception ignored) {
                zoneId = ZoneOffset.UTC;
            }
        }

        Instant start = ZonedDateTime.of(date, heure, zoneId).toInstant();
        Instant end = start.plusSeconds(duration * 60L);

        boolean available = calendarService.isSlotAvailable(resolvedDoctorProfileId, start, end, null);

        AppointmentAvailabilityResponseDto dto = new AppointmentAvailabilityResponseDto();
        dto.setAvailable(available);
        dto.setMessage(available ? "Créneau disponible" : "Créneau indisponible");
        return dto;
    }

    @Transactional
    public PatientAppointmentDto createFromFront(AppointmentCreateFrontRequestDto dto) {
        if (dto.getDate() == null) throw new BadRequestException("date requise.");
        if (dto.getHeure() == null) throw new BadRequestException("heure requise.");

        Instant start = LocalDateTime.of(dto.getDate(), dto.getHeure()).toInstant(ZoneOffset.UTC);
        int duration = dto.getDurationMinutes() == null || dto.getDurationMinutes() <= 0 ? 30 : dto.getDurationMinutes();
        Instant end = start.plusSeconds(duration * 60L);

        CreatePatientAppointmentDto createDto = new CreatePatientAppointmentDto();
        createDto.setPatientId(dto.getPatientId());
        createDto.setDoctorId(dto.getDoctorId());
        createDto.setType(dto.getTypeRDV());
        createDto.setTitle(dto.getTitle());
        createDto.setDescription(dto.getDescription());
        createDto.setDate(start);
        createDto.setEndDate(end);
        createDto.setLocation(dto.getLocation());

        return createPatientAppointment(createDto);
    }

    @Transactional
    public Page<PatientAppointmentDto> listPatientAppointmentsAlias(
            UUID patientId,
            UUID doctorId,
            LocalDate date,
            Appointment.AppointmentType typeRDV,
            Appointment.Status status,
            int page,
            int size
    ) {
        return getPatientAppointments(patientId, date, typeRDV, status, doctorId, page, size);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> listDoctorAppointmentsAlias(
            UUID doctorId,
            Instant from,
            Instant to
    ) {
        UUID resolvedDoctorProfileId = resolveDoctorProfileId(doctorId);

        if (resolvedDoctorProfileId == null) {
            throw new BadRequestException("doctorId requis.");
        }

        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new BadRequestException("from doit être inférieur ou égal à to.");
            }
            return listDoctorCalendar(resolvedDoctorProfileId, from, to);
        }

        if (from != null) {
            return appointmentRepository.findByLink_Doctor_IdAndStartAtGreaterThanEqualOrderByStartAtAsc(resolvedDoctorProfileId, from)
                    .stream().map(this::toResponse).toList();
        }

        if (to != null) {
            return appointmentRepository.findByLink_Doctor_IdAndStartAtLessThanEqualOrderByStartAtAsc(resolvedDoctorProfileId, to)
                    .stream().map(this::toResponse).toList();
        }

        return appointmentRepository.findByLink_Doctor_IdOrderByStartAtAsc(resolvedDoctorProfileId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<PatientAppointmentDto> getPatientAppointments(
            UUID patientId,
            LocalDate date,
            Appointment.AppointmentType type,
            Appointment.Status status,
            UUID doctorId,
            int page,
            int size
    ) {
        UUID resolvedPatientProfileId = resolvePatientProfileId(patientId);
        UUID resolvedDoctorProfileId = doctorId != null ? resolveDoctorProfileId(doctorId) : null;

        autoCompletePastAppointments(resolvedPatientProfileId);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("startAt").descending());

        Specification<Appointment> specification = (root, query, cb) -> cb.equal(root.join("link").join("patient").get("id"), resolvedPatientProfileId);

        if (date != null) {
            Instant dayStart = date.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant dayEnd = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            specification = specification.and((root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get("startAt"), dayStart),
                    cb.lessThan(root.get("startAt"), dayEnd)
            ));
        }

        if (type != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }

        if (status != null) {
            Appointment.Status normalizedStatus = normalizeStatusForDatabase(status);
            if (status == Appointment.Status.UPCOMING && normalizedStatus != Appointment.Status.UPCOMING) {
                specification = specification.and((root, query, cb) -> root.get("status").in(Appointment.Status.UPCOMING, normalizedStatus));
            } else {
                specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), normalizedStatus));
            }
        }

        if (resolvedDoctorProfileId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.join("link").join("doctor").get("id"), resolvedDoctorProfileId));
        }

        return appointmentRepository.findAll(specification, pageable).map(this::toPatientAppointmentDto);
    }

    @Transactional(readOnly = true)
    public PatientAppointmentDto getAppointmentDetails(UUID patientId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findByIdWithNotes(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment introuvable pour ce patient."));
        // Vérification de sécurité : le rendez-vous appartient bien au patient demandé
        if (appointment.getLink() == null || appointment.getLink().getPatient() == null || !appointment.getLink().getPatient().getId().equals(patientId)) {
            throw new NotFoundException("Appointment introuvable pour ce patient.");
        }
        refreshStatusIfPast(appointment);
        return toPatientAppointmentDto(appointment);
    }

    @Transactional
    public PatientAppointmentDto createPatientAppointment(CreatePatientAppointmentDto dto) {
        if (dto.getDate() == null) throw new BadRequestException("date requise.");

        LOG.debug("createPatientAppointment called with patientId={}, doctorId={}, linkId={}",
            dto.getPatientId(), dto.getDoctorId(), dto.getLinkId());

        Instant endDate = dto.getEndDate() != null ? dto.getEndDate() : dto.getDate().plusSeconds(30 * 60);
        if (!dto.getDate().isBefore(endDate)) throw new BadRequestException("date doit être avant endDate.");
        validateFutureStart(dto.getDate());

        PatientDoctorLink link;
        if (dto.getLinkId() != null) {
            link = linkRepository.findById(dto.getLinkId())
                    .orElseThrow(() -> new NotFoundException("Lien patient-médecin introuvable."));

            LOG.debug("createPatientAppointment found link by linkId: linkId={}, patientProfileId={}, doctorProfileId={}, status={}",
                link.getId(),
                link.getPatient() != null ? link.getPatient().getId() : null,
                link.getDoctor() != null ? link.getDoctor().getId() : null,
                link.getStatus());

            if (dto.getPatientId() != null && (link.getPatient() == null || !dto.getPatientId().equals(link.getPatient().getId()))) {
                throw new BadRequestException("linkId/patientId incohérents.");
            }

            if (dto.getDoctorId() != null && (link.getDoctor() == null || !dto.getDoctorId().equals(link.getDoctor().getId()))) {
                throw new BadRequestException("linkId/doctorId incohérents.");
            }
        } else {
            if (dto.getPatientId() == null) throw new BadRequestException("patientId requis.");
            if (dto.getDoctorId() == null) throw new BadRequestException("doctorId requis.");

            UUID resolvedPatientProfileId = resolvePatientProfileId(dto.getPatientId());
            UUID resolvedDoctorProfileId = resolveDoctorProfileId(dto.getDoctorId());

            LOG.debug("createPatientAppointment resolved IDs: inputPatientId={}, inputDoctorId={}, resolvedPatientProfileId={}, resolvedDoctorProfileId={}",
                    dto.getPatientId(), dto.getDoctorId(), resolvedPatientProfileId, resolvedDoctorProfileId);

            link = linkRepository.findByPatient_IdAndDoctor_Id(resolvedPatientProfileId, resolvedDoctorProfileId)
                    .orElseThrow(() -> new NotFoundException("Lien patient-médecin introuvable."));

            LOG.debug("createPatientAppointment found link by patient/doctor pair: linkId={}, status={}", link.getId(), link.getStatus());
        }

        if (link.getStatus() != PatientDoctorLink.Status.ACTIVE) {
            LOG.warn("createPatientAppointment denied because link is not ACTIVE: linkId={}, status={}", link.getId(), link.getStatus());
            throw new BadRequestException("Création RDV autorisée uniquement avec un lien ACTIVE.");
        }

        calendarService.assertSlotBookable(link.getDoctor().getId(), dto.getDate(), endDate, null);

        Appointment appointment = new Appointment();
        appointment.setLink(link);
        appointment.setType(dto.getType());
        appointment.setTitle(dto.getTitle());
        appointment.setDescription(dto.getDescription());
        appointment.setLocation(dto.getLocation());
        appointment.setStartAt(dto.getDate());
        appointment.setEndAt(endDate);
        appointment.setMode(Appointment.Mode.IN_PERSON);
        appointment.setStatus(Appointment.Status.UPCOMING);
        appointment.setReason(dto.getDescription());
        appointment.setPatientNotes(dto.getDescription());
        if (dto.getNotes() != null && !dto.getNotes().isEmpty()) {
            appointment.setNotes(new ArrayList<>(dto.getNotes()));
        }
        appointment.setCreatedAt(Instant.now());
        appointment.setUpdatedAt(Instant.now());

        normalizeAndValidateForDatabase(appointment, "createPatientAppointment");

        appointment = appointmentRepository.save(appointment);
        return toPatientAppointmentDto(appointment);
    }

    @Transactional
    public PatientAppointmentDto updatePatientAppointment(UUID appointmentId, UpdatePatientAppointmentDto dto) {
        if (dto.getPatientId() == null) throw new BadRequestException("patientId requis.");

        Appointment appointment = appointmentRepository.findByIdAndLink_Patient_Id(appointmentId, dto.getPatientId())
                .orElseThrow(() -> new NotFoundException("Appointment introuvable pour ce patient."));

        if (appointment.getStatus() == Appointment.Status.CANCELLED || appointment.getStatus() == Appointment.Status.COMPLETED) {
            throw new BadRequestException("RDV finalisé : modification interdite.");
        }

        Instant newStart = dto.getDate() != null ? dto.getDate() : appointment.getStartAt();
        Instant newEnd = dto.getEndDate() != null ? dto.getEndDate() : appointment.getEndAt();

        if (!newStart.isBefore(newEnd)) throw new BadRequestException("date doit être avant endDate.");
        validateFutureStart(newStart);

        calendarService.assertSlotBookable(appointment.getLink().getDoctor().getId(), newStart, newEnd, appointmentId);

        if (dto.getTitle() != null) appointment.setTitle(dto.getTitle());
        if (dto.getDescription() != null) {
            appointment.setDescription(dto.getDescription());
            appointment.setReason(dto.getDescription());
            appointment.setPatientNotes(dto.getDescription());
        }
        if (dto.getLocation() != null) appointment.setLocation(dto.getLocation());
        appointment.setStartAt(newStart);
        appointment.setEndAt(newEnd);
        if (dto.getNotes() != null && !dto.getNotes().isEmpty()) {
            appointment.setNotes(new ArrayList<>(dto.getNotes()));
        }
        appointment.setUpdatedAt(Instant.now());

        appointment = appointmentRepository.save(appointment);
        return toPatientAppointmentDto(appointment);
    }

    @Transactional
    public void cancelPatientAppointment(UUID patientId, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findByIdAndLink_Patient_Id(appointmentId, patientId)
                .orElseThrow(() -> new NotFoundException("Appointment introuvable pour ce patient."));

        appointment.setStatus(Appointment.Status.CANCELLED);
        appointment.setUpdatedAt(Instant.now());
        appointmentRepository.save(appointment);
    }

    @Transactional
    public AppointmentResponseDto cancelAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment introuvable."));

        if (appointment.getStatus() == Appointment.Status.CANCELLED) {
            return toResponse(appointment);
        }
        if (appointment.getStatus() == Appointment.Status.COMPLETED) {
            throw new BadRequestException("Impossible d'annuler un rendez-vous déjà terminé.");
        }

        appointment.setStatus(Appointment.Status.CANCELLED);
        appointment.setUpdatedAt(Instant.now());
        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponseDto rescheduleAppointment(UUID appointmentId, AppointmentRescheduleRequestDto dto) {
        Appointment original = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment introuvable."));

        if (dto.getStartAt() == null || dto.getEndAt() == null || !dto.getStartAt().isBefore(dto.getEndAt())) {
            throw new BadRequestException("startAt/endAt invalides.");
        }
        validateFutureStart(dto.getStartAt());

        if (original.getStatus() == Appointment.Status.CANCELLED || original.getStatus() == Appointment.Status.COMPLETED) {
            throw new BadRequestException("RDV finalisé : reschedule interdit.");
        }

        UUID doctorId = original.getLink().getDoctor().getId();
        calendarService.assertSlotBookable(doctorId, dto.getStartAt(), dto.getEndAt(), null);

        Appointment rescheduled = new Appointment();
        rescheduled.setLink(original.getLink());
        rescheduled.setRescheduledFrom(original);
        rescheduled.setStartAt(dto.getStartAt());
        rescheduled.setEndAt(dto.getEndAt());
        rescheduled.setType(original.getType());
        rescheduled.setTitle(original.getTitle());
        rescheduled.setDescription(original.getDescription());
        rescheduled.setLocation(original.getLocation());
        rescheduled.setMode(original.getMode());
        rescheduled.setStatus(Appointment.Status.REQUESTED);
        rescheduled.setReason(dto.getReason() != null ? dto.getReason() : original.getReason());
        rescheduled.setPatientNotes(original.getPatientNotes());
        rescheduled.setDoctorNotes(original.getDoctorNotes());
        rescheduled.setVideoRoomUrl(original.getVideoRoomUrl());
        rescheduled.setCreatedAt(Instant.now());
        rescheduled.setUpdatedAt(Instant.now());
        if (original.getNotes() != null && !original.getNotes().isEmpty()) {
            rescheduled.setNotes(new ArrayList<>(original.getNotes()));
        }

        normalizeAndValidateForDatabase(rescheduled, "rescheduleAppointment");
        rescheduled = appointmentRepository.save(rescheduled);

        original.setStatus(Appointment.Status.CANCELLED);
        original.setUpdatedAt(Instant.now());
        original.setReason("Rescheduled to " + rescheduled.getId());
        appointmentRepository.save(original);

        return toResponse(rescheduled);
    }

    @Transactional
    public Optional<Appointment> getNextAppointment(UUID patientId) {
        autoCompletePastAppointments(patientId);
        Set<Appointment.Status> statuses = Set.of(Appointment.Status.UPCOMING, Appointment.Status.CONFIRMED, Appointment.Status.REQUESTED);
        return appointmentRepository
                .findFirstByLink_Patient_IdAndStatusInAndStartAtAfterOrderByStartAtAsc(patientId, statuses, Instant.now());
    }

    @Transactional
    public Optional<PatientAppointmentDto> getNextAppointmentDto(UUID patientId) {
        return getNextAppointment(patientId).map(this::toPatientAppointmentDto);
    }

    @Transactional
    public AppointmentStatsDto getAppointmentStats(UUID patientId) {
        if (patientId == null) {
            throw new BadRequestException("patientId requis.");
        }

        autoCompletePastAppointments(patientId);

        long totalAppointments = appointmentRepository.countByLink_Patient_Id(patientId);
        long totalDoctors = appointmentRepository.countDistinctDoctorByPatientId(patientId);
        long totalExams = appointmentRepository.countByLink_Patient_IdAndType(patientId, Appointment.AppointmentType.EXAM);

        long completedCount = appointmentRepository.findByLink_Patient_IdOrderByStartAtAsc(patientId)
                .stream()
                .filter(appointment -> appointment.getStatus() == Appointment.Status.COMPLETED)
                .count();

        int computedProgress = totalAppointments == 0 ? 0 : (int) Math.round((completedCount * 100.0) / totalAppointments);
        int profileCompletion = patientProfileRepository.findById(patientId)
            .map(patientProfile -> patientProfile.getProfileCompletion() == null ? 0 : patientProfile.getProfileCompletion())
                .orElse(0);

        AppointmentStatsDto dto = new AppointmentStatsDto();
        dto.setTotalAppointments(totalAppointments);
        dto.setTotalDoctors(totalDoctors);
        dto.setTotalExams(totalExams);
        dto.setProgressPercentage(Math.max(computedProgress, profileCompletion));
        return dto;
    }

    @Transactional
    public List<TimelineEventDto> generateTimeline(UUID patientId) {
        autoCompletePastAppointments(patientId);

        return appointmentRepository.findByLink_Patient_IdOrderByStartAtAsc(patientId)
                .stream()
                .filter(appointment -> appointment.getStatus() != Appointment.Status.CANCELLED)
                .map(this::toTimelineEvent)
                .toList();
    }

    private AppointmentResponseDto toResponse(Appointment a) {
        AppointmentResponseDto dto = new AppointmentResponseDto();
        dto.setId(a.getId());
        dto.setLinkId(a.getLink() != null ? a.getLink().getId() : null);

        // ✅ CORRIGÉ : champs du DTO
        dto.setDoctorProfileId(
                a.getLink() != null && a.getLink().getDoctor() != null ? a.getLink().getDoctor().getId() : null
        );
        dto.setRescheduledFromId(a.getRescheduledFrom() != null ? a.getRescheduledFrom().getId() : null);
        dto.setPatientProfileId(
                a.getLink() != null && a.getLink().getPatient() != null ? a.getLink().getPatient().getId() : null
        );

        dto.setStartAt(a.getStartAt());
        dto.setEndAt(a.getEndAt());
        dto.setMode(a.getMode());
        dto.setStatus(a.getStatus());
        dto.setReason(a.getReason());

        if (a.getLink() != null && a.getLink().getPatient() != null && a.getLink().getPatient().getUser() != null) {
            dto.setPatientFirstName(a.getLink().getPatient().getUser().getFirstName());
            dto.setPatientLastName(a.getLink().getPatient().getUser().getLastName());
        }

        dto.setPatientNotes(a.getPatientNotes());
        dto.setDoctorNotes(a.getDoctorNotes());
        dto.setVideoRoomUrl(a.getVideoRoomUrl());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }

    private PatientAppointmentDto toPatientAppointmentDto(Appointment appointment) {
        PatientAppointmentDto dto = new PatientAppointmentDto();
        dto.setId(appointment.getId());
        dto.setType(appointment.getType());
        dto.setTitle(appointment.getTitle());
        dto.setDescription(appointment.getDescription());
        dto.setDate(appointment.getStartAt());
        dto.setEndDate(appointment.getEndAt());
        dto.setStatus(appointment.getStatus());
        dto.setLocation(appointment.getLocation());
        List<String> notes = appointment.getNotes();
        dto.setNotes(notes == null ? List.of() : new ArrayList<>(notes));

        AppointmentDoctorDto doctorDto = new AppointmentDoctorDto();
        DoctorProfile doctor = appointment.getLink() != null ? appointment.getLink().getDoctor() : null;
        if (doctor != null) {
            doctorDto.setId(doctor.getId());
            doctorDto.setSpecialty(doctor.getSpeciality());
            doctorDto.setStructure(doctor.getClinicName());
            if (doctor.getUser() != null) {
                doctorDto.setFirstName(doctor.getUser().getFirstName());
                doctorDto.setLastName(doctor.getUser().getLastName());
                doctorDto.setContact(doctor.getUser().getPhone());
            }
        }
        dto.setDoctor(doctorDto);

        return dto;
    }

    private TimelineEventDto toTimelineEvent(Appointment appointment) {
        TimelineEventDto dto = new TimelineEventDto();
        dto.setDate(appointment.getStartAt());
        dto.setType(appointment.getType());

        String defaultLabel = "Appointment";
        if (appointment.getLink() != null && appointment.getLink().getDoctor() != null && appointment.getLink().getDoctor().getUser() != null) {
            String lastName = appointment.getLink().getDoctor().getUser().getLastName();
            defaultLabel = "Consultation Dr " + lastName;
        }

        dto.setLabel(appointment.getTitle() != null && !appointment.getTitle().isBlank() ? appointment.getTitle() : defaultLabel);
        dto.setDescription(appointment.getDescription());

        Instant now = Instant.now();
        if (appointment.getStatus() == Appointment.Status.COMPLETED || appointment.getStartAt().isBefore(now)) {
            dto.setStatus(TimelineEventDto.TimelineStatus.COMPLETED);
        } else if (appointment.getStartAt().minusSeconds(2 * 60 * 60).isBefore(now)) {
            dto.setStatus(TimelineEventDto.TimelineStatus.ACTIVE);
        } else {
            dto.setStatus(TimelineEventDto.TimelineStatus.UPCOMING);
        }

        return dto;
    }

    protected void autoCompletePastAppointments(UUID patientId) {
        Instant now = Instant.now();
        List<Appointment> appointments = appointmentRepository.findByLink_Patient_IdOrderByStartAtAsc(patientId);
        List<Appointment> toUpdate = new ArrayList<>();

        for (Appointment appointment : appointments) {
            if (refreshStatusIfPastInternal(appointment, now)) {
                toUpdate.add(appointment);
            }
        }

        if (!toUpdate.isEmpty()) {
            appointmentRepository.saveAll(toUpdate);
        }
    }

    protected void refreshStatusIfPast(Appointment appointment) {
        if (refreshStatusIfPastInternal(appointment, Instant.now())) {
            appointmentRepository.save(appointment);
        }
    }

    private boolean refreshStatusIfPastInternal(Appointment appointment, Instant now) {
        if (appointment.getStartAt() == null) return false;

        boolean canAutoComplete = appointment.getStatus() == Appointment.Status.UPCOMING
                || appointment.getStatus() == Appointment.Status.CONFIRMED
                || appointment.getStatus() == Appointment.Status.REQUESTED;

        if (canAutoComplete && appointment.getStartAt().isBefore(now)) {
            appointment.setStatus(Appointment.Status.COMPLETED);
            appointment.setUpdatedAt(now);
            return true;
        }

        return false;
    }

    private void normalizeAndValidateForDatabase(Appointment appointment, String source) {
        Appointment.AppointmentType type = appointment.getType() != null
                ? appointment.getType()
                : Appointment.AppointmentType.OTHER;
        appointment.setType(type);

        Appointment.Status normalized = normalizeStatusForDatabase(appointment.getStatus());
        if (appointment.getStatus() != normalized) {
            LOG.warn("Normalisation status RDV dans {}: {} -> {} (type={})",
                    source,
                    appointment.getStatus(),
                    normalized,
                    type);
            appointment.setStatus(normalized);
        }

        validateStatusTypeCombination(type, appointment.getStatus());
    }

    private Appointment.Status normalizeStatusForDatabase(Appointment.Status status) {
        if (status == null) {
            return Appointment.Status.REQUESTED;
        }
        // Compatibilité métier frontend (UPCOMING) -> valeur persistable DB.
        if (status == Appointment.Status.UPCOMING) {
            return Appointment.Status.REQUESTED;
        }
        return status;
    }

    private void validateStatusTypeCombination(Appointment.AppointmentType type, Appointment.Status status) {
        Set<Appointment.Status> allowed = ALLOWED_STATUS_BY_TYPE.get(type);
        if (allowed == null || !allowed.contains(status) || !DB_ALLOWED_STATUSES.contains(status)) {
            throw new BadRequestException("Combinaison status/type invalide pour insertion: status=" + status + ", type=" + type);
        }
    }

    private void validateFutureStart(Instant startAt) {
        if (startAt != null && !startAt.isAfter(Instant.now())) {
            throw new BadRequestException("Le rendez-vous doit être planifié dans le futur.");
        }
    }

    private UUID resolveDoctorProfileId(UUID doctorIdOrUserId) {
        if (doctorIdOrUserId == null) return null;
        if (doctorProfileRepository.existsById(doctorIdOrUserId)) return doctorIdOrUserId;
        return doctorProfileRepository.findByUser_Id(doctorIdOrUserId)
                .map(DoctorProfile::getId)
                .orElse(doctorIdOrUserId);
    }

    private UUID resolvePatientProfileId(UUID patientIdOrUserId) {
        if (patientIdOrUserId == null) return null;
        if (patientProfileRepository.existsById(patientIdOrUserId)) return patientIdOrUserId;
        return patientProfileRepository.findByUser_Id(patientIdOrUserId)
                .map(PatientProfile::getId)
                .orElse(patientIdOrUserId);
    }
}
