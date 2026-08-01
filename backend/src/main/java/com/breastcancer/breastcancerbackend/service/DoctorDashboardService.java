package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.DoctorDashboardDto;
import com.breastcancer.breastcancerbackend.dto.DoctorDashboardDto.*;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.entity.PatientProfile.PatientStatus;
import com.breastcancer.breastcancerbackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DoctorDashboardService {

    private final DoctorProfileRepository doctorRepo;
    private final PatientDoctorLinkRepository linkRepo;
    private final AlertRepository alertRepo;
    private final AppointmentRepository appointmentRepo;
    private final TreatmentRepository treatmentRepo;
    private final MedicalRecordRepository medicalRecordRepo;
    private final ChatMessageRepository chatRepo;
    private final TrackerEntryRepository trackerRepo;

    public DoctorDashboardService(DoctorProfileRepository doctorRepo,
                                   PatientDoctorLinkRepository linkRepo,
                                   AlertRepository alertRepo,
                                   AppointmentRepository appointmentRepo,
                                   TreatmentRepository treatmentRepo,
                                   MedicalRecordRepository medicalRecordRepo,
                                   ChatMessageRepository chatRepo,
                                   TrackerEntryRepository trackerRepo) {
        this.doctorRepo = doctorRepo;
        this.linkRepo = linkRepo;
        this.alertRepo = alertRepo;
        this.appointmentRepo = appointmentRepo;
        this.treatmentRepo = treatmentRepo;
        this.medicalRecordRepo = medicalRecordRepo;
        this.chatRepo = chatRepo;
        this.trackerRepo = trackerRepo;
    }

    public DoctorDashboardDto getDashboard(UUID doctorProfileId) {
        DoctorProfile doctor = doctorRepo.findById(doctorProfileId)
                .orElseThrow(() -> new ServiceExceptions.NotFoundException("Doctor profile not found"));

        UUID doctorUserId = doctor.getUser().getId();

        // ── Get active links & patients ──
        List<PatientDoctorLink> activeLinks = linkRepo.findByDoctor_IdAndStatusOrderByActivatedAtDesc(
                doctorProfileId, PatientDoctorLink.Status.ACTIVE);
        List<PatientProfile> patients = activeLinks.stream()
                .map(PatientDoctorLink::getPatient)
                .collect(Collectors.toList());
        Set<UUID> patientIds = patients.stream()
                .map(PatientProfile::getId)
                .collect(Collectors.toSet());

        DoctorDashboardDto dto = new DoctorDashboardDto();

        // ── KPIs ──
        dto.setTotalPatients(activeLinks.size());
        dto.setPendingRequests(linkRepo.findByDoctor_IdAndStatus(
                doctorProfileId, PatientDoctorLink.Status.REQUESTED).size());

        List<Alert> allUnresolved = alertRepo.findUnresolvedAlertsByDoctorId(doctorProfileId);
        dto.setUnresolvedAlerts(allUnresolved.size());
        dto.setCriticalAlerts(allUnresolved.stream()
                .filter(a -> a.getSeverity() == Alert.Severity.CRITICAL || a.getSeverity() == Alert.Severity.HIGH)
                .count());

        // Today appointments
        ZoneId zone = ZoneId.of(doctor.getTimezone() != null ? doctor.getTimezone() : "UTC");
        LocalDate today = LocalDate.now(zone);
        Instant todayStart = today.atStartOfDay(zone).toInstant();
        Instant todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();
        List<Appointment> todayAppts = appointmentRepo.findByLink_Doctor_IdAndStartAtBetween(
                doctorProfileId, todayStart, todayEnd);
        dto.setAppointmentsToday(todayAppts.size());

        // Avg health score
        double avgScore = patients.stream()
                .mapToInt(p -> p.getHealthScore() != null ? p.getHealthScore() : 100)
                .average()
                .orElse(0);
        dto.setAvgHealthScore(Math.round(avgScore * 10.0) / 10.0);

        // Unread messages
        dto.setUnreadMessages(chatRepo.countUnreadByRecipient(
                doctorUserId, ChatMessage.MessageStatus.READ));

        // ── Status Distribution ──
        Map<String, Long> statusDist = new LinkedHashMap<>();
        statusDist.put("STABLE", patients.stream().filter(p -> p.getPatientStatus() == null || p.getPatientStatus() == PatientStatus.STABLE).count());
        statusDist.put("WARNING", patients.stream().filter(p -> p.getPatientStatus() == PatientStatus.WARNING).count());
        statusDist.put("CRITICAL", patients.stream().filter(p -> p.getPatientStatus() == PatientStatus.CRITICAL).count());
        dto.setStatusDistribution(statusDist);

        // ── Stage Distribution ──
        Map<String, Long> stageDist = new LinkedHashMap<>();
        stageDist.put("STAGE_0", 0L);
        stageDist.put("STAGE_I", 0L);
        stageDist.put("STAGE_II", 0L);
        stageDist.put("STAGE_III", 0L);
        stageDist.put("STAGE_IV", 0L);
        stageDist.put("UNKNOWN", 0L);

        for (UUID pid : patientIds) {
            medicalRecordRepo.findByPatient_Id(pid).ifPresent(mr -> {
                String key = mr.getCancerStage() != null ? mr.getCancerStage().name() : "UNKNOWN";
                stageDist.merge(key, 1L, Long::sum);
            });
        }
        dto.setStageDistribution(stageDist);

        // ── Treatment Distribution (active treatments) ──
        Map<String, Long> treatDist = new LinkedHashMap<>();
        treatDist.put("CHEMO", 0L);
        treatDist.put("RADIO", 0L);
        treatDist.put("SURGERY", 0L);
        treatDist.put("HORMONAL", 0L);
        treatDist.put("IMMUNOTHERAPY", 0L);

        for (UUID pid : patientIds) {
            List<Treatment> treatments = treatmentRepo.findByPatient_IdAndStatus(pid, Treatment.Status.ACTIVE);
            for (Treatment t : treatments) {
                treatDist.merge(t.getTreatmentType().name(), 1L, Long::sum);
            }
        }
        dto.setTreatmentDistribution(treatDist);

        // ── Health Trend (last 15 days, using latest tracker entries per day) ──
        List<HealthTrendPoint> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 14; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            trend.add(new HealthTrendPoint(d.format(fmt), avgScore)); // base all same for now
        }
        // Try enriching with actual tracker data if we have patients
        if (!patientIds.isEmpty()) {
            Instant trendStart = today.minusDays(14).atStartOfDay(zone).toInstant();
            for (UUID pid : patientIds) {
                List<TrackerEntry> entries = trackerRepo.findByPatient_IdAndRecordedAtAfterOrderByRecordedAtAsc(pid, trendStart);
                for (TrackerEntry entry : entries) {
                    LocalDate entryDate = entry.getRecordedAt().atZone(zone).toLocalDate();
                    int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(today.minusDays(14), entryDate);
                    if (dayIndex >= 0 && dayIndex < trend.size()) {
                        // Use mood as an approximation of health (inverted pain)
                        int moodScore = entry.getMoodLevel() != null ? entry.getMoodLevel() * 10 : 70;
                        int painPenalty = entry.getPainLevel() != null ? entry.getPainLevel() * 5 : 0;
                        double compositeScore = Math.max(0, Math.min(100, moodScore - painPenalty + 30));
                        // Average with existing value
                        double existing = trend.get(dayIndex).getAvgScore();
                        trend.get(dayIndex).setAvgScore(Math.round(((existing + compositeScore) / 2.0) * 10.0) / 10.0);
                    }
                }
            }
        }
        dto.setHealthTrend(trend);

        // ── Recent Alerts (limit 5) ──
        List<AlertSummary> recentAlerts = allUnresolved.stream()
                .sorted(Comparator.comparing(Alert::getCreatedAt).reversed())
                .limit(5)
                .map(a -> {
                    AlertSummary as = new AlertSummary();
                    as.setId(a.getId());
                    as.setSeverity(a.getSeverity().name());
                    as.setAlertType(a.getAlertType().name());
                    as.setMessage(a.getMessage());
                    as.setPatientProfileId(a.getPatient().getId());
                    User u = a.getPatient().getUser();
                    as.setPatientName(u.getFirstName() + " " + u.getLastName());
                    as.setCreatedAt(a.getCreatedAt());
                    return as;
                })
                .collect(Collectors.toList());
        dto.setRecentAlerts(recentAlerts);

        // ── Today's Appointments ──
        List<AppointmentSummary> todayAppointments = todayAppts.stream()
                .sorted(Comparator.comparing(Appointment::getStartAt))
                .limit(8)
                .map(a -> {
                    AppointmentSummary apptSum = new AppointmentSummary();
                    apptSum.setId(a.getId());
                    apptSum.setTitle(a.getTitle());
                    apptSum.setStartAt(a.getStartAt());
                    apptSum.setEndAt(a.getEndAt());
                    apptSum.setType(a.getType().name());
                    apptSum.setStatus(a.getStatus().name());
                    apptSum.setMode(a.getMode().name());
                    PatientProfile patient = a.getPatient();
                    if (patient != null) {
                        apptSum.setPatientProfileId(patient.getId());
                        apptSum.setPatientName(patient.getUser().getFirstName() + " " + patient.getUser().getLastName());
                    }
                    return apptSum;
                })
                .collect(Collectors.toList());
        dto.setTodayAppointments(todayAppointments);

        // ── Critical patients (sorted by health score ASC — worst first) ──
        List<PatientSummary> criticalPatients = patients.stream()
                .sorted(Comparator.comparingInt(p -> p.getHealthScore() != null ? p.getHealthScore() : 100))
                .limit(8)
                .map(p -> {
                    PatientSummary ps = new PatientSummary();
                    ps.setPatientProfileId(p.getId());
                    ps.setFirstName(p.getUser().getFirstName());
                    ps.setLastName(p.getUser().getLastName());
                    ps.setHealthScore(p.getHealthScore() != null ? p.getHealthScore() : 100);
                    ps.setPatientStatus(p.getPatientStatus() != null ? p.getPatientStatus().name() : "STABLE");

                    // Find active treatment
                    List<Treatment> activeTreatments = treatmentRepo.findByPatient_IdAndStatus(p.getId(), Treatment.Status.ACTIVE);
                    if (!activeTreatments.isEmpty()) {
                        ps.setActiveTreatment(activeTreatments.get(0).getTreatmentType().name());
                    }

                    // Find cancer stage
                    medicalRecordRepo.findByPatient_Id(p.getId()).ifPresent(mr -> {
                        if (mr.getCancerStage() != null) {
                            ps.setCancerStage(mr.getCancerStage().name());
                        }
                    });

                    return ps;
                })
                .collect(Collectors.toList());
        dto.setCriticalPatients(criticalPatients);

        return dto;
    }
}
