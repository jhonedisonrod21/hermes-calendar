package co.com.hermes.calendar.scheduling.appointment;

import co.com.hermes.calendar.scheduling.catalog.CatalogClient;
import co.com.hermes.calendar.scheduling.catalog.OfferingSnapshot;
import co.com.hermes.calendar.scheduling.schedule.BusinessHours;
import co.com.hermes.calendar.scheduling.schedule.BusinessHoursRepository;
import co.com.hermes.calendar.scheduling.schedule.ScheduleExceptionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvailabilityServiceTest {

    private final CatalogClient catalog = mock(CatalogClient.class);
    private final BusinessHoursRepository hours = mock(BusinessHoursRepository.class);
    private final ScheduleExceptionRepository exceptions = mock(ScheduleExceptionRepository.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final AvailabilityService service = new AvailabilityService(catalog, hours, exceptions, appointments);

    private final UUID offeringId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2026, Month.JULY, 1);

    private OfferingSnapshot offering(int duration) {
        return new OfferingSnapshot(offeringId, tenantId, "Cita", duration, "IN_PERSON", null, null, false, true, List.of());
    }

    @Test
    void computesSlotsFromBusinessHoursMinusBooked() {
        when(catalog.findOffering(offeringId)).thenReturn(Optional.of(offering(30)));
        when(exceptions.findByTenantIdAndDate(tenantId, date)).thenReturn(Optional.empty());
        when(hours.findByTenantIdOrderByDayOfWeekAscOpensAtAsc(tenantId)).thenReturn(List.of(
                BusinessHours.of(UUID.randomUUID(), tenantId, date.getDayOfWeek(), LocalTime.of(9, 0), LocalTime.of(11, 0))));
        when(appointments.findByOfferingIdAndSlotStartGreaterThanEqualAndSlotStartLessThan(eq(offeringId), any(), any()))
                .thenReturn(List.of());

        List<AvailableSlot> slots = service.availableSlots(offeringId, date);

        // 09:00, 09:30, 10:00, 10:30 (cada uno de 30 min, cierra 11:00)
        assertThat(slots).hasSize(4);
        assertThat(slots.get(0).start()).isEqualTo(date.atTime(9, 0));
        assertThat(slots.get(3).end()).isEqualTo(date.atTime(11, 0));
    }

    @Test
    void returnsNoSlotsWhenInactiveOffering() {
        OfferingSnapshot inactive = new OfferingSnapshot(offeringId, tenantId, "Cita", 30, "IN_PERSON", null, null, false, false, List.of());
        when(catalog.findOffering(offeringId)).thenReturn(Optional.of(inactive));

        assertThat(service.availableSlots(offeringId, date)).isEmpty();
    }
}
