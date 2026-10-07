package kigali.clinic.rw;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import kigali.clinic.rw.controller.AppointmentController;
import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.domain.AppointmentStatus;
import kigali.clinic.rw.domain.Doctor;
import kigali.clinic.rw.domain.Office;
import kigali.clinic.rw.domain.Patient;
import kigali.clinic.rw.domain.Specialization;
import kigali.clinic.rw.repository.AppointmentRepository;
import kigali.clinic.rw.service.AppointmentService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AppointmentBonusTest {
    private AppointmentRepository repository;
    private MockMvc mvc;
    private StandardServiceRegistry registry;
    private SessionFactoryImplementor factory;

    @BeforeEach
    void setUp() {
        repository = mock(AppointmentRepository.class);
        var service = new AppointmentService(repository, null, null);
        mvc = MockMvcBuilders.standaloneSetup(new AppointmentController(service))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }

    @AfterEach
    void tearDown() {
        if (factory != null) factory.close();
        if (registry != null) StandardServiceRegistryBuilder.destroy(registry);
    }

    private Appointment appointment(String date) {
        Appointment appointment = new Appointment();
        appointment.setAppointmentDate(LocalDate.parse(date));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        return appointment;
    }

    @Test
    void pageRouteBindsStandardParametersAndReturnsRepositoryPage() throws Exception {
        var pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "appointmentDate"));
        var content = List.of(appointment("2026-11-10"), appointment("2026-11-03"),
            appointment("2026-10-25"), appointment("2026-10-20"), appointment("2026-10-18"));
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(content, pageable, 10));
        mvc.perform(get("/api/appointments/page")
                .param("page", "0").param("size", "5").param("sort", "appointmentDate,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.totalElements").value(10))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.content[0].appointmentDate").value("2026-11-10"));
        verify(repository).findAll(pageable);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void deleteBindsLocalDateAndReturnsActualCountWithOneRepositoryCall(int count) throws Exception {
        LocalDate cutoff = LocalDate.of(2026, 10, 15);
        when(repository.deleteCancelledBefore(cutoff, AppointmentStatus.CANCELLED)).thenReturn(count);
        mvc.perform(delete("/api/appointments/cancelled-before").param("date", "2026-10-15"))
            .andExpect(status().isOk())
            .andExpect(content().string(count + " appointments deleted"));
        verify(repository).deleteCancelledBefore(cutoff, AppointmentStatus.CANCELLED);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void malformedDateDoesNotExecuteDelete() throws Exception {
        mvc.perform(delete("/api/appointments/cancelled-before").param("date", "invalid"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void missingDateDoesNotExecuteDelete() throws Exception {
        mvc.perform(delete("/api/appointments/cancelled-before"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void bulkDeleteHasTransactionAndValidStrictDateJpql() throws Exception {
        var method = AppointmentRepository.class.getMethod("deleteCancelledBefore", LocalDate.class, AppointmentStatus.class);
        var modifying = method.getAnnotation(Modifying.class);
        assertNotNull(modifying);
        assertTrue(modifying.clearAutomatically());
        assertTrue(modifying.flushAutomatically());
        assertNotNull(AppointmentService.class.getMethod("deleteCancelledBefore", LocalDate.class)
            .getAnnotation(Transactional.class));
        var query = method.getAnnotation(Query.class);
        assertFalse(query.nativeQuery());
        assertTrue(query.value().contains("a.status = :status"));
        assertTrue(query.value().contains("a.appointmentDate < :date"));
        assertFalse(query.value().contains("<="));

        registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            .applySetting("hibernate.connection.provider_class",
                "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
            .applySetting("hibernate.hbm2ddl.auto", "none").build();
        factory = (SessionFactoryImplementor) new MetadataSources(registry)
            .addAnnotatedClass(Appointment.class).addAnnotatedClass(Doctor.class)
            .addAnnotatedClass(Patient.class).addAnnotatedClass(Office.class)
            .addAnnotatedClass(Specialization.class).buildMetadata().buildSessionFactory();
        assertNotNull(factory.getQueryEngine().getHqlTranslator().translate(query.value(), null));
    }
}
