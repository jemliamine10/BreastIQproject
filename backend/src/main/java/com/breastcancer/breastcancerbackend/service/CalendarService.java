package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.CalendarSlotDto;
import com.breastcancer.breastcancerbackend.dto.DoctorCalendarDto;
import com.breastcancer.breastcancerbackend.entity.Appointment;
import com.breastcancer.breastcancerbackend.entity.Availability;
import com.breastcancer.breastcancerbackend.entity.AvailabilityException;
import com.breastcancer.breastcancerbackend.entity.DoctorProfile;
import com.breastcancer.breastcancerbackend.repository.AppointmentRepository;
import com.breastcancer.breastcancerbackend.repository.AvailabilityExceptionRepository;
import com.breastcancer.breastcancerbackend.repository.AvailabilityRepository;
import com.breastcancer.breastcancerbackend.repository.DoctorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.BadRequestException;
import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.NotFoundException;

@Service
public class CalendarService {

    private static final Set<Appointment.Status> ACTIVE_STATUSES = EnumSet.of(
            Appointment.Status.REQUESTED,
            Appointment.Status.CONFIRMED,
            Appointment.Status.UPCOMING
    );

    private final DoctorProfileRepository doctorProfileRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;
    private final AppointmentRepository appointmentRepository;

    public CalendarService(
            DoctorProfileRepository doctorProfileRepository,
            AvailabilityRepository availabilityRepository,
            AvailabilityExceptionRepository availabilityExceptionRepository,
            AppointmentRepository appointmentRepository
    ) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.availabilityRepository = availabilityRepository;
        this.availabilityExceptionRepository = availabilityExceptionRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public DoctorCalendarDto getDoctorCalendar(UUID doctorId, LocalDate date) {
        if (doctorId == null) {
            throw new BadRequestException("doctorId requis.");
        }
        if (date == null) {
            throw new BadRequestException("date requise.");
        }

        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor introuvable."));

        ZoneId zoneId = resolveZone(doctor.getTimezone());

        List<Availability> availabilities = availabilityRepository
                .findByDoctor_IdAndIsActiveTrueAndDayOfWeekOrderByStartHourAsc(doctorId, date.getDayOfWeek());

        DoctorCalendarDto dto = new DoctorCalendarDto();
        dto.setDoctorId(doctorId);
        dto.setDate(date);
        dto.setTimezone(zoneId.getId());

        if (availabilities.isEmpty()) {
            dto.setSlots(List.of());
            return dto;
        }

        ZonedDateTime startOfDay = date.atStartOfDay(zoneId);
        ZonedDateTime endOfDay = startOfDay.plusDays(1);

        List<Appointment> appointments = appointmentRepository
                .findByLink_Doctor_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
                        doctorId,
                        ACTIVE_STATUSES,
                        endOfDay.toInstant(),
                        startOfDay.toInstant()
                );

        List<AvailabilityException> exceptions = availabilityExceptionRepository
                .findByDoctor_IdAndIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        doctorId,
                        date,
                        date
                );

        List<CalendarSlotDto> slots = new ArrayList<>();

        for (Availability availability : availabilities) {
            LocalTime cursor = availability.getStartHour();
            while (cursor.plusMinutes(availability.getSlotDuration()).compareTo(availability.getEndHour()) <= 0) {
                LocalTime slotEnd = cursor.plusMinutes(availability.getSlotDuration());
                Instant slotStartInstant = ZonedDateTime.of(date, cursor, zoneId).toInstant();
                Instant slotEndInstant = ZonedDateTime.of(date, slotEnd, zoneId).toInstant();

                CalendarSlotDto slot = new CalendarSlotDto();
                slot.setStartTime(slotStartInstant);
                slot.setEndTime(slotEndInstant);

                if (isBlockedByException(date, cursor, slotEnd, exceptions)) {
                    slot.setStatus(CalendarSlotDto.SlotStatus.BLOCKED);
                } else if (hasAppointmentOverlap(slotStartInstant, slotEndInstant, appointments, null)) {
                    slot.setStatus(CalendarSlotDto.SlotStatus.BOOKED);
                } else {
                    slot.setStatus(CalendarSlotDto.SlotStatus.AVAILABLE);
                }

                slots.add(slot);
                cursor = slotEnd;
            }
        }

        dto.setSlots(slots);
        return dto;
    }

    @Transactional(readOnly = true)
    public void assertSlotBookable(UUID doctorId, Instant start, Instant end, UUID excludedAppointmentId) {
        if (doctorId == null) {
            throw new BadRequestException("doctorId requis.");
        }
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BadRequestException("Créneau invalide.");
        }

        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor introuvable."));
        ZoneId zoneId = resolveZone(doctor.getTimezone());

        ZonedDateTime startZdt = start.atZone(zoneId);
        ZonedDateTime endZdt = end.atZone(zoneId);
        if (!startZdt.toLocalDate().equals(endZdt.toLocalDate())) {
            throw new BadRequestException("Le créneau doit rester dans la même journée médecin.");
        }

        LocalDate date = startZdt.toLocalDate();
        LocalTime startLocal = startZdt.toLocalTime();
        LocalTime endLocal = endZdt.toLocalTime();

        List<Availability> availabilities = availabilityRepository
                .findByDoctor_IdAndIsActiveTrueAndDayOfWeekOrderByStartHourAsc(doctorId, date.getDayOfWeek());
        if (availabilities.isEmpty()) {
            throw new BadRequestException("Aucune disponibilité configurée pour ce médecin sur ce jour.");
        }

        boolean insideAvailability = availabilities.stream().anyMatch(av -> isAlignedInsideAvailability(av, startLocal, endLocal));
        if (!insideAvailability) {
            throw new BadRequestException("Le créneau n'appartient pas aux disponibilités du médecin.");
        }

        List<AvailabilityException> exceptions = availabilityExceptionRepository
                .findByDoctor_IdAndIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        doctorId,
                        date,
                        date
                );
        if (isBlockedByException(date, startLocal, endLocal, exceptions)) {
            throw new BadRequestException("Le créneau est bloqué par une indisponibilité du médecin.");
        }

        boolean overlaps = !appointmentRepository
                .findByLink_Doctor_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
                        doctorId,
                        ACTIVE_STATUSES,
                        end,
                        start
                )
                .stream()
                .filter(existing -> excludedAppointmentId == null || !excludedAppointmentId.equals(existing.getId()))
                .toList()
                .isEmpty();

        if (overlaps) {
            throw new ServiceExceptions.ConflictException("Créneau indisponible : déjà réservé.");
        }
    }

    @Transactional(readOnly = true)
    public boolean isSlotAvailable(UUID doctorId, Instant start, Instant end, UUID excludedAppointmentId) {
        try {
            assertSlotBookable(doctorId, start, end, excludedAppointmentId);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean isAlignedInsideAvailability(Availability availability, LocalTime start, LocalTime end) {
        if (start.isBefore(availability.getStartHour()) || end.isAfter(availability.getEndHour())) {
            return false;
        }

        long slotDuration = availability.getSlotDuration();
        long fromWindowStartMinutes = Duration.between(availability.getStartHour(), start).toMinutes();
        long requestedDurationMinutes = Duration.between(start, end).toMinutes();

        return fromWindowStartMinutes >= 0
                && fromWindowStartMinutes % slotDuration == 0
                && requestedDurationMinutes > 0
                && requestedDurationMinutes % slotDuration == 0;
    }

    private boolean isBlockedByException(
            LocalDate date,
            LocalTime slotStart,
            LocalTime slotEnd,
            List<AvailabilityException> exceptions
    ) {
        for (AvailabilityException exception : exceptions) {
            if (exception.getStartDate() == null || exception.getEndDate() == null) {
                continue;
            }
            if (date.isBefore(exception.getStartDate()) || date.isAfter(exception.getEndDate())) {
                continue;
            }
            if (exception.getStartHour() == null && exception.getEndHour() == null) {
                return true;
            }
            if (exception.getStartHour() != null && exception.getEndHour() != null) {
                boolean overlaps = slotStart.isBefore(exception.getEndHour()) && slotEnd.isAfter(exception.getStartHour());
                if (overlaps) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAppointmentOverlap(
            Instant slotStart,
            Instant slotEnd,
            List<Appointment> appointments,
            UUID excludedAppointmentId
    ) {
        return appointments.stream().anyMatch(appointment -> {
            if (excludedAppointmentId != null && excludedAppointmentId.equals(appointment.getId())) {
                return false;
            }
            return slotStart.isBefore(appointment.getEndAt()) && slotEnd.isAfter(appointment.getStartAt());
        });
    }

    private ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            return ZoneOffset.UTC;
        }
    }
}
