package co.com.hermes.calendar.scheduling.schedule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Franja de horario laboral de un establecimiento para un día de la semana. */
@Entity
@Table(name = "business_hours")
public class BusinessHours {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "opens_at", nullable = false)
    private LocalTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalTime closesAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected BusinessHours() {
    }

    public static BusinessHours of(UUID id, UUID tenantId, DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
        BusinessHours hours = new BusinessHours();
        hours.id = id;
        hours.tenantId = tenantId;
        hours.dayOfWeek = dayOfWeek;
        hours.opensAt = opensAt;
        hours.closesAt = closesAt;
        hours.createdAt = OffsetDateTime.now();
        return hours;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getOpensAt() {
        return opensAt;
    }

    public LocalTime getClosesAt() {
        return closesAt;
    }
}
