package co.com.hermes.calendar.scheduling.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    /** Citas de un servicio en un rango (para calcular cupos ocupados de un día). */
    List<Appointment> findByOfferingIdAndSlotStartGreaterThanEqualAndSlotStartLessThan(
            UUID offeringId, LocalDateTime from, LocalDateTime to);

    boolean existsByOfferingIdAndSlotStartAndStatusIn(
            UUID offeringId, LocalDateTime slotStart, List<AppointmentStatus> statuses);

    Page<Appointment> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Appointment> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Appointment> findByIdAndCustomerUserId(UUID id, UUID customerUserId);

    /** Citas en un estado dado creadas antes de un instante (para expirar reservas sin pagar). */
    List<Appointment> findByStatusAndCreatedAtBefore(AppointmentStatus status, OffsetDateTime cutoff);
}
