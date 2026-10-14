package kigali.clinic.rw.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import kigali.clinic.rw.domain.Prescription;
import kigali.clinic.rw.service.PrescriptionService;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    @Autowired
    private PrescriptionService prescriptionService;

    // C2: Create a prescription
    @PostMapping("/save")
    public ResponseEntity<Prescription> savePrescription(
            @RequestBody Prescription prescription) {

        Prescription savedPrescription =
            prescriptionService.savePrescription(prescription);

        return new ResponseEntity<>(
            savedPrescription,
            HttpStatus.CREATED
        );
    }

    // C3: Get prescriptions by appointment
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<List<Prescription>> getByAppointment(
            @PathVariable("appointmentId") UUID appointmentId) {

        List<Prescription> prescriptions =
            prescriptionService.getPrescriptionsByAppointment(appointmentId);

        return new ResponseEntity<>(prescriptions, HttpStatus.OK);
    }

    // C3: Get prescriptions by patient
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Prescription>> getByPatient(
            @PathVariable("patientId") UUID patientId) {

        List<Prescription> prescriptions =
            prescriptionService.getPrescriptionsByPatient(patientId);

        return new ResponseEntity<>(prescriptions, HttpStatus.OK);
    }

    // C4: Update prescription
    @PutMapping("/{id}")
    public ResponseEntity<Prescription> updatePrescription(
            @PathVariable("id") UUID id,
            @RequestBody Prescription prescription) {

        Prescription updatedPrescription =
            prescriptionService.updatePrescription(id, prescription);

        return new ResponseEntity<>(updatedPrescription, HttpStatus.OK);
    }

    // C4: Delete prescription
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(
            @PathVariable("id") UUID id) {

        prescriptionService.deletePrescription(id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Bonus 1: Most prescribed medicines
    @GetMapping("/stats/top-medicines")
    public ResponseEntity<List<Object[]>> getTopMedicines() {

        List<Object[]> medicines = prescriptionService.getTopMedicines();

        return new ResponseEntity<>(medicines, HttpStatus.OK);
    }
}