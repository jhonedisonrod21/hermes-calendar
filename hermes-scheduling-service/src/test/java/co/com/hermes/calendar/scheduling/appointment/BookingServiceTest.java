package co.com.hermes.calendar.scheduling.appointment;

import co.com.hermes.calendar.scheduling.catalog.CatalogClient;
import co.com.hermes.calendar.scheduling.catalog.OfferingSnapshot;
import co.com.hermes.calendar.scheduling.notification.NotificationClient;
import co.com.hermes.calendar.scheduling.requirementfile.RequirementFileService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    private final CatalogClient catalog = mock(CatalogClient.class);
    private final AvailabilityService availability = mock(AvailabilityService.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final NotificationClient notificationClient = mock(NotificationClient.class);
    private final RequirementFileService requirementFiles = mock(RequirementFileService.class);
    private final BookingService service = new BookingService(catalog, availability, appointments, notificationClient, requirementFiles);

    private final UUID offeringId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final LocalDateTime slotStart = LocalDateTime.of(2026, Month.JULY, 1, 9, 0);

    private OfferingSnapshot offering(boolean paid, List<OfferingSnapshot.Requirement> reqs) {
        return new OfferingSnapshot(offeringId, tenantId, "Cita", 30, "IN_PERSON", null, null, paid, true, reqs);
    }

    private void slotIsAvailable() {
        when(availability.slotsFor(any(), any())).thenReturn(List.of(new AvailableSlot(slotStart, slotStart.plusMinutes(30))));
    }

    @Test
    void confirmsFreeBooking() {
        OfferingSnapshot offering = offering(false, List.of());
        when(catalog.findOffering(offeringId)).thenReturn(Optional.of(offering));
        slotIsAvailable();
        when(appointments.saveAndFlush(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse response = service.book(userId, new AppointmentBookingRequest(offeringId, slotStart, null));

        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.slotEnd()).isEqualTo(slotStart.plusMinutes(30));
    }

    @Test
    void leavesPaidBookingPendingPayment() {
        OfferingSnapshot offering = offering(true, List.of());
        when(catalog.findOffering(offeringId)).thenReturn(Optional.of(offering));
        slotIsAvailable();
        when(appointments.saveAndFlush(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse response = service.book(userId, new AppointmentBookingRequest(offeringId, slotStart, null));

        assertThat(response.status()).isEqualTo(AppointmentStatus.PENDING_PAYMENT);
    }

    @Test
    void rejectsUnavailableSlot() {
        OfferingSnapshot offering = offering(false, List.of());
        when(catalog.findOffering(offeringId)).thenReturn(Optional.of(offering));
        when(availability.slotsFor(any(), any())).thenReturn(List.of());

        var request = new AppointmentBookingRequest(offeringId, slotStart, null);
        assertThatThrownBy(() -> service.book(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rejectsMissingRequiredAnnex() {
        OfferingSnapshot offering = offering(false,
                List.of(new OfferingSnapshot.Requirement("vehicle_plate", "Matricula", "TEXT", true)));
        when(catalog.findOffering(offeringId)).thenReturn(Optional.of(offering));
        slotIsAvailable();

        var request = new AppointmentBookingRequest(offeringId, slotStart, Map.of());
        assertThatThrownBy(() -> service.book(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Appointment confirmed(UUID id) {
        return Appointment.book(id, new Appointment.BookingRefs(tenantId, offeringId, userId),
                new Appointment.SlotRange(slotStart, slotStart.plusMinutes(30)),
                AppointmentStatus.CONFIRMED, new Appointment.Pricing(null, null, false), List.of());
    }

    @Test
    void reschedulesToAvailableSlot() {
        UUID apptId = UUID.randomUUID();
        LocalDateTime newStart = LocalDateTime.of(2026, Month.JULY, 2, 10, 30);
        when(appointments.findByIdAndCustomerUserId(apptId, userId)).thenReturn(Optional.of(confirmed(apptId)));
        when(catalog.findOffering(offeringId)).thenReturn(Optional.of(offering(false, List.of())));
        when(availability.slotsFor(any(), any())).thenReturn(List.of(new AvailableSlot(newStart, newStart.plusMinutes(30))));
        when(appointments.saveAndFlush(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse response = service.rescheduleByCustomer(apptId, userId, newStart);

        assertThat(response.slotStart()).isEqualTo(newStart);
        assertThat(response.slotEnd()).isEqualTo(newStart.plusMinutes(30));
    }

    @Test
    void rejectsRescheduleToUnavailableSlot() {
        UUID apptId = UUID.randomUUID();
        LocalDateTime newStart = LocalDateTime.of(2026, Month.JULY, 2, 10, 30);
        when(appointments.findByIdAndCustomerUserId(apptId, userId)).thenReturn(Optional.of(confirmed(apptId)));
        when(catalog.findOffering(offeringId)).thenReturn(Optional.of(offering(false, List.of())));
        when(availability.slotsFor(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.rescheduleByCustomer(apptId, userId, newStart))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rejectsRescheduleOfCancelledAppointment() {
        UUID apptId = UUID.randomUUID();
        Appointment cancelled = Appointment.book(apptId, new Appointment.BookingRefs(tenantId, offeringId, userId),
                new Appointment.SlotRange(slotStart, slotStart.plusMinutes(30)),
                AppointmentStatus.CANCELLED, new Appointment.Pricing(null, null, false), List.of());
        when(appointments.findByIdAndCustomerUserId(apptId, userId)).thenReturn(Optional.of(cancelled));

        LocalDateTime newStart = LocalDateTime.of(2026, Month.JULY, 2, 10, 30);
        assertThatThrownBy(() -> service.rescheduleByCustomer(apptId, userId, newStart))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
    }

    private Appointment pending(UUID id) {
        return Appointment.book(id, new Appointment.BookingRefs(tenantId, offeringId, userId),
                new Appointment.SlotRange(slotStart, slotStart.plusMinutes(30)),
                AppointmentStatus.PENDING_PAYMENT, new Appointment.Pricing(null, null, true), List.of());
    }

    @Test
    void confirmPaymentMovesPendingToConfirmed() {
        UUID apptId = UUID.randomUUID();
        Appointment appointment = pending(apptId);
        when(appointments.findById(apptId)).thenReturn(Optional.of(appointment));

        assertThat(service.confirmPayment(apptId)).isEqualTo(BookingService.ConfirmationResult.CONFIRMED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void confirmPaymentIsIdempotentWhenAlreadyConfirmed() {
        UUID apptId = UUID.randomUUID();
        when(appointments.findById(apptId)).thenReturn(Optional.of(confirmed(apptId)));

        assertThat(service.confirmPayment(apptId)).isEqualTo(BookingService.ConfirmationResult.ALREADY_CONFIRMED);
    }

    @Test
    void confirmPaymentRejectsExpiredAppointment() {
        UUID apptId = UUID.randomUUID();
        Appointment expired = Appointment.book(apptId, new Appointment.BookingRefs(tenantId, offeringId, userId),
                new Appointment.SlotRange(slotStart, slotStart.plusMinutes(30)),
                AppointmentStatus.EXPIRED, new Appointment.Pricing(null, null, true), List.of());
        when(appointments.findById(apptId)).thenReturn(Optional.of(expired));

        assertThat(service.confirmPayment(apptId)).isEqualTo(BookingService.ConfirmationResult.NOT_CONFIRMABLE);
    }

    @Test
    void expiresStalePendingPayments() {
        Appointment a = pending(UUID.randomUUID());
        Appointment b = pending(UUID.randomUUID());
        when(appointments.findByStatusAndCreatedAtBefore(eq(AppointmentStatus.PENDING_PAYMENT), any()))
                .thenReturn(List.of(a, b));

        int expired = service.expireStalePendingPayments(Duration.ofMinutes(15));

        assertThat(expired).isEqualTo(2);
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.EXPIRED);
        assertThat(b.getStatus()).isEqualTo(AppointmentStatus.EXPIRED);
    }

    @Test
    void completeMovesConfirmedToCompleted() {
        UUID apptId = UUID.randomUUID();
        Appointment appointment = confirmed(apptId);
        when(appointments.findByIdAndTenantId(apptId, tenantId)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = service.completeByTenant(apptId, tenantId);

        assertThat(response.status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void noShowMovesConfirmedToNoShow() {
        UUID apptId = UUID.randomUUID();
        Appointment appointment = confirmed(apptId);
        when(appointments.findByIdAndTenantId(apptId, tenantId)).thenReturn(Optional.of(appointment));

        AppointmentResponse response = service.markNoShowByTenant(apptId, tenantId);

        assertThat(response.status()).isEqualTo(AppointmentStatus.NO_SHOW);
    }

    @Test
    void completeIsIdempotentWhenAlreadyCompleted() {
        UUID apptId = UUID.randomUUID();
        Appointment appointment = Appointment.book(apptId, new Appointment.BookingRefs(tenantId, offeringId, userId),
                new Appointment.SlotRange(slotStart, slotStart.plusMinutes(30)),
                AppointmentStatus.COMPLETED, new Appointment.Pricing(null, null, false), List.of());
        when(appointments.findByIdAndTenantId(apptId, tenantId)).thenReturn(Optional.of(appointment));

        assertThat(service.completeByTenant(apptId, tenantId).status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void completeRejectsPendingPaymentAppointment() {
        UUID apptId = UUID.randomUUID();
        when(appointments.findByIdAndTenantId(apptId, tenantId)).thenReturn(Optional.of(pending(apptId)));

        assertThatThrownBy(() -> service.completeByTenant(apptId, tenantId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void completeRejectsAppointmentOfAnotherTenant() {
        UUID apptId = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        when(appointments.findByIdAndTenantId(apptId, otherTenant)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeByTenant(apptId, otherTenant))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void capturesProvidedAnnex() {
        OfferingSnapshot offering = offering(false,
                List.of(new OfferingSnapshot.Requirement("vehicle_plate", "Matricula", "TEXT", true)));
        when(catalog.findOffering(offeringId)).thenReturn(Optional.of(offering));
        slotIsAvailable();
        when(appointments.saveAndFlush(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

        AppointmentResponse response = service.book(userId,
                new AppointmentBookingRequest(offeringId, slotStart, Map.of("vehicle_plate", "ABC123")));

        assertThat(response.requirementValues()).containsEntry("vehicle_plate", "ABC123");
    }
}
