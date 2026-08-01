package com.breastcancer.breastcancerbackend.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatResponse {

    private String reply;
    private List<DoctorSuggestionDto> doctors = new ArrayList<>();
    private ChatAppointmentDto nextAppointment;
    private List<ChatTreatmentDto> activeTreatments = new ArrayList<>();
    private List<ChatAlertDto> recentAlerts = new ArrayList<>();
    private List<DoctorSuggestionDto> connectedDoctors = new ArrayList<>();

    public ChatResponse() {
    }

    public ChatResponse(String reply) {
        this.reply = reply;
    }

    public ChatResponse(String reply, List<DoctorSuggestionDto> doctors) {
        this.reply = reply;
        this.doctors = doctors;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<DoctorSuggestionDto> getDoctors() {
        return doctors;
    }

    public void setDoctors(List<DoctorSuggestionDto> doctors) {
        this.doctors = doctors;
    }

    public ChatAppointmentDto getNextAppointment() {
        return nextAppointment;
    }

    public void setNextAppointment(ChatAppointmentDto nextAppointment) {
        this.nextAppointment = nextAppointment;
    }

    public List<ChatTreatmentDto> getActiveTreatments() {
        return activeTreatments;
    }

    public void setActiveTreatments(List<ChatTreatmentDto> activeTreatments) {
        this.activeTreatments = activeTreatments;
    }

    public List<ChatAlertDto> getRecentAlerts() {
        return recentAlerts;
    }

    public void setRecentAlerts(List<ChatAlertDto> recentAlerts) {
        this.recentAlerts = recentAlerts;
    }

    public List<DoctorSuggestionDto> getConnectedDoctors() {
        return connectedDoctors;
    }

    public void setConnectedDoctors(List<DoctorSuggestionDto> connectedDoctors) {
        this.connectedDoctors = connectedDoctors;
    }
}