package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.PatientDashboardDto;
import com.breastcancer.breastcancerbackend.dto.PatientDashboardDto.*;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.entity.ChatMessage.MessageStatus;
import com.breastcancer.breastcancerbackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PatientDashboardService {

    private final PatientProfileRepository patientRepo;
    private final AppointmentRepository appointmentRepo;
    private final TrackerEntryRepository trackerRepo;
    private final TreatmentRepository treatmentRepo;
    private final TreatmentSessionRepository sessionRepo;
    private final AlertRepository alertRepo;
    private final MedicalEventRepository eventRepo;
    private final MedicalDocumentRepository documentRepo;
    private final ChatMessageRepository chatRepo;

    public PatientDashboardService(PatientProfileRepository patientRepo,
                                    AppointmentRepository appointmentRepo,
                                    TrackerEntryRepository trackerRepo,
                                    TreatmentRepository treatmentRepo,
                                    TreatmentSessionRepository sessionRepo,
                                    AlertRepository alertRepo,
                                    MedicalEventRepository eventRepo,
                                    MedicalDocumentRepository documentRepo,
                                    ChatMessageRepository chatRepo) {
        this.patientRepo = patientRepo;
        this.appointmentRepo = appointmentRepo;
        this.trackerRepo = trackerRepo;
        this.treatmentRepo = treatmentRepo;
        this.sessionRepo = sessionRepo;
        this.alertRepo = alertRepo;
        this.eventRepo = eventRepo;
        this.documentRepo = documentRepo;
        this.chatRepo = chatRepo;
    }

    public PatientDashboardDto getDashboard(UUID patientProfileId) {
        PatientProfile patient = patientRepo.findById(patientProfileId)
                .orElseThrow(() -> new ServiceExceptions.NotFoundException("Patient profile not found"));

        UUID userId = patient.getUser().getId();
        PatientDashboardDto dto = new PatientDashboardDto();

        // ── KPIs ──
        dto.setHealthScore(patient.getHealthScore() != null ? patient.getHealthScore() : 100);
        dto.setPatientStatus(patient.getPatientStatus() != null ? patient.getPatientStatus().name() : "STABLE");
        dto.setProfileCompletion(patient.getProfileCompletion() != null ? patient.getProfileCompletion() : 0);

        List<Treatment> activeTreatments = treatmentRepo.findByPatient_IdAndStatus(patientProfileId, Treatment.Status.ACTIVE);
        dto.setActiveTreatmentCount(activeTreatments.size());

        dto.setUnresolvedAlerts(alertRepo.countByPatient_IdAndResolvedFalse(patientProfileId));
        dto.setUnreadMessages(chatRepo.countUnreadByRecipient(userId, MessageStatus.READ));

        List<MedicalDocument> docs = documentRepo.findByPatient_IdAndDeletedFalseOrderByUploadDateDesc(patientProfileId);
        dto.setDocumentCount(docs.size());

        // ── Next Appointment ──
        Set<Appointment.Status> upcomingStatuses = Set.of(
                Appointment.Status.UPCOMING, Appointment.Status.CONFIRMED, Appointment.Status.REQUESTED);
        appointmentRepo.findFirstByLink_Patient_IdAndStatusInAndStartAtAfterOrderByStartAtAsc(
                patientProfileId, upcomingStatuses, Instant.now()
        ).ifPresent(appt -> {
            AppointmentInfo info = new AppointmentInfo();
            info.setId(appt.getId());
            info.setTitle(appt.getTitle());
            info.setStartAt(appt.getStartAt());
            info.setEndAt(appt.getEndAt());
            info.setType(appt.getType().name());
            info.setStatus(appt.getStatus().name());
            info.setMode(appt.getMode().name());
            info.setLocation(appt.getLocation());

            DoctorProfile doc = appt.getDoctor();
            if (doc != null && doc.getUser() != null) {
                info.setDoctorFirstName(doc.getUser().getFirstName());
                info.setDoctorLastName(doc.getUser().getLastName());
                info.setSpeciality(doc.getSpeciality());
            }
            dto.setNextAppointment(info);
        });

        // ── Tracker Trend (last 7 days) ──
        Instant sevenDaysAgo = Instant.now().minus(Duration.ofDays(7));
        List<TrackerEntry> recentEntries = trackerRepo.findByPatient_IdAndRecordedAtAfterOrderByRecordedAtAsc(
                patientProfileId, sevenDaysAgo);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE", Locale.FRENCH);
        ZoneId zone = ZoneId.systemDefault();

        // Build 7 day buckets
        List<TrackerPoint> trackerTrend = new ArrayList<>();
        LocalDate today = LocalDate.now(zone);
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String label = i == 0 ? "Auj" : d.format(fmt);

            // Find entries for this day
            final LocalDate targetDate = d;
            Optional<TrackerEntry> dayEntry = recentEntries.stream()
                    .filter(e -> e.getRecordedAt().atZone(zone).toLocalDate().equals(targetDate))
                    .reduce((first, second) -> second); // last entry of day

            if (dayEntry.isPresent()) {
                TrackerEntry e = dayEntry.get();
                trackerTrend.add(new TrackerPoint(label, e.getPainLevel(), e.getFatigueLevel(), e.getMoodLevel(), e.getTemperature()));
            } else {
                trackerTrend.add(new TrackerPoint(label, null, null, null, null));
            }
        }
        dto.setTrackerTrend(trackerTrend);

        // ── Current Treatment Progress ──
        if (!activeTreatments.isEmpty()) {
            Treatment mainTreatment = activeTreatments.get(0);
            TreatmentProgress progress = new TreatmentProgress();
            progress.setId(mainTreatment.getId());
            progress.setTreatmentType(mainTreatment.getTreatmentType().name());
            progress.setProtocol(mainTreatment.getProtocol());
            progress.setStatus(mainTreatment.getStatus().name());
            progress.setCurrentCycle(mainTreatment.getCurrentCycle() != null ? mainTreatment.getCurrentCycle() : 0);
            progress.setTotalCycles(mainTreatment.getCyclesTotal() != null ? mainTreatment.getCyclesTotal() : 0);
            if (mainTreatment.getStartDate() != null) {
                progress.setStartDate(mainTreatment.getStartDate().toString());
            }
            if (mainTreatment.getEndDate() != null) {
                progress.setEndDate(mainTreatment.getEndDate().toString());
            }

            // Session counts
            List<TreatmentSession> sessions = sessionRepo.findByTreatment_IdOrderBySessionNumberAsc(mainTreatment.getId());
            progress.setTotalSessions(sessions.size());
            progress.setCompletedSessions(sessions.stream()
                    .filter(s -> s.getStatus() == TreatmentSession.SessionStatus.DONE)
                    .count());

            dto.setCurrentTreatment(progress);
        }

        // ── Recent Timeline (limit 8) ──
        List<MedicalEvent> events = eventRepo.findByPatient_IdOrderByEventDateDesc(patientProfileId);
        List<TimelineItem> timeline = events.stream()
                .limit(8)
                .map(e -> {
                    TimelineItem item = new TimelineItem();
                    item.setId(e.getId());
                    item.setEventType(e.getEventType().name());
                    item.setTitle(e.getTitle());
                    item.setDescription(e.getDescription());
                    item.setSeverity(e.getSeverity());
                    item.setEventDate(e.getEventDate());
                    return item;
                })
                .collect(Collectors.toList());
        dto.setRecentTimeline(timeline);

        // ── Recent Alerts (unresolved, limit 5) ──
        List<Alert> unresolvedAlerts = alertRepo.findByPatient_IdAndResolvedFalseOrderByCreatedAtDesc(patientProfileId);
        List<AlertInfo> alertInfos = unresolvedAlerts.stream()
                .limit(5)
                .map(a -> {
                    AlertInfo ai = new AlertInfo();
                    ai.setId(a.getId());
                    ai.setSeverity(a.getSeverity().name());
                    ai.setAlertType(a.getAlertType().name());
                    ai.setMessage(a.getMessage());
                    ai.setCreatedAt(a.getCreatedAt());
                    return ai;
                })
                .collect(Collectors.toList());
        dto.setRecentAlerts(alertInfos);

        return dto;
    }
}
