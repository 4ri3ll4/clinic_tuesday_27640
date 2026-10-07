package kigali.clinic.rw.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import kigali.clinic.rw.repository.AppointmentRepository;

import kigali.clinic.rw.domain.Patient;
import kigali.clinic.rw.repository.PatientRepository;
import kigali.clinic.rw.repository.DoctorRepository;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepo;

    @Autowired
    private AppointmentRepository appointmentRepo;

    @Autowired
    private DoctorRepository doctorRepo;

    public Patient savePatient(Patient patient) {
        if (patient.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Omit id when creating a patient");
        }
        return patientRepo.save(patient);
    }

    public List<Patient> getAllPatients() {
        return patientRepo.findAll();
    }

    public List<Patient> getFrequentPatients(int min) {
        return patientRepo.findFrequentPatients(min);
    }

    public List<Patient> getPatientsByLastName(String lastName) {
        return patientRepo.findByLastNameIgnoreCaseOrderByFirstNameAsc(lastName);
    }

    public Optional<List<Patient>> getPatientsOfDoctor(UUID doctorId) {
        if (!doctorRepo.existsById(doctorId)) {
            return Optional.empty();
        }
        return Optional.of(patientRepo.findPatientsOfDoctor(doctorId));
    }

    public Optional<Patient> getPatientById(UUID id) {
        return patientRepo.findById(id);
    }

    public Optional<Patient> updatePatient(UUID id, Patient patient) {
        Optional<Patient> existingPatient = patientRepo.findById(id);

        if (existingPatient.isEmpty()) {
            return Optional.empty();
        }

        Patient patientToUpdate = existingPatient.get();
        patientToUpdate.setFirstName(patient.getFirstName());
        patientToUpdate.setLastName(patient.getLastName());
        patientToUpdate.setDateOfBirth(patient.getDateOfBirth());

        return Optional.of(patientRepo.save(patientToUpdate));
    }

    public boolean deletePatient(UUID id) {
        if (!patientRepo.existsById(id)) {
            return false;
        }
        if (appointmentRepo.existsByPatientId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delete this patient's appointments first");
        }
        patientRepo.deleteById(id);
        return true;
    }
}
