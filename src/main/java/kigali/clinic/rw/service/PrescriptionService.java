package kigali.clinic.rw.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.domain.AppointmentStatus;
import kigali.clinic.rw.domain.Prescription;
import kigali.clinic.rw.repository.AppointmentRepository;
import kigali.clinic.rw.repository.PrescriptionRepository;

@Service
public class PrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepo;

    @Autowired
    private AppointmentRepository appointmentRepo;

    // C2: Create a prescription
    public Prescription savePrescription(Prescription prescription) {

        // A new prescription should not already have an ID
        if (prescription.getId() != null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Do not provide an ID when creating a prescription"
            );
        }

        // Check the appointment ID
        if (prescription.getAppointment() == null ||
            prescription.getAppointment().getId() == null) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Appointment ID is required"
            );
        }

        // Find the appointment
        Optional<Appointment> existingAppointment =
            appointmentRepo.findById(prescription.getAppointment().getId());

        if (existingAppointment.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Appointment not found"
            );
        }

        Appointment appointment = existingAppointment.get();

        // Only COMPLETED appointments can have prescriptions
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Prescriptions can only be issued for completed appointments"
            );
        }

        // Validate prescription information
        if (prescription.getMedicineName() == null ||
            prescription.getMedicineName().isBlank()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Medicine name is required"
            );
        }

        if (prescription.getDosage() == null ||
            prescription.getDosage().isBlank()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Dosage is required"
            );
        }

        if (prescription.getDurationDays() <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Duration must be greater than zero"
            );
        }

        // Assign the existing appointment
        prescription.setAppointment(appointment);

        // Always use today's date
        prescription.setIssuedDate(LocalDate.now());

        // Save to PostgreSQL
        return prescriptionRepo.save(prescription);
    }

    // C3: Get prescriptions by appointment
    public List<Prescription> getPrescriptionsByAppointment(UUID appointmentId) {

        if (!appointmentRepo.existsById(appointmentId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Appointment not found"
            );
        }

        return prescriptionRepo.findByAppointmentId(appointmentId);
    }

    // C3: Get prescriptions by patient
    public List<Prescription> getPrescriptionsByPatient(UUID patientId) {

        return prescriptionRepo.findByAppointmentPatientId(patientId);
    }
}