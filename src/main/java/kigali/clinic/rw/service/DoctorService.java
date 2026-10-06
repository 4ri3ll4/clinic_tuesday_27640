package kigali.clinic.rw.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import kigali.clinic.rw.domain.Doctor;
import kigali.clinic.rw.repository.DoctorRepository;
import kigali.clinic.rw.repository.OfficeRepository;
import kigali.clinic.rw.repository.AppointmentRepository;
import kigali.clinic.rw.domain.Specialization;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepo;

    @Autowired
    private OfficeRepository officeRepo;

    @Autowired
    private AppointmentRepository appointmentRepo;

    private void resolveOffice(Doctor doctor, UUID doctorId) {
        if (doctor.getOffice() == null || doctor.getOffice().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor must have an existing office.id");
        }
        UUID officeId = doctor.getOffice().getId();
        doctor.setOffice(officeRepo.findById(officeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Office not found")));
        Optional<Doctor> occupant = doctorRepo.findByOfficeId(officeId);
        if (occupant.isPresent() && !occupant.get().getId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Office already assigned to another doctor");
        }
    }

    public Doctor saveDoctor(Doctor doctor) {
        if (doctor.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Omit id when creating a doctor");
        }
        resolveOffice(doctor, null);
        return doctorRepo.save(doctor);
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepo.findAll();
    }

    public Optional<Doctor> getDoctorById(UUID id) {
        return doctorRepo.findById(id);
    }

    public Optional<Doctor> updateDoctor(UUID id, Doctor doctor) {
        Optional<Doctor> existingDoctor = doctorRepo.findById(id);

        if (existingDoctor.isEmpty()) {
            return Optional.empty();
        }

        Doctor doctorToUpdate = existingDoctor.get();
        resolveOffice(doctor, id);
        doctorToUpdate.setFirstName(doctor.getFirstName());
        doctorToUpdate.setLastName(doctor.getLastName());
        doctorToUpdate.setDateOfBirth(doctor.getDateOfBirth());
        doctorToUpdate.setOffice(doctor.getOffice());

        return Optional.of(doctorRepo.save(doctorToUpdate));
    }

    @Transactional
    public boolean deleteDoctor(UUID id) {
        if (!doctorRepo.existsById(id)) {
            return false;
        }
        if (appointmentRepo.existsByDoctorId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delete this doctor's appointments first");
        }
        Doctor doctor = doctorRepo.findById(id).orElseThrow();
        for (Specialization specialization : doctor.getSpecializations()) {
            specialization.getDoctors().remove(doctor);
        }
        doctorRepo.delete(doctor);
        return true;
    }
}
