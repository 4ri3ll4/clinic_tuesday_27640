package kigali.clinic.rw.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import kigali.clinic.rw.domain.Specialization;
import kigali.clinic.rw.service.SpecializationService;

@RestController
@RequestMapping("/api/specializations")
public class SpecializationController {
    private final SpecializationService specializationService;

    public SpecializationController(SpecializationService specializationService) {
        this.specializationService = specializationService;
    }

    @PostMapping({"", "/save"})
    public ResponseEntity<?> create(@RequestBody Specialization specialization) {
        return new ResponseEntity<>(specializationService.save(specialization), HttpStatus.CREATED);
    }

    @GetMapping({"", "/all"})
    public List<Specialization> getAll() { return specializationService.getAll(); }

    @GetMapping("/unused")
    public List<Specialization> getUnusedSpecializations() {
        return specializationService.getUnusedSpecializations();
    }

    @PostMapping("/{specializationId}/doctors/{doctorId}")
    public ResponseEntity<?> assignDoctor(@PathVariable UUID specializationId, @PathVariable UUID doctorId) {
        return ResponseEntity.ok(specializationService.assignDoctor(specializationId, doctorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        return specializationService.getById(id).<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Specialization not found"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Specialization specialization) {
        return specializationService.update(id, specialization).<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Specialization not found"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable UUID id) {
        return specializationService.delete(id) ? ResponseEntity.ok("Specialization deleted successfully")
            : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Specialization not found");
    }
}
