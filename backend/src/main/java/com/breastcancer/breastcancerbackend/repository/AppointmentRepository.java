
package com.breastcancer.breastcancerbackend.repository;

import com.breastcancer.breastcancerbackend.entity.Appointment;
import com.breastcancer.breastcancerbackend.entity.Appointment.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {

    @Query("""
            select distinct a
            from Appointment a
            left join fetch a.notes
            left join fetch a.link l
            left join fetch l.patient
            left join fetch l.doctor d
            left join fetch d.user
            where a.id = :appointmentId
            """)
    Optional<Appointment> findByIdWithNotes(@Param("appointmentId") UUID appointmentId);

    // Tous les RDV d’un lien patient-doctor
    List<Appointment> findByLink_IdOrderByStartAtDesc(UUID linkId);

    // RDV futurs/past d’un lien
    List<Appointment> findByLink_IdAndStartAtAfterOrderByStartAtAsc(UUID linkId, Instant after);
    List<Appointment> findByLink_IdAndStartAtBeforeOrderByStartAtDesc(UUID linkId, Instant before);

    // RDV par statut (ex: demandes en attente)
    List<Appointment> findByLink_IdAndStatusOrderByStartAtAsc(UUID linkId, Status status);

    // ✅ EXACTEMENT comme dans AppointmentService (sans OrderBy)
    List<Appointment> findByLink_Doctor_IdAndStartAtBetween(UUID doctorId, Instant from, Instant to);

        // ✅ Alias calendrier médecin (bornes optionnelles)
        List<Appointment> findByLink_Doctor_IdOrderByStartAtAsc(UUID doctorId);
        List<Appointment> findByLink_Doctor_IdAndStartAtGreaterThanEqualOrderByStartAtAsc(UUID doctorId, Instant from);
        List<Appointment> findByLink_Doctor_IdAndStartAtLessThanEqualOrderByStartAtAsc(UUID doctorId, Instant to);

    // ✅ EXACTEMENT comme dans AppointmentService (sans OrderBy)
    List<Appointment> findByLink_Patient_IdAndStartAtBetween(UUID patientId, Instant from, Instant to);

    // ✅ Supporte Set<Status> (Collection) + utilisé dans create/update
    List<Appointment> findByLink_Doctor_IdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
            UUID doctorId,
            Collection<Status> statuses,
            Instant endExclusive,
            Instant startInclusive
    );

    // Lire RDV si tu veux vérifier l’accès (par lien)
    Optional<Appointment> findByIdAndLink_Id(UUID appointmentId, UUID linkId);

    Optional<Appointment> findByIdAndLink_Patient_Id(UUID appointmentId, UUID patientId);

    Optional<Appointment> findFirstByLink_Patient_IdAndStatusInAndStartAtAfterOrderByStartAtAsc(
            UUID patientId,
            Collection<Status> statuses,
            Instant after
    );

    List<Appointment> findByLink_Patient_IdOrderByStartAtAsc(UUID patientId);

    long countByLink_Patient_Id(UUID patientId);

    long countByLink_Patient_IdAndType(UUID patientId, Appointment.AppointmentType type);

        @Query("select count(distinct d.id) from Appointment a join a.link l join l.doctor d where l.patient.id = :patientId")
    long countDistinctDoctorByPatientId(@Param("patientId") UUID patientId);
}
