package com.breastcancer.breastcancerbackend.dto;

import com.breastcancer.breastcancerbackend.entity.Appointment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PatientAppointmentDto {

    private UUID id;
    private Appointment.AppointmentType type;
    private String title;
    private String description;
    private Instant date;
    private Instant endDate;
    private Appointment.Status status;
    private String location;
    private AppointmentDoctorDto doctor;
    private List<String> notes = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Appointment.AppointmentType getType() { return type; }
    public void setType(Appointment.AppointmentType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }

    public Instant getEndDate() { return endDate; }
    public void setEndDate(Instant endDate) { this.endDate = endDate; }

    public Appointment.Status getStatus() { return status; }
    public void setStatus(Appointment.Status status) { this.status = status; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public AppointmentDoctorDto getDoctor() { return doctor; }
    public void setDoctor(AppointmentDoctorDto doctor) { this.doctor = doctor; }

    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }
}
