package kigali.clinic.rw.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.repository.AppointmentRepository;
import kigali.clinic.rw.repository.DoctorRepository;
import kigali.clinic.rw.repository.PatientRepository;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;

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

    public Appointment create(Appointment appointment) {
        if (appointment.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Omit id when creating an appointment");
        }
        resolveRelationships(appointment);
        return appointmentRepo.save(appointment);
    }

    public List<Appointment> getAll() { return appointmentRepo.findAll(); }
    public Optional<Appointment> getById(UUID id) { return appointmentRepo.findById(id); }

    public Optional<Appointment> update(UUID id, Appointment appointment) {
        Optional<Appointment> existing = appointmentRepo.findById(id);
        if (existing.isEmpty()) return Optional.empty();
        resolveRelationships(appointment);
        Appointment target = existing.get();
        target.setAppointmentDate(appointment.getAppointmentDate());
        target.setReason(appointment.getReason());
        target.setStatus(appointment.getStatus());
        target.setDoctor(appointment.getDoctor());
        target.setPatient(appointment.getPatient());
        return Optional.of(appointmentRepo.save(target));
    }

    public boolean delete(UUID id) {
        if (!appointmentRepo.existsById(id)) return false;
        appointmentRepo.deleteById(id);
        return true;
    }
}
