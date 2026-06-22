package co.com.hermes.calendar.scheduling.schedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleExceptionRepository extends JpaRepository<ScheduleException, UUID> {

    Page<ScheduleException> findByTenantIdOrderByDateAsc(UUID tenantId, Pageable pageable);

    Optional<ScheduleException> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ScheduleException> findByTenantIdAndDate(UUID tenantId, LocalDate date);

    boolean existsByTenantIdAndDate(UUID tenantId, LocalDate date);
}
