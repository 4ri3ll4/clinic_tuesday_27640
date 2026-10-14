package kigali.clinic.rw.controller;

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
}