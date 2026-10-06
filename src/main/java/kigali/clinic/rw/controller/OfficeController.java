package kigali.clinic.rw.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import kigali.clinic.rw.domain.Office;
import kigali.clinic.rw.service.OfficeService;

@RestController
@RequestMapping("/api/office")
public class OfficeController {
    private final OfficeService offServe;
    public OfficeController(OfficeService offServe) { this.offServe = offServe; }

    @PostMapping("/save")
    public ResponseEntity<?> saveOffice(@RequestBody Office office) {
        return new ResponseEntity<>(offServe.saveOffice(office), HttpStatus.CREATED);
    }

    @GetMapping("/all")
    public List<Office> getAllOffices() { return offServe.getAllOffices(); }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOfficeById(@PathVariable UUID id) {
        return offServe.getOfficeById(id).<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Office not found"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateOffice(@PathVariable UUID id, @RequestBody Office office) {
        Optional<Office> updated = offServe.updateOffice(id, office);
        return updated.<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Office not found"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteOffice(@PathVariable UUID id) {
        return offServe.deleteOffice(id)
            ? ResponseEntity.ok("Office deleted successfully")
            : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Office not found");
    }
}
