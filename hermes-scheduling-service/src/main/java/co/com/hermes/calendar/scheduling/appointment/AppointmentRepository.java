package co.com.hermes.calendar.scheduling.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Citas de un tenant cuyo inicio cae en el rango [from, to) (para la vista calendario). */
    List<Appointment> findByTenantIdAndSlotStartGreaterThanEqualAndSlotStartLessThan(
            UUID tenantId, LocalDateTime from, LocalDateTime to);

    /** Citas de un cliente (para su pantalla "mis citas"). */
    Page<Appointment> findByCustomerUserId(UUID customerUserId, Pageable pageable);

    Optional<Appointment> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Appointment> findByIdAndCustomerUserId(UUID id, UUID customerUserId);

    /** Citas en un estado dado creadas antes de un instante (para expirar reservas sin pagar). */
    List<Appointment> findByStatusAndCreatedAtBefore(AppointmentStatus status, OffsetDateTime cutoff);

    /** Conteo de citas por estado de un tenant en un rango de fechas de cita [from, to) (estadísticas). */
    @Query("select a.status as status, count(a) as total from Appointment a "
            + "where a.tenantId = :tenantId and a.slotStart >= :from and a.slotStart < :to group by a.status")
    List<StatusCount> countByStatus(@Param("tenantId") UUID tenantId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

    /** Proyección para la agregación de {@link #countByStatus}. */
    interface StatusCount {
        AppointmentStatus getStatus();
        long getTotal();
    }
}
