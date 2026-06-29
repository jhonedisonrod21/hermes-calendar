package co.com.hermes.calendar.scheduling.appointment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Cita reservada para un servicio de un establecimiento. */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "offering_id", nullable = false)
    private UUID offeringId;

    @Column(name = "customer_user_id", nullable = false)
    private UUID customerUserId;

    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private LocalDateTime slotEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "price_amount", precision = 12, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", length = 3)
    private String priceCurrency;

    @Column(name = "requires_online_payment", nullable = false)
    private boolean requiresOnlinePayment;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "appointment_id", nullable = false)
    private List<AppointmentRequirementValue> requirementValues = new ArrayList<>();

    protected Appointment() {
    }

    /** Establecimiento, servicio y cliente de la cita. */
    public record BookingRefs(UUID tenantId, UUID offeringId, UUID customerUserId) {
    }

    /** Franja horaria (inicio/fin) de la cita. */
    public record SlotRange(LocalDateTime start, LocalDateTime end) {
    }

    /** Precio acordado y si exige pago en línea por adelantado. */
    public record Pricing(BigDecimal priceAmount, String priceCurrency, boolean requiresOnlinePayment) {
    }

    public static Appointment book(UUID id, BookingRefs refs, SlotRange slot, AppointmentStatus status,
                                   Pricing pricing, List<AppointmentRequirementValue> requirementValues) {
        Appointment appointment = new Appointment();
        appointment.id = id;
        appointment.tenantId = refs.tenantId();
        appointment.offeringId = refs.offeringId();
        appointment.customerUserId = refs.customerUserId();
        appointment.slotStart = slot.start();
        appointment.slotEnd = slot.end();
        appointment.status = status;
        appointment.priceAmount = pricing.priceAmount();
        appointment.priceCurrency = pricing.priceCurrency();
        appointment.requiresOnlinePayment = pricing.requiresOnlinePayment();
        appointment.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        appointment.updatedAt = appointment.createdAt;
        appointment.requirementValues.addAll(requirementValues);
        return appointment;
    }

    public void changeStatus(AppointmentStatus status) {
        this.status = status;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /** Mueve la cita a un nuevo cupo (mismo servicio); el estado no cambia. */
    public void reschedule(LocalDateTime slotStart, LocalDateTime slotEnd) {
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getOfferingId() {
        return offeringId;
    }

    public UUID getCustomerUserId() {
        return customerUserId;
    }

    public LocalDateTime getSlotStart() {
        return slotStart;
    }

    public LocalDateTime getSlotEnd() {
        return slotEnd;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public String getPriceCurrency() {
        return priceCurrency;
    }

    public boolean isRequiresOnlinePayment() {
        return requiresOnlinePayment;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<AppointmentRequirementValue> getRequirementValues() {
        return requirementValues;
    }
}
