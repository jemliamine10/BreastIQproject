package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {

    List<Availability> findByDoctor_IdAndIsActiveTrueAndDayOfWeekOrderByStartHourAsc(UUID doctorId, DayOfWeek dayOfWeek);

    List<Availability> findByDoctor_IdAndIsActiveTrueOrderByDayOfWeekAscStartHourAsc(UUID doctorId);

    List<Availability> findByDoctor_IdOrderByDayOfWeekAscStartHourAsc(UUID doctorId);
}
