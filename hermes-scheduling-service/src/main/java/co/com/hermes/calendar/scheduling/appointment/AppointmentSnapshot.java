package co.com.hermes.calendar.scheduling.appointment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vista interna de una cita para otros servicios del contexto (payment). Expone solo lo que el
 * servicio de pagos necesita para cobrar: importe, moneda, dueño y estado. El importe sale de la
 * cita (del servidor), nunca del cliente.
 */
public record AppointmentSnapshot(
        UUID id,
        UUID tenantId,
        UUID offeringId,
        UUID customerUserId,
        LocalDateTime slotStart,
        LocalDateTime slotEnd,
        AppointmentStatus status,
        BigDecimal priceAmount,
        String priceCurrency,
        boolean requiresOnlinePayment
) {
    public static AppointmentSnapshot from(Appointment appointment) {
        return new AppointmentSnapshot(
                appointment.getId(),
                appointment.getTenantId(),
                appointment.getOfferingId(),
                appointment.getCustomerUserId(),
                appointment.getSlotStart(),
                appointment.getSlotEnd(),
                appointment.getStatus(),
                appointment.getPriceAmount(),
                appointment.getPriceCurrency(),
                appointment.isRequiresOnlinePayment());
    }
}
