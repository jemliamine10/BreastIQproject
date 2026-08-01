package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.AvailabilityException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, UUID> {

    List<AvailabilityException> findByDoctor_IdAndIsActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID doctorId,
            LocalDate dateLowerBound,
            LocalDate dateUpperBound
    );

    List<AvailabilityException> findByDoctor_IdOrderByStartDateDesc(UUID doctorId);
}
