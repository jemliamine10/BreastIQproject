package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.*;
import com.breastcancer.breastcancerbackend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiChatService {

    private static final Logger LOG = LoggerFactory.getLogger(AiChatService.class);
    private static final String CHAT_PATH = "/api/chat";
    private static final int MAX_RECOMMENDED_DOCTORS = 6;
    private static final int MAX_ALERTS = 5;

    private static final String SYSTEM_PROMPT =
        "Tu es un assistant IA empathique et médicalement informé, spécialisé dans la santé du sein et l'éducation sur le cancer du sein.\n\n"
        + "Ton rôle :\n"
        + "- Aider les utilisatrices à comprendre les symptômes et termes médicaux\n"
        + "- Poser des questions de suivi avant de donner des explications\n"
        + "- Fournir un soutien émotionnel et rassurant\n"
        + "- Suggérer quel médecin consulter (généraliste, gynécologue, oncologue, radiologue)\n"
        + "- Utiliser les données patient fournies dans le contexte pour personnaliser tes réponses\n\n"
        + "Règles :\n"
        + "- Ne jamais diagnostiquer\n"
        + "- Ne jamais créer de peur ou de panique\n"
        + "- Utiliser 'cela pourrait être...' au lieu de certitudes\n"
        + "- Toujours encourager la consultation d'un vrai médecin\n"
        + "- Répondre TOUJOURS en français\n\n"
        + "Format de réponse :\n"
        + "1. Empathie\n"
        + "2. Question de clarification\n"
        + "3. Explication possible\n"
        + "4. Prochaine étape suggérée";

    private final RestTemplate ollamaRestTemplate;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AvailabilityRepository availabilityRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AppointmentRepository appointmentRepository;
    private final TreatmentRepository treatmentRepository;
    private final AlertRepository alertRepository;
    private final PatientDoctorLinkRepository linkRepository;
    private final Map<String, List<OllamaMessage>> conversationHistory = new ConcurrentHashMap<>();

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:qwen2.5}")
    private String ollamaModel;

    public AiChatService(
            @Qualifier("ollamaRestTemplate") RestTemplate ollamaRestTemplate,
            DoctorProfileRepository doctorProfileRepository,
            AvailabilityRepository availabilityRepository,
            PatientProfileRepository patientProfileRepository,
            AppointmentRepository appointmentRepository,
            TreatmentRepository treatmentRepository,
            AlertRepository alertRepository,
            PatientDoctorLinkRepository linkRepository
    ) {
        this.ollamaRestTemplate = ollamaRestTemplate;
        this.doctorProfileRepository = doctorProfileRepository;
        this.availabilityRepository = availabilityRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.appointmentRepository = appointmentRepository;
        this.treatmentRepository = treatmentRepository;
        this.alertRepository = alertRepository;
        this.linkRepository = linkRepository;
    }

    public ChatResponse chat(ChatRequest request) {
        validateRequest(request);

        String sessionId = request.getSessionId().trim();
        String userMessage = request.getMessage().trim();
        String patientUserId = request.getPatientUserId();

        List<OllamaMessage> history = conversationHistory.computeIfAbsent(
                sessionId,
                key -> Collections.synchronizedList(new ArrayList<>())
        );

        synchronized (history) {
            history.add(new OllamaMessage("user", userMessage));

            DoctorContext doctorContext = buildDoctorContext(userMessage);
            PatientContext patientContext = buildPatientContext(patientUserId);

            OllamaRequest ollamaRequest = new OllamaRequest();
            ollamaRequest.setModel(ollamaModel);
            ollamaRequest.setStream(false);
            ollamaRequest.setMessages(buildMessages(history, doctorContext.contextPrompt, patientContext.contextPrompt));

            OllamaResponse response = callOllama(ollamaRequest);
            String reply = extractReply(response);

            history.add(new OllamaMessage("assistant", reply));

            ChatResponse chatResponse = new ChatResponse(reply, doctorContext.suggestions);
            chatResponse.setNextAppointment(patientContext.nextAppointment);
            chatResponse.setActiveTreatments(patientContext.activeTreatments);
            chatResponse.setRecentAlerts(patientContext.recentAlerts);
            chatResponse.setConnectedDoctors(patientContext.connectedDoctors);
            return chatResponse;
        }
    }

    // ==================== VALIDATION ====================

    private void validateRequest(ChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Chat request is required.");
        }
        if (request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId is required.");
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("message is required.");
        }
    }

    // ==================== MESSAGE BUILDING ====================

    private List<OllamaMessage> buildMessages(List<OllamaMessage> history, String doctorContextPrompt, String patientContextPrompt) {
        List<OllamaMessage> messages = new ArrayList<>(history.size() + 3);
        messages.add(new OllamaMessage("system", SYSTEM_PROMPT));
        messages.add(new OllamaMessage("system", doctorContextPrompt));
        if (patientContextPrompt != null && !patientContextPrompt.isBlank()) {
            messages.add(new OllamaMessage("system", patientContextPrompt));
        }
        messages.addAll(history);
        return messages;
    }

    private DoctorContext buildDoctorContext(String userMessage) {
        DoctorProfile.DoctorType preferredType = inferDoctorType(userMessage);

        // 4-level fallback: verified+type → verified all → any+type → any all
        List<DoctorProfile> doctors = List.of();
        if (preferredType != null) {
            doctors = doctorProfileRepository.findTop6ByDoctorTypeAndVerifiedTrueOrderByVerifiedAtDesc(preferredType);
            if (doctors.isEmpty()) {
                doctors = doctorProfileRepository.findTop6ByDoctorTypeOrderByIdDesc(preferredType);
            }
        }
        if (doctors.isEmpty()) {
            doctors = doctorProfileRepository.findTop6ByVerifiedTrueOrderByVerifiedAtDesc();
        }
        if (doctors.isEmpty()) {
            doctors = doctorProfileRepository.findTop6ByOrderByIdDesc();
        }

        if (doctors.isEmpty()) {
            return new DoctorContext(
                    "Contexte médecins : aucun médecin trouvé dans la base de données. Dis à l'utilisateur qu'aucun profil médecin n'est encore enregistré sur la plateforme.",
                    List.of()
            );
        }

        LocalDate today = LocalDate.now();
        DayOfWeek todayDay = today.getDayOfWeek();
        List<String> availableToday = new ArrayList<>();
        List<String> fallbackDoctors = new ArrayList<>();
        List<DoctorSuggestionDto> suggestions = new ArrayList<>();

        for (DoctorProfile doctor : doctors) {
            boolean availableTodayFlag = isDoctorAvailableToday(doctor.getId(), todayDay);
            suggestions.add(toSuggestion(doctor, availableTodayFlag));

            String doctorSummary = formatDoctorSummary(doctor, todayDay, availableTodayFlag);
            if (availableTodayFlag) {
                availableToday.add(doctorSummary);
            } else {
                fallbackDoctors.add(doctorSummary);
            }
        }

        StringBuilder context = new StringBuilder();
        context.append("IMPORTANT — Voici les médecins RÉELS de notre plateforme. ");
        context.append("Quand l'utilisateur demande un médecin (radiologue, oncologue, etc.), ");
        context.append("tu DOIS recommander DIRECTEMENT les médecins de cette liste par leur nom, spécialité et disponibilité. ");
        context.append("NE PAS donner de conseils génériques comme 'consultez l'Ordre des Médecins'. ");
        context.append("NE PAS dire qu'il n'y a pas de médecin si cette liste est non vide. ");
        context.append("Dis à l'utilisateur qu'il peut ajouter le médecin via le bouton '+' sur sa carte ou via 'Mes Médecins'.\n\n");
        context.append("Aujourd'hui : ").append(today).append(" (").append(todayDay).append(")\n");

        if (!availableToday.isEmpty()) {
            context.append("Médecins disponibles aujourd'hui :\n");
            appendLimited(context, availableToday, MAX_RECOMMENDED_DOCTORS);
        }

        if (!fallbackDoctors.isEmpty()) {
            context.append("Autres médecins inscrits :\n");
            appendLimited(context, fallbackDoctors, MAX_RECOMMENDED_DOCTORS);
        }

        return new DoctorContext(context.toString(), suggestions);
    }

    // ==================== PATIENT CONTEXT (NEW) ====================

    private PatientContext buildPatientContext(String patientUserId) {
        if (patientUserId == null || patientUserId.isBlank()) {
            return PatientContext.empty();
        }

        UUID userId;
        try {
            userId = UUID.fromString(patientUserId.trim());
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid patientUserId for chat context: {}", patientUserId);
            return PatientContext.empty();
        }

        Optional<PatientProfile> optPatient = patientProfileRepository.findByUser_Id(userId);
        if (optPatient.isEmpty()) {
            return PatientContext.empty();
        }

        PatientProfile patient = optPatient.get();
        UUID patientId = patient.getId();
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Contexte patient connecté :\n");
        contextBuilder.append("- Score de santé : ").append(patient.getHealthScore()).append("/100\n");
        contextBuilder.append("- Statut : ").append(patient.getPatientStatus()).append("\n");

        // --- Next appointment ---
        ChatAppointmentDto nextAppt = null;
        try {
            Set<Appointment.Status> upcomingStatuses = Set.of(
                    Appointment.Status.UPCOMING, Appointment.Status.CONFIRMED, Appointment.Status.REQUESTED
            );
            Optional<Appointment> optAppt = appointmentRepository
                    .findFirstByLink_Patient_IdAndStatusInAndStartAtAfterOrderByStartAtAsc(
                            patientId, upcomingStatuses, Instant.now()
                    );
            if (optAppt.isPresent()) {
                Appointment appt = optAppt.get();
                nextAppt = new ChatAppointmentDto();
                nextAppt.setId(appt.getId().toString());
                nextAppt.setTitle(appt.getTitle() != null ? appt.getTitle() : appt.getType().name());
                nextAppt.setStartAt(appt.getStartAt().toString());
                nextAppt.setEndAt(appt.getEndAt().toString());
                nextAppt.setType(appt.getType().name());
                nextAppt.setStatus(appt.getStatus().name());
                nextAppt.setLocation(appt.getLocation());

                DoctorProfile doc = appt.getDoctor();
                if (doc != null && doc.getUser() != null) {
                    nextAppt.setDoctorName("Dr. " + safe(doc.getUser().getFirstName()) + " " + safe(doc.getUser().getLastName()));
                }

                contextBuilder.append("- Prochain RDV : ").append(nextAppt.getStartAt())
                        .append(" avec ").append(nextAppt.getDoctorName() != null ? nextAppt.getDoctorName() : "un médecin")
                        .append(" (").append(nextAppt.getType()).append(")\n");
            } else {
                contextBuilder.append("- Aucun rendez-vous à venir\n");
            }
        } catch (Exception e) {
            LOG.warn("Error fetching next appointment for chat context: {}", e.getMessage());
        }

        // --- Active treatments ---
        List<ChatTreatmentDto> treatments = new ArrayList<>();
        try {
            List<Treatment> activeTreatments = treatmentRepository.findByPatient_IdAndStatus(patientId, Treatment.Status.ACTIVE);
            for (Treatment t : activeTreatments) {
                ChatTreatmentDto dto = new ChatTreatmentDto();
                dto.setId(t.getId().toString());
                dto.setTreatmentType(t.getTreatmentType().name());
                dto.setProtocol(t.getProtocol());
                dto.setStatus(t.getStatus().name());
                dto.setCurrentCycle(t.getCurrentCycle());
                dto.setTotalCycles(t.getCyclesTotal());
                dto.setStartDate(t.getStartDate() != null ? t.getStartDate().toString() : null);
                dto.setEndDate(t.getEndDate() != null ? t.getEndDate().toString() : null);
                treatments.add(dto);

                contextBuilder.append("- Traitement actif : ").append(t.getTreatmentType())
                        .append(" (").append(safe(t.getProtocol())).append(")")
                        .append(" cycle ").append(t.getCurrentCycle()).append("/").append(t.getCyclesTotal())
                        .append("\n");
            }
            if (activeTreatments.isEmpty()) {
                contextBuilder.append("- Aucun traitement actif\n");
            }
        } catch (Exception e) {
            LOG.warn("Error fetching treatments for chat context: {}", e.getMessage());
        }

        // --- Recent alerts ---
        List<ChatAlertDto> alerts = new ArrayList<>();
        try {
            List<Alert> unresolvedAlerts = alertRepository.findByPatient_IdAndResolvedFalseOrderByCreatedAtDesc(patientId);
            int limit = Math.min(unresolvedAlerts.size(), MAX_ALERTS);
            for (int i = 0; i < limit; i++) {
                Alert a = unresolvedAlerts.get(i);
                ChatAlertDto dto = new ChatAlertDto();
                dto.setId(a.getId().toString());
                dto.setSeverity(a.getSeverity().name());
                dto.setAlertType(a.getAlertType().name());
                dto.setMessage(a.getMessage());
                dto.setCreatedAt(a.getCreatedAt().toString());
                alerts.add(dto);

                contextBuilder.append("- Alerte (").append(a.getSeverity()).append(") : ").append(a.getMessage()).append("\n");
            }
            if (unresolvedAlerts.isEmpty()) {
                contextBuilder.append("- Aucune alerte non résolue\n");
            }
        } catch (Exception e) {
            LOG.warn("Error fetching alerts for chat context: {}", e.getMessage());
        }

        // --- Connected doctors ---
        List<DoctorSuggestionDto> connectedDocs = new ArrayList<>();
        try {
            List<PatientDoctorLink> activeLinks = linkRepository.findByPatient_IdAndStatus(patientId, PatientDoctorLink.Status.ACTIVE);
            for (PatientDoctorLink link : activeLinks) {
                DoctorProfile doc = link.getDoctor();
                if (doc != null) {
                    boolean avail = isDoctorAvailableToday(doc.getId(), LocalDate.now().getDayOfWeek());
                    connectedDocs.add(toSuggestion(doc, avail));
                    String name = doc.getUser() != null
                            ? "Dr. " + safe(doc.getUser().getFirstName()) + " " + safe(doc.getUser().getLastName())
                            : "Médecin inconnu";
                    contextBuilder.append("- Médecin connecté : ").append(name)
                            .append(" (").append(safe(doc.getSpeciality())).append(")\n");
                }
            }
            if (activeLinks.isEmpty()) {
                contextBuilder.append("- Aucun médecin connecté\n");
            }
        } catch (Exception e) {
            LOG.warn("Error fetching connected doctors for chat context: {}", e.getMessage());
        }

        contextBuilder.append("Utilise ces informations pour personnaliser tes réponses. ");
        contextBuilder.append("Si le patient demande ses RDV, traitements, alertes ou médecins, utilise ces données réelles.");

        return new PatientContext(contextBuilder.toString(), nextAppt, treatments, alerts, connectedDocs);
    }

    // ==================== DOCTOR TYPE INFERENCE (FR+EN) ====================

    private DoctorProfile.DoctorType inferDoctorType(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        String m = message.toLowerCase();

        // Oncologie
        if (m.contains("oncolog") || m.contains("cancer") || m.contains("tumeur") || m.contains("chimio")) {
            return DoctorProfile.DoctorType.ONCOLOGIST;
        }
        // Radiologie
        if (m.contains("radio") || m.contains("mammogra") || m.contains("imaging") || m.contains("imagerie")
                || m.contains("échograph") || m.contains("echograph") || m.contains("irm") || m.contains("scanner")) {
            return DoctorProfile.DoctorType.RADIOLOGIST;
        }
        // Gynécologie
        if (m.contains("gyne") || m.contains("gyné") || m.contains("gynéco") || m.contains("gyno")) {
            return DoctorProfile.DoctorType.GYNECOLOGIST;
        }
        // Chirurgie
        if (m.contains("chirurg") || m.contains("surge") || m.contains("opéra") || m.contains("opera")
                || m.contains("surgery") || m.contains("biopsie")) {
            return DoctorProfile.DoctorType.SURGEON;
        }
        // Psychologie
        if (m.contains("psych") || m.contains("anxié") || m.contains("stress") || m.contains("dépression")
                || m.contains("depression") || m.contains("moral") || m.contains("soutien")) {
            return DoctorProfile.DoctorType.PSYCHOLOGIST;
        }
        // Généraliste
        if (m.contains("généraliste") || m.contains("generaliste") || m.contains("general") || m.contains("gp")
                || m.contains("médecin traitant") || m.contains("medecin traitant") || m.contains("family doctor")) {
            return DoctorProfile.DoctorType.GENERAL_PRACTITIONER;
        }
        // Pathologie
        if (m.contains("patholog") || m.contains("anatomo")) {
            return DoctorProfile.DoctorType.PATHOLOGIST;
        }

        return null;
    }

    // ==================== HELPERS ====================

    private void appendLimited(StringBuilder builder, List<String> lines, int maxItems) {
        int limit = Math.min(lines.size(), maxItems);
        for (int i = 0; i < limit; i++) {
            builder.append("- ").append(lines.get(i)).append("\n");
        }
    }

    private String formatDoctorSummary(DoctorProfile doctor, DayOfWeek todayDay, boolean availableToday) {
        String fullName = "Dr. " + safe(doctor.getUser() != null ? doctor.getUser().getFirstName() : null)
                + " " + safe(doctor.getUser() != null ? doctor.getUser().getLastName() : null);

        List<Availability> todayAvailability = availableToday
            ? availabilityRepository.findByDoctor_IdAndIsActiveTrueAndDayOfWeekOrderByStartHourAsc(doctor.getId(), todayDay)
            : List.of();

        String availabilityText;
        if (todayAvailability.isEmpty()) {
            availabilityText = "pas de créneau aujourd'hui";
        } else {
            LocalTime start = todayAvailability.get(0).getStartHour();
            LocalTime end = todayAvailability.get(todayAvailability.size() - 1).getEndHour();
            availabilityText = "créneaux aujourd'hui " + start + "-" + end;
        }

        return String.format(
                "%s | type=%s | spécialité=%s | clinique=%s | mode=%s | %s",
                fullName.trim(),
                doctor.getDoctorType(),
                safe(doctor.getSpeciality()),
                safe(doctor.getClinicName()),
                doctor.getConsultationMode(),
                availabilityText
        );
    }

    private DoctorSuggestionDto toSuggestion(DoctorProfile doctor, boolean availableToday) {
        DoctorSuggestionDto dto = new DoctorSuggestionDto();
        dto.setDoctorProfileId(doctor.getId());
        dto.setUserId(doctor.getUser() != null ? doctor.getUser().getId() : null);
        dto.setFullName(("Dr. "
                + safe(doctor.getUser() != null ? doctor.getUser().getFirstName() : null)
                + " "
                + safe(doctor.getUser() != null ? doctor.getUser().getLastName() : null)).trim());
        dto.setSpeciality(safe(doctor.getSpeciality()));
        dto.setImageUrl(doctor.getUser() != null ? doctor.getUser().getProfilePhotoUrl() : null);
        dto.setConsultationMode(doctor.getConsultationMode() != null ? doctor.getConsultationMode().name() : null);
        dto.setAvailableToday(availableToday);
        return dto;
    }

    private boolean isDoctorAvailableToday(UUID doctorId, DayOfWeek todayDay) {
        return !availabilityRepository
                .findByDoctor_IdAndIsActiveTrueAndDayOfWeekOrderByStartHourAsc(doctorId, todayDay)
                .isEmpty();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "n/a" : value;
    }

    // ==================== CONTEXT CLASSES ====================

    private static final class DoctorContext {
        private final String contextPrompt;
        private final List<DoctorSuggestionDto> suggestions;

        private DoctorContext(String contextPrompt, List<DoctorSuggestionDto> suggestions) {
            this.contextPrompt = contextPrompt;
            this.suggestions = suggestions;
        }
    }

    private static final class PatientContext {
        private final String contextPrompt;
        private final ChatAppointmentDto nextAppointment;
        private final List<ChatTreatmentDto> activeTreatments;
        private final List<ChatAlertDto> recentAlerts;
        private final List<DoctorSuggestionDto> connectedDoctors;

        private PatientContext(String contextPrompt, ChatAppointmentDto nextAppointment,
                               List<ChatTreatmentDto> activeTreatments, List<ChatAlertDto> recentAlerts,
                               List<DoctorSuggestionDto> connectedDoctors) {
            this.contextPrompt = contextPrompt;
            this.nextAppointment = nextAppointment;
            this.activeTreatments = activeTreatments;
            this.recentAlerts = recentAlerts;
            this.connectedDoctors = connectedDoctors;
        }

        static PatientContext empty() {
            return new PatientContext(null, null, List.of(), List.of(), List.of());
        }
    }

    // ==================== OLLAMA COMMUNICATION ====================

    private OllamaResponse callOllama(OllamaRequest request) {
        String endpoint = normalizeBaseUrl(ollamaUrl) + CHAT_PATH;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<OllamaRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<OllamaResponse> response = ollamaRestTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    entity,
                    OllamaResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Ollama returned an empty response.");
            }

            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            throw new IllegalStateException(
                    "Ollama returned HTTP " + ex.getStatusCode().value() + (body == null || body.isBlank() ? "" : ": " + body),
                    ex
            );
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                throw new IllegalStateException("Ollama request timed out. Please try again shortly.", ex);
            }
            if (isConnectionProblem(ex)) {
                throw new IllegalStateException("Ollama is not running or is unreachable at " + normalizeBaseUrl(ollamaUrl) + ".", ex);
            }
            throw new IllegalStateException("Unable to reach Ollama at " + normalizeBaseUrl(ollamaUrl) + ".", ex);
        } catch (Exception ex) {
            LOG.error("Unexpected Ollama chat error: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Unexpected error while contacting Ollama.", ex);
        }
    }

    private String extractReply(OllamaResponse response) {
        if (response == null || response.getMessage() == null || response.getMessage().getContent() == null) {
            throw new IllegalStateException("Ollama returned no assistant message.");
        }

        String reply = response.getMessage().getContent().trim();
        if (reply.isEmpty()) {
            throw new IllegalStateException("Ollama returned an empty assistant message.");
        }

        return reply;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:11434";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private boolean isTimeout(Throwable throwable) {
        return hasCause(throwable, SocketTimeoutException.class)
                || hasCause(throwable, java.io.InterruptedIOException.class)
                || messageContains(throwable, "timed out");
    }

    private boolean isConnectionProblem(Throwable throwable) {
        return hasCause(throwable, ConnectException.class)
                || messageContains(throwable, "connection refused")
                || messageContains(throwable, "unreachable");
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean messageContains(Throwable throwable, String token) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains(token.toLowerCase())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}