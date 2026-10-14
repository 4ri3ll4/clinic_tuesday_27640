package kigali.clinic.rw.repository;

import java.util.UUID;
import java.util.Optional;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import kigali.clinic.rw.domain.Doctor;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    Optional<Doctor> findByOfficeId(UUID officeId);

    @Query("SELECT DISTINCT d FROM Doctor d JOIN d.specializations s WHERE LOWER(s.name) = LOWER(:name)")
    List<Doctor> findDoctorsBySpecialization(@Param("name") String name);

    @Query("SELECT d FROM Doctor d WHERE d.office IS NULL ORDER BY d.lastName ASC")
    List<Doctor> findDoctorsWithoutOffice();

    boolean existsByOfficeIdAndIdNot(UUID officeId, UUID doctorId);

    // Bonus 2: Doctor workload
    @Query("SELECT d.firstName, d.lastName, COUNT(a) " +
        "FROM Appointment a JOIN a.doctor d " +
        "WHERE a.status = kigali.clinic.rw.domain.AppointmentStatus.COMPLETED " +
        "AND a.appointmentDate BETWEEN :start AND :end " +
        "GROUP BY d.id, d.firstName, d.lastName " +
        "ORDER BY COUNT(a) DESC")
    List<Object[]> findDoctorWorkload(
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );
}
