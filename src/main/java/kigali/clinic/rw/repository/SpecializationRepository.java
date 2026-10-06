package kigali.clinic.rw.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import kigali.clinic.rw.domain.Specialization;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, UUID> {
    Optional<Specialization> findByNameIgnoreCase(String name);

    @Query("SELECT s FROM Specialization s WHERE s.doctors IS EMPTY")
    List<Specialization> findUnusedSpecializations();
}
