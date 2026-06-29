package co.com.hermes.calendar.scheduling.schedule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Excepción de calendario para una fecha: feriado/cierre u horario especial. Es una entidad de dominio
 * (no un {@link Throwable}); el nombre refleja el concepto de negocio "excepción del horario".
 */
@Entity
@Table(name = "schedule_exceptions")
@SuppressWarnings("java:S2166")
public class ScheduleException {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "exception_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExceptionType type;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(length = 200)
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ScheduleException() {
    }

    public static ScheduleException of(
            UUID id,
            UUID tenantId,
            LocalDate date,
            ExceptionType type,
            LocalTime opensAt,
            LocalTime closesAt,
            String description
    ) {
        ScheduleException exception = new ScheduleException();
        exception.id = id;
        exception.tenantId = tenantId;
        exception.date = date;
        exception.type = type;
        exception.opensAt = opensAt;
        exception.closesAt = closesAt;
        exception.description = description;
        exception.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        return exception;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public LocalDate getDate() {
        return date;
    }

    public ExceptionType getType() {
        return type;
    }

    public LocalTime getOpensAt() {
        return opensAt;
    }

    public LocalTime getClosesAt() {
        return closesAt;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
