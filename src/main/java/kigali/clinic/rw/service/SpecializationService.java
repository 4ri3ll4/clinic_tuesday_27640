package kigali.clinic.rw.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import kigali.clinic.rw.domain.Doctor;
import kigali.clinic.rw.domain.Specialization;
import kigali.clinic.rw.repository.DoctorRepository;
import kigali.clinic.rw.repository.SpecializationRepository;

@Service
public class SpecializationService {
    private final SpecializationRepository specializationRepo;
    private final DoctorRepository doctorRepo;

    public SpecializationService(SpecializationRepository specializationRepo, DoctorRepository doctorRepo) {
        this.specializationRepo = specializationRepo;
        this.doctorRepo = doctorRepo;
    }

    public Specialization save(Specialization specialization) {
        if (specialization.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Omit id when creating a specialization");
        }
        if (specialization.getName() == null || specialization.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specialization name is required");
        }
        if (specializationRepo.findByNameIgnoreCase(specialization.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Specialization already exists");
        }
        return specializationRepo.save(specialization);
    }

    public List<Specialization> getAll() { return specializationRepo.findAll(); }

    public List<Specialization> getUnusedSpecializations() {
        return specializationRepo.findUnusedSpecializations();
    }

    public Optional<Specialization> getById(UUID id) { return specializationRepo.findById(id); }

    public Optional<Specialization> update(UUID id, Specialization specialization) {
        Optional<Specialization> existing = specializationRepo.findById(id);
        if (existing.isEmpty()) return Optional.empty();
        if (specialization.getName() == null || specialization.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specialization name is required");
        }
        Optional<Specialization> duplicate = specializationRepo.findByNameIgnoreCase(specialization.getName());
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Specialization already exists");
        }
        Specialization target = existing.get();
        target.setName(specialization.getName());
        return Optional.of(specializationRepo.save(target));
    }

    public boolean delete(UUID id) {
        if (!specializationRepo.existsById(id)) return false;
        specializationRepo.deleteById(id);
        return true;
    }

    @Transactional
    public Specialization assignDoctor(UUID specializationId, UUID doctorId) {
        Specialization specialization = specializationRepo.findById(specializationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Specialization not found"));
        Doctor doctor = doctorRepo.findById(doctorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        if (!specialization.getDoctors().contains(doctor)) {
            specialization.getDoctors().add(doctor);
        }
        return specializationRepo.save(specialization);
    }
}
