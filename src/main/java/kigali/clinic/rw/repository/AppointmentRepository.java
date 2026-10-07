package kigali.clinic.rw.repository;

import java.util.UUID;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.domain.AppointmentStatus;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    boolean existsByDoctorId(UUID doctorId);
    boolean existsByPatientId(UUID patientId);
    List<Appointment> findByStatusOrderByAppointmentDateAsc(AppointmentStatus status);
    List<Appointment> findByAppointmentDateBetweenOrderByAppointmentDateAsc(LocalDate start, LocalDate end);
    boolean existsByDoctorIdAndAppointmentDateAndStatusNot(UUID doctorId, LocalDate appointmentDate, AppointmentStatus status);

    @Query("SELECT a.status, COUNT(a) FROM Appointment a GROUP BY a.status")
    List<Object[]> getAppointmentStatsByStatus();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Appointment a SET a.status = :cancelled WHERE a.doctor.id = :doctorId AND a.appointmentDate = :date AND a.status <> :completed")
    int cancelDoctorDay(@Param("doctorId") UUID doctorId, @Param("date") LocalDate date,
        @Param("cancelled") AppointmentStatus cancelled, @Param("completed") AppointmentStatus completed);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Appointment a WHERE a.status = :status AND a.appointmentDate < :date")
    int deleteCancelledBefore(@Param("date") LocalDate date, @Param("status") AppointmentStatus status);
}
