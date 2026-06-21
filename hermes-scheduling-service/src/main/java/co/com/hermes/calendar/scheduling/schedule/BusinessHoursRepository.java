package co.com.hermes.calendar.scheduling.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessHoursRepository extends JpaRepository<BusinessHours, UUID> {

    List<BusinessHours> findByTenantIdOrderByDayOfWeekAscOpensAtAsc(UUID tenantId);

    void deleteByTenantId(UUID tenantId);
}
