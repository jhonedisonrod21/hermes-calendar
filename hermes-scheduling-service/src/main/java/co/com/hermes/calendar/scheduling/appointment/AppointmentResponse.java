package co.com.hermes.calendar.scheduling.appointment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Vista de una cita.")
public record AppointmentResponse(
        UUID id,
        UUID tenantId,
        UUID offeringId,
        UUID customerUserId,
        LocalDateTime slotStart,
        LocalDateTime slotEnd,
        AppointmentStatus status,
        BigDecimal priceAmount,
        String priceCurrency,
        boolean requiresOnlinePayment,
        Map<String, String> requirementValues,
        OffsetDateTime createdAt
) {

    public static AppointmentResponse from(Appointment a) {
        Map<String, String> values = new LinkedHashMap<>();
        a.getRequirementValues().forEach(v -> values.put(v.getKey(), v.getValue()));
        return new AppointmentResponse(
                a.getId(), a.getTenantId(), a.getOfferingId(), a.getCustomerUserId(),
                a.getSlotStart(), a.getSlotEnd(), a.getStatus(), a.getPriceAmount(), a.getPriceCurrency(),
                a.isRequiresOnlinePayment(), values, a.getCreatedAt());
    }
}
