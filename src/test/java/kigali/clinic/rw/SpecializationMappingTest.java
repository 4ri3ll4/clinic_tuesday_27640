package kigali.clinic.rw;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import kigali.clinic.rw.domain.Appointment;
import kigali.clinic.rw.domain.Doctor;
import kigali.clinic.rw.domain.Office;
import kigali.clinic.rw.domain.Patient;
import kigali.clinic.rw.domain.Specialization;

import static org.junit.jupiter.api.Assertions.*;

class SpecializationMappingTest {
    private StandardServiceRegistry registry;

    @AfterEach
    void tearDown() {
        if (registry != null) StandardServiceRegistryBuilder.destroy(registry);
    }

    @Test
    void hibernateGeneratesJoinForeignKeysToTheCorrectParentTables() {
        registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            .applySetting("hibernate.connection.provider_class",
                "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
            .build();
        var metadata = new MetadataSources(registry)
            .addAnnotatedClass(Doctor.class).addAnnotatedClass(Specialization.class)
            .addAnnotatedClass(Office.class).addAnnotatedClass(Patient.class)
            .addAnnotatedClass(Appointment.class).buildMetadata();
        var owning = metadata.getCollectionBinding(Specialization.class.getName() + ".doctors");
        var inverse = metadata.getCollectionBinding(Doctor.class.getName() + ".specializations");
        var table = owning.getCollectionTable();
        assertEquals("doctor_specialization", table.getName());
        assertEquals(table, inverse.getCollectionTable());
        assertTrue(inverse.isInverse());
        assertEquals(2, table.getForeignKeys().size());
        var targets = new java.util.HashMap<String, String>();
        table.getForeignKeys().values().forEach(foreignKey ->
            targets.put(foreignKey.getColumns().get(0).getName(), foreignKey.getReferencedTable().getName()));
        assertEquals(java.util.Map.of("doctor_id", "doctor", "specialization_id", "specialization"), targets);
    }
}
