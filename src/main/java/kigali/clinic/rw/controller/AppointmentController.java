package kigali.clinic.rw.controller;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.domain.AppointmentStatus;
import kigali.clinic.rw.service.AppointmentService;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/save")
    public ResponseEntity<?> create(@RequestBody Appointment appointment) {
        return appointmentService.create(appointment)
            .<ResponseEntity<?>>map(saved -> new ResponseEntity<>(saved, HttpStatus.CREATED))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).body("Doctor is already booked on that date"));
    }

    @GetMapping({"", "/all"})
    public List<Appointment> getAll() { return appointmentService.getAll(); }

    @GetMapping("/by-status")
    public List<Appointment> getByStatus(@RequestParam AppointmentStatus status) {
        return appointmentService.getByStatus(status);
    }

    @GetMapping("/between")
    public List<Appointment> getBetweenDates(@RequestParam String start, @RequestParam String end) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);
        return appointmentService.getBetweenDates(startDate, endDate);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable UUID id) {
        return appointmentService.getById(id).<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Appointment not found"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Appointment appointment) {
        return appointmentService.update(id, appointment).<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Appointment not found"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable UUID id) {
        return appointmentService.delete(id) ? ResponseEntity.ok("Appointment deleted successfully")
            : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Appointment not found");
    }
}
