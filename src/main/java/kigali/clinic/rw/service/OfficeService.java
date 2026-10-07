package kigali.clinic.rw.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import kigali.clinic.rw.repository.DoctorRepository;
import kigali.clinic.rw.domain.Office;
import kigali.clinic.rw.repository.OfficeRepository;

@Service
public class OfficeService {
    private final OfficeRepository offRepo;

    @Autowired
    private DoctorRepository doctorRepo;

    public OfficeService(OfficeRepository offRepo) { this.offRepo = offRepo; }

    public Office saveOffice(Office office) {
        if (office.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Omit id when creating an office");
        }
        if (offRepo.findByOfficeNumber(office.getOfficeNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Office number already exists");
        }
        return offRepo.save(office);
    }

    public List<Office> getAllOffices() { return offRepo.findAll(); }
    public Optional<Office> getOfficeById(UUID id) { return offRepo.findById(id); }

    public Optional<Object[]> getBusiestOffice() {
        List<Object[]> offices = offRepo.findOfficesByAppointmentCount();
        if (offices.isEmpty()) return Optional.empty();
        return Optional.of(offices.get(0));
    }

    public Optional<Office> updateOffice(UUID id, Office office) {
        Optional<Office> existing = offRepo.findById(id);
        if (existing.isEmpty()) return Optional.empty();
        Optional<Office> duplicate = offRepo.findByOfficeNumber(office.getOfficeNumber());
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Office number already exists");
        }
        Office target = existing.get();
        target.setName(office.getName());
        target.setOfficeNumber(office.getOfficeNumber());
        return Optional.of(offRepo.save(target));
    }

    public boolean deleteOffice(UUID id) {
        if (!offRepo.existsById(id)) return false;
        if (doctorRepo.findByOfficeId(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Office is assigned to a doctor");
        }
        offRepo.deleteById(id);
        return true;
    }
}
