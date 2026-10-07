package kigali.clinic.rw.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import kigali.clinic.rw.domain.Office;

@Repository 
public interface OfficeRepository extends JpaRepository<Office,UUID> {
    
    Optional<Office> findByOfficeNumber(int officeN);

    @Query("SELECT o.name, o.officeNumber, COUNT(a) FROM Office o JOIN o.doctor d JOIN d.appointments a GROUP BY o.id, o.name, o.officeNumber ORDER BY COUNT(a) DESC")
    List<Object[]> findOfficesByAppointmentCount();

}
