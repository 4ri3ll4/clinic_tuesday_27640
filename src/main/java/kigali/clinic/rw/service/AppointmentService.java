package kigali.clinic.rw.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.domain.AppointmentStatus;
import kigali.clinic.rw.repository.AppointmentRepository;
import kigali.clinic.rw.repository.DoctorRepository;
import kigali.clinic.rw.repository.PatientRepository;
import kigali.clinic.rw.repository.PrescriptionRepository;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;

    @Autowired
    private PrescriptionRepository prescriptionRepo;

    public AppointmentService(AppointmentRepository appointmentRepo, DoctorRepository doctorRepo, PatientRepository patientRepo) {
        this.appointmentRepo = appointmentRepo;
        this.doctorRepo = doctorRepo;
        this.patientRepo = patientRepo;
    }

    private void resolveRelationships(Appointment appointment) {
        if (appointment.getAppointmentDate() == null || appointment.getStatus() == null
                || appointment.getDoctor() == null || appointment.getDoctor().getId() == null
                || appointment.getPatient() == null || appointment.getPatient().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Appointment date, status, doctor.id and patient.id are required");
        }
        appointment.setDoctor(doctorRepo.findById(appointment.getDoctor().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found")));
        appointment.setPatient(patientRepo.findById(appointment.getPatient().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found")));
    }

    public Optional<Appointment> create(Appointment appointment) {
        if (appointment.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Omit id when creating an appointment");
        }

        // B1: Rejects appointments without a date
        if (appointment.getAppointmentDate() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Appointment date is required"
            );
        }

        // B1: Rejects appointments in the past
        if (appointment.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cannot create an appointment in the past"
            );
        }

        // B1: Where sll new appointments must start as SCHEDULED
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        resolveRelationships(appointment);

        //preventing double bookings
        if (appointmentRepo.existsByDoctorIdAndAppointmentDateAndStatusNot(
                appointment.getDoctor().getId(),
                appointment.getAppointmentDate(),
                AppointmentStatus.CANCELLED)) {
            return Optional.empty();
        }

        return Optional.of(appointmentRepo.save(appointment));
    }

    public List<Appointment> getAll() { return appointmentRepo.findAll(); }
    public Optional<Appointment> getById(UUID id) { return appointmentRepo.findById(id); }

    public Page<Appointment> getAppointmentsPage(Pageable pageable) {
        return appointmentRepo.findAll(pageable);
    }

    @Transactional
    public int deleteCancelledBefore(LocalDate date) {
        return appointmentRepo.deleteCancelledBefore(date, AppointmentStatus.CANCELLED);
    }

    public List<Object[]> getAppointmentStatsByStatus() {
        return appointmentRepo.getAppointmentStatsByStatus();
    }

    @Transactional
    public int cancelDoctorDay(UUID doctorId, LocalDate date) {
        return appointmentRepo.cancelDoctorDay(doctorId, date,
            AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED);
    }

    public List<Appointment> getByStatus(AppointmentStatus status) {
        return appointmentRepo.findByStatusOrderByAppointmentDateAsc(status);
    }

    public List<Appointment> getBetweenDates(LocalDate start, LocalDate end) {
        return appointmentRepo.findByAppointmentDateBetweenOrderByAppointmentDateAsc(
            start, end);
    }

    public Optional<Appointment> update(UUID id, Appointment appointment) {
        Optional<Appointment> existing = appointmentRepo.findById(id);
        if (existing.isEmpty()) return Optional.empty();
        resolveRelationships(appointment);
        Appointment target = existing.get();
        target.setAppointmentDate(appointment.getAppointmentDate());
        target.setReason(appointment.getReason());
        target.setDoctor(appointment.getDoctor());
        target.setPatient(appointment.getPatient());
        return Optional.of(appointmentRepo.save(target));
    }

    //C5: Prevent deletion of appointments with prescriptions
    public boolean delete(UUID id) {
        if (!appointmentRepo.existsById(id)) {
            return false;
        }

        //check if the appointment has prescriptions
        if (prescriptionRepo.existsByAppointmentId(id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot delete an appointment that has prescriptions"
            );
        }

        //delete the appointment if it has no prescriptions
        appointmentRepo.deleteById(id);
        return true;
    }


    public Appointment updateStatus(UUID id, AppointmentStatus newStatus) {

        Appointment appointment = appointmentRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Appointment not found"
            ));

        AppointmentStatus currentStatus = appointment.getStatus();

        if (newStatus == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Status is required"
            );
        }

        if (currentStatus == AppointmentStatus.SCHEDULED) {

            if (newStatus != AppointmentStatus.CONFIRMED &&
                newStatus != AppointmentStatus.CANCELLED) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid status change"
                );
            }

        } else if (currentStatus == AppointmentStatus.CONFIRMED) {

            if (newStatus != AppointmentStatus.COMPLETED &&
                newStatus != AppointmentStatus.CANCELLED) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid status change"
                );
            }

        } else {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Completed or cancelled appointments cannot change status"
            );
        }

        appointment.setStatus(newStatus);

        return appointmentRepo.save(appointment);
    }

    public Appointment rescheduleAppointment(UUID id, LocalDate newDate) {

        // Find the appointment
        Appointment appointment = appointmentRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Appointment not found"
            ));

        // B3: Check the new date
        if (newDate == null || newDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "New appointment date cannot be in the past"
            );
        }

        // B3: Completed or cancelled appointments cannot be rescheduled
        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
            appointment.getStatus() == AppointmentStatus.CANCELLED) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Completed or cancelled appointments cannot be rescheduled"
            );
        }

        // B3: Check if another appointment already exists for the doctor
        boolean booked = appointmentRepo.existsByDoctorIdAndAppointmentDateAndStatusNotAndIdNot(
            appointment.getDoctor().getId(),
            newDate,
            AppointmentStatus.CANCELLED,
            id
        );

        if (booked) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Doctor is already booked on the new date"
            );
        }

        // B3: Update the date and reset the status
        appointment.setAppointmentDate(newDate);
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        return appointmentRepo.save(appointment);
    }

    // D1: Get a doctor's agenda
    public List<Appointment> getDoctorAgenda(
            UUID doctorId, LocalDate start, LocalDate end) {

        // Check if the doctor exists
        if (!doctorRepo.existsById(doctorId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Doctor not found"
            );
        }

        // Check the date range
        if (start == null || end == null || start.isAfter(end)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid date range"
            );
        }

        return appointmentRepo
            .findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAsc(
                doctorId, start, end
            );
    }

    // D2: Get upcoming appointments for a patient
    public List<Appointment> getPatientUpcomingAppointments(UUID patientId) {

        // Check if the patient exists
        if (!patientRepo.existsById(patientId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Patient not found"
            );
        }

        // Only appointments that are still active
        List<AppointmentStatus> activeStatuses = List.of(
            AppointmentStatus.SCHEDULED,
            AppointmentStatus.CONFIRMED
        );

        return appointmentRepo
            .findByPatientIdAndAppointmentDateGreaterThanEqualAndStatusInOrderByAppointmentDateAsc(
                patientId,
                LocalDate.now(),
                activeStatuses
            );
    }
}
