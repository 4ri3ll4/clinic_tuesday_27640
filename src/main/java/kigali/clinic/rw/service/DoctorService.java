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
import kigali.clinic.rw.repository.SpecializationRepository;
import kigali.clinic.rw.domain.Office;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepo;

    @Autowired
    private OfficeRepository officeRepo;

    @Autowired
    private AppointmentRepository appointmentRepo;

    @Autowired
    private SpecializationRepository specializationRepo;

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

    public List<Doctor> getDoctorsBySpecialization(String name) {
        return doctorRepo.findDoctorsBySpecialization(name);
    }

    public List<Doctor> getDoctorsWithoutOffice() {
        return doctorRepo.findDoctorsWithoutOffice();
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

    @Transactional
    public Doctor assignSpecialization(UUID doctorId, UUID specializationId) {

        Optional<Doctor> existingDoctor = doctorRepo.findById(doctorId);

        if (existingDoctor.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Doctor not found"
            );
        }

        Optional<Specialization> existingSpecialization =
                specializationRepo.findById(specializationId);

        if (existingSpecialization.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Specialization not found"
            );
        }

        Doctor doctor = existingDoctor.get();
        Specialization specialization = existingSpecialization.get();

        for (Specialization s : doctor.getSpecializations()) {
            if (s.getId().equals(specializationId)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Doctor already has this specialization"
                );
            }
        }

        doctor.getSpecializations().add(specialization);

        return doctorRepo.save(doctor);
    }

    @Transactional
    public Doctor removeSpecialization(UUID doctorId, UUID specializationId) {

        Optional<Doctor> existingDoctor = doctorRepo.findById(doctorId);

        if (existingDoctor.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Doctor not found"
            );
        }

        Optional<Specialization> existingSpecialization =
                specializationRepo.findById(specializationId);

        if (existingSpecialization.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Specialization not found"
            );
        }

        Doctor doctor = existingDoctor.get();

        boolean removed = doctor.getSpecializations()
            .removeIf(s -> s.getId().equals(specializationId));

        if (!removed) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Doctor does not have this specialization"
            );
        }

        return doctorRepo.save(doctor);
    }

    @Transactional
    public Doctor reassignOffice(UUID doctorId, Integer officeNumber) {

        Doctor doctor = doctorRepo.findById(doctorId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Doctor not found"
            ));

        Office office = officeRepo.findByOfficeNumber(officeNumber)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Office not found"
            ));

        boolean occupied = doctorRepo.existsByOfficeIdAndIdNot(
            office.getId(), doctorId
        );

        if (occupied) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Office is already assigned to another doctor"
            );
        }

        doctor.setOffice(office);

        return doctorRepo.save(doctor);
    }
}
