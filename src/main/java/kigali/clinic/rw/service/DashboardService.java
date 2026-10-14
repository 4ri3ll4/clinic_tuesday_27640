package kigali.clinic.rw.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kigali.clinic.rw.domain.AppointmentStatus;
import kigali.clinic.rw.repository.AppointmentRepository;
import kigali.clinic.rw.repository.DoctorRepository;
import kigali.clinic.rw.repository.PatientRepository;
import kigali.clinic.rw.repository.PrescriptionRepository;

@Service
public class DashboardService {

    @Autowired
    private DoctorRepository doctorRepo;

    @Autowired
    private PatientRepository patientRepo;

    @Autowired
    private AppointmentRepository appointmentRepo;

    @Autowired
    private PrescriptionRepository prescriptionRepo;

    public Map<String, Long> getSummary() {

        Map<String, Long> summary = new LinkedHashMap<>();

        summary.put("totalDoctors", doctorRepo.count());
        summary.put("totalPatients", patientRepo.count());
        summary.put("totalAppointments", appointmentRepo.count());
        summary.put("totalPrescriptions", prescriptionRepo.count());

        summary.put("scheduledAppointments",
            appointmentRepo.countByStatus(AppointmentStatus.SCHEDULED));

        summary.put("confirmedAppointments",
            appointmentRepo.countByStatus(AppointmentStatus.CONFIRMED));

        summary.put("completedAppointments",
            appointmentRepo.countByStatus(AppointmentStatus.COMPLETED));

        summary.put("cancelledAppointments",
            appointmentRepo.countByStatus(AppointmentStatus.CANCELLED));

        return summary;
    }
}