package com.breastcancer.breastcancerbackend.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DoctorCalendarDto {

    private UUID doctorId;
    private LocalDate date;
    private String timezone;
    private List<CalendarSlotDto> slots = new ArrayList<>();

    public UUID getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(UUID doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public List<CalendarSlotDto> getSlots() {
        return slots;
    }

    public void setSlots(List<CalendarSlotDto> slots) {
        this.slots = slots;
    }
}
