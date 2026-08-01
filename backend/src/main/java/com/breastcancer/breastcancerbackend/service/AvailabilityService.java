package com.breastcancer.breastcancerbackend.service;

import com.breastcancer.breastcancerbackend.dto.*;
import com.breastcancer.breastcancerbackend.entity.Availability;
import com.breastcancer.breastcancerbackend.entity.AvailabilityException;
import com.breastcancer.breastcancerbackend.entity.DoctorProfile;
import com.breastcancer.breastcancerbackend.repository.AvailabilityExceptionRepository;
import com.breastcancer.breastcancerbackend.repository.AvailabilityRepository;
import com.breastcancer.breastcancerbackend.repository.DoctorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.BadRequestException;
import static com.breastcancer.breastcancerbackend.service.ServiceExceptions.NotFoundException;

@Service
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityExceptionRepository availabilityExceptionRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    public AvailabilityService(
            AvailabilityRepository availabilityRepository,
            AvailabilityExceptionRepository availabilityExceptionRepository,
            DoctorProfileRepository doctorProfileRepository
    ) {
        this.availabilityRepository = availabilityRepository;
        this.availabilityExceptionRepository = availabilityExceptionRepository;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    @Transactional
    public AvailabilityResponseDto create(UUID doctorId, AvailabilityUpsertRequestDto dto) {
        DoctorProfile doctor = findDoctor(doctorId);
        validateAvailabilityDto(dto);

        Availability availability = new Availability();
        availability.setDoctor(doctor);
        availability.setDayOfWeek(dto.getDayOfWeek());
        availability.setStartHour(dto.getStartHour());
        availability.setEndHour(dto.getEndHour());
        availability.setSlotDuration(dto.getSlotDuration());
        availability.setActive(dto.getIsActive() == null || dto.getIsActive());
        availability.setCreatedAt(Instant.now());
        availability.setUpdatedAt(Instant.now());

        return toDto(availabilityRepository.save(availability));
    }

    @Transactional
    public AvailabilityResponseDto update(UUID availabilityId, AvailabilityUpsertRequestDto dto) {
        Availability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new NotFoundException("Availability introuvable."));

        validateAvailabilityDto(dto);

        availability.setDayOfWeek(dto.getDayOfWeek());
        availability.setStartHour(dto.getStartHour());
        availability.setEndHour(dto.getEndHour());
        availability.setSlotDuration(dto.getSlotDuration());
        if (dto.getIsActive() != null) {
            availability.setActive(dto.getIsActive());
        }
        availability.setUpdatedAt(Instant.now());

        return toDto(availabilityRepository.save(availability));
    }

    @Transactional
    public void softDelete(UUID availabilityId) {
        Availability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new NotFoundException("Availability introuvable."));
        availability.setActive(false);
        availability.setUpdatedAt(Instant.now());
        availabilityRepository.save(availability);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponseDto> listByDoctor(UUID doctorId) {
        return availabilityRepository.findByDoctor_IdOrderByDayOfWeekAscStartHourAsc(doctorId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AvailabilityExceptionResponseDto createException(UUID doctorId, AvailabilityExceptionUpsertRequestDto dto) {
        DoctorProfile doctor = findDoctor(doctorId);
        validateAvailabilityExceptionDto(dto, true);

        AvailabilityException exception = new AvailabilityException();
        exception.setDoctor(doctor);
        exception.setStartDate(dto.getStartDate());
        exception.setEndDate(dto.getEndDate() != null ? dto.getEndDate() : dto.getStartDate());
        exception.setStartHour(dto.getStartHour());
        exception.setEndHour(dto.getEndHour());
        exception.setReason(dto.getReason());
        exception.setActive(dto.getIsActive() == null || dto.getIsActive());
        exception.setCreatedAt(Instant.now());
        exception.setUpdatedAt(Instant.now());

        return toDto(availabilityExceptionRepository.save(exception));
    }

    @Transactional
    public AvailabilityExceptionResponseDto updateException(UUID exceptionId, AvailabilityExceptionUpsertRequestDto dto) {
        AvailabilityException exception = availabilityExceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new NotFoundException("AvailabilityException introuvable."));

        validateAvailabilityExceptionDto(dto, false);

        if (dto.getStartDate() != null) {
            exception.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            exception.setEndDate(dto.getEndDate());
        }
        if (dto.getStartHour() != null || dto.getEndHour() != null) {
            exception.setStartHour(dto.getStartHour());
            exception.setEndHour(dto.getEndHour());
        }
        if (dto.getReason() != null) {
            exception.setReason(dto.getReason());
        }
        if (dto.getIsActive() != null) {
            exception.setActive(dto.getIsActive());
        }

        if (exception.getEndDate().isBefore(exception.getStartDate())) {
            throw new BadRequestException("endDate doit être >= startDate.");
        }
        if ((exception.getStartHour() == null) != (exception.getEndHour() == null)) {
            throw new BadRequestException("startHour et endHour doivent être fournis ensemble.");
        }
        if (exception.getStartHour() != null && !exception.getStartHour().isBefore(exception.getEndHour())) {
            throw new BadRequestException("startHour doit être avant endHour.");
        }

        exception.setUpdatedAt(Instant.now());
        return toDto(availabilityExceptionRepository.save(exception));
    }

    @Transactional
    public void softDeleteException(UUID exceptionId) {
        AvailabilityException exception = availabilityExceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new NotFoundException("AvailabilityException introuvable."));
        exception.setActive(false);
        exception.setUpdatedAt(Instant.now());
        availabilityExceptionRepository.save(exception);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityExceptionResponseDto> listExceptionsByDoctor(UUID doctorId) {
        return availabilityExceptionRepository.findByDoctor_IdOrderByStartDateDesc(doctorId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private DoctorProfile findDoctor(UUID doctorId) {
        if (doctorId == null) {
            throw new BadRequestException("doctorId requis.");
        }
        return doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor introuvable."));
    }

    private void validateAvailabilityDto(AvailabilityUpsertRequestDto dto) {
        if (dto == null) {
            throw new BadRequestException("Payload availability requis.");
        }
        if (dto.getDayOfWeek() == null) {
            throw new BadRequestException("dayOfWeek requis.");
        }
        if (dto.getStartHour() == null || dto.getEndHour() == null) {
            throw new BadRequestException("startHour/endHour requis.");
        }
        if (!dto.getStartHour().isBefore(dto.getEndHour())) {
            throw new BadRequestException("startHour doit être avant endHour.");
        }
        if (dto.getSlotDuration() == null || dto.getSlotDuration() <= 0) {
            throw new BadRequestException("slotDuration invalide.");
        }
        long windowMinutes = java.time.Duration.between(dto.getStartHour(), dto.getEndHour()).toMinutes();
        if (windowMinutes < dto.getSlotDuration()) {
            throw new BadRequestException("slotDuration dépasse la fenêtre de disponibilité.");
        }
    }

    private void validateAvailabilityExceptionDto(AvailabilityExceptionUpsertRequestDto dto, boolean create) {
        if (dto == null) {
            throw new BadRequestException("Payload availability exception requis.");
        }
        if (create && dto.getStartDate() == null) {
            throw new BadRequestException("startDate requis.");
        }
        if (dto.getEndDate() != null && dto.getStartDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BadRequestException("endDate doit être >= startDate.");
        }
        if ((dto.getStartHour() == null) != (dto.getEndHour() == null)) {
            throw new BadRequestException("startHour et endHour doivent être fournis ensemble.");
        }
        if (dto.getStartHour() != null && !dto.getStartHour().isBefore(dto.getEndHour())) {
            throw new BadRequestException("startHour doit être avant endHour.");
        }
    }

    private AvailabilityResponseDto toDto(Availability availability) {
        AvailabilityResponseDto dto = new AvailabilityResponseDto();
        dto.setId(availability.getId());
        dto.setDoctorId(availability.getDoctor() != null ? availability.getDoctor().getId() : null);
        dto.setDayOfWeek(availability.getDayOfWeek());
        dto.setStartHour(availability.getStartHour());
        dto.setEndHour(availability.getEndHour());
        dto.setSlotDuration(availability.getSlotDuration());
        dto.setActive(availability.isActive());
        dto.setCreatedAt(availability.getCreatedAt());
        dto.setUpdatedAt(availability.getUpdatedAt());
        return dto;
    }

    private AvailabilityExceptionResponseDto toDto(AvailabilityException exception) {
        AvailabilityExceptionResponseDto dto = new AvailabilityExceptionResponseDto();
        dto.setId(exception.getId());
        dto.setDoctorId(exception.getDoctor() != null ? exception.getDoctor().getId() : null);
        dto.setStartDate(exception.getStartDate());
        dto.setEndDate(exception.getEndDate());
        dto.setStartHour(exception.getStartHour());
        dto.setEndHour(exception.getEndHour());
        dto.setReason(exception.getReason());
        dto.setActive(exception.isActive());
        dto.setCreatedAt(exception.getCreatedAt());
        dto.setUpdatedAt(exception.getUpdatedAt());
        return dto;
    }
}
