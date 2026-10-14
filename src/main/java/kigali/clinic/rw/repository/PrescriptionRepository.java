package kigali.clinic.rw.repository;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import kigali.clinic.rw.domain.Prescription;

import org.springframework.data.jpa.repository.Query;

@Repository
public interface PrescriptionRepository
        extends JpaRepository<Prescription, UUID> {

    List<Prescription> findByAppointmentId(UUID appointmentId);

    List<Prescription> findByAppointmentPatientId(UUID patientId);

    boolean existsByAppointmentId(UUID appointmentId);

    // Bonus 1: Most prescribed medicines
    @Query("SELECT p.medicineName, COUNT(p) " +
        "FROM Prescription p " +
        "GROUP BY p.medicineName " +
        "ORDER BY COUNT(p) DESC")
    List<Object[]> findTopMedicines();
    }