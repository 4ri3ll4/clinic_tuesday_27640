package kigali.clinic.rw;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import kigali.clinic.rw.controller.AppointmentController;
import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.domain.AppointmentStatus;
import kigali.clinic.rw.repository.AppointmentRepository;
import kigali.clinic.rw.repository.DoctorRepository;
import kigali.clinic.rw.repository.PatientRepository;
import kigali.clinic.rw.service.AppointmentService;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentDateTest {
    private static final UUID DOCTOR_ID = UUID.fromString("37148a60-a6d6-4de4-b15a-84863cde8a71");
    private static final UUID PATIENT_ID = UUID.fromString("284f0dae-1d59-47b3-ace7-43da66468c5e");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 25);

    private TimeZone originalTimeZone;
    private AnnotationConfigApplicationContext context;
    private JsonMapper mapper;
    private AppointmentRepository appointments;
    private DoctorRepository doctors;
    private PatientRepository patients;
    private AppointmentController controller;

    @BeforeEach
    void setUp() {
        originalTimeZone = TimeZone.getDefault();
        context = new AnnotationConfigApplicationContext(JacksonAutoConfiguration.class);
        mapper = context.getBean(JsonMapper.class);
        appointments = mock(AppointmentRepository.class);
        doctors = mock(DoctorRepository.class);
        patients = mock(PatientRepository.class);
        controller = new AppointmentController(new AppointmentService(appointments, doctors, patients));
    }

    @AfterEach
    void tearDown() {
        context.close();
        TimeZone.setDefault(originalTimeZone);
    }

    private Appointment request() {
        return mapper.readValue("""
            {
              "appointmentDate": "2026-10-25",
              "reason": "Date regression test",
              "status": "SCHEDULED",
              "patient": {"id": "284f0dae-1d59-47b3-ace7-43da66468c5e"},
              "doctor": {"id": "37148a60-a6d6-4de4-b15a-84863cde8a71"}
            }
            """, Appointment.class);
    }

    private void existingReferences(Appointment request) {
        when(doctors.findById(DOCTOR_ID)).thenReturn(Optional.of(request.getDoctor()));
        when(patients.findById(PATIENT_ID)).thenReturn(Optional.of(request.getPatient()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Africa/Kigali", "UTC", "America/Los_Angeles", "Pacific/Auckland"})
    void postedAndStoredCalendarDatesMatchAndSecondBookingIsRejected(String timeZone) {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
        Appointment first = request();
        assertEquals(DATE, first.getAppointmentDate());
        existingReferences(first);
        when(appointments.existsByDoctorIdAndAppointmentDateAndStatusNot(
            DOCTOR_ID, DATE, AppointmentStatus.CANCELLED)).thenReturn(false, true);
        when(appointments.save(first)).thenReturn(first);

        assertEquals(201, controller.create(first).getStatusCode().value());
        Appointment stored = new Appointment();
        // Model a JDBC DATE read as a calendar date, without formatting a midnight instant.
        stored.setAppointmentDate(java.sql.Date.valueOf(first.getAppointmentDate()).toLocalDate());
        when(appointments.findById(UUID.fromString("093e2e82-5a35-4945-9b17-9657cf14c9c0")))
            .thenReturn(Optional.of(stored));
        Object read = controller.getById(UUID.fromString("093e2e82-5a35-4945-9b17-9657cf14c9c0")).getBody();
        assertEquals("2026-10-25", mapper.readTree(mapper.writeValueAsString(read))
            .get("appointmentDate").asString());

        Appointment second = request();
        var response = controller.create(second);
        assertEquals(409, response.getStatusCode().value());
        assertEquals("Doctor is already booked on that date", response.getBody());
        verify(appointments, times(2)).existsByDoctorIdAndAppointmentDateAndStatusNot(
            DOCTOR_ID, DATE, AppointmentStatus.CANCELLED);
        verify(appointments, times(1)).save(any(Appointment.class));
        verify(appointments, never()).save(second);
    }

    @Test
    void a3PassesInclusiveCalendarBoundariesWithoutTimestampConversion() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);
        when(appointments.findByAppointmentDateBetweenOrderByAppointmentDateAsc(start, end))
            .thenReturn(List.of());
        assertTrue(controller.getBetweenDates("2026-10-01", "2026-10-31").isEmpty());
        verify(appointments).findByAppointmentDateBetweenOrderByAppointmentDateAsc(start, end);
        PartTree query = new PartTree("findByAppointmentDateBetweenOrderByAppointmentDateAsc", Appointment.class);
        assertEquals(Part.Type.BETWEEN, query.getParts().iterator().next().getType());
        assertTrue(query.getSort().getOrderFor("appointmentDate").isAscending());
    }

    @Test
    void cancelledOnlyDayStillAllowsSavingAndA4RemainsDerived() throws NoSuchMethodException {
        Appointment appointment = request();
        existingReferences(appointment);
        when(appointments.existsByDoctorIdAndAppointmentDateAndStatusNot(
            DOCTOR_ID, DATE, AppointmentStatus.CANCELLED)).thenReturn(false);
        when(appointments.save(appointment)).thenReturn(appointment);
        assertEquals(201, controller.create(appointment).getStatusCode().value());
        verify(appointments).save(appointment);
        assertNull(AppointmentRepository.class.getMethod(
            "existsByDoctorIdAndAppointmentDateAndStatusNot", UUID.class, LocalDate.class, AppointmentStatus.class)
            .getAnnotation(org.springframework.data.jpa.repository.Query.class));
    }
}
