package kigali.clinic.rw.controller;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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

    @GetMapping("/page")
    public Page<Appointment> getAppointmentsPage(Pageable pageable) {
        return appointmentService.getAppointmentsPage(pageable);
    }

    @DeleteMapping("/cancelled-before")
    public String deleteCancelledBefore(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        int count = appointmentService.deleteCancelledBefore(date);
        return count + " appointments deleted";
    }

    @GetMapping("/stats/by-status")
    public List<Object[]> getAppointmentStatsByStatus() {
        return appointmentService.getAppointmentStatsByStatus();
    }

    @PatchMapping("/cancel-day")
    public String cancelDoctorDay(@RequestParam UUID doctorId, @RequestParam String date) {
        LocalDate appointmentDate = LocalDate.parse(date);
        int count = appointmentService.cancelDoctorDay(doctorId, appointmentDate);
        return count + " appointments cancelled";
    }

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
