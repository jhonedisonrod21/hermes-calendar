package co.com.hermes.calendar.scheduling.schedule;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantScheduleServiceTest {

    private final BusinessHoursRepository businessHours = mock(BusinessHoursRepository.class);
    private final ScheduleExceptionRepository exceptions = mock(ScheduleExceptionRepository.class);
    private final TenantScheduleService service = new TenantScheduleService(businessHours, exceptions);

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void replacesWeeklyHours() {
        when(businessHours.saveAll(any())).thenAnswer(invocation -> {
            Iterable<BusinessHours> arg = invocation.getArgument(0);
            List<BusinessHours> list = new java.util.ArrayList<>();
            arg.forEach(list::add);
            return list;
        });
        BusinessScheduleRequest request = new BusinessScheduleRequest(List.of(
                new BusinessHoursDto(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new BusinessHoursDto(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0))
        ));

        List<BusinessHoursDto> result = service.replaceHours(tenantId, request);

        verify(businessHours).deleteByTenantId(tenantId);
        assertThat(result).hasSize(2);
    }

    @Test
    void rejectsInvalidTimeRange() {
        BusinessScheduleRequest request = new BusinessScheduleRequest(List.of(
                new BusinessHoursDto(DayOfWeek.TUESDAY, LocalTime.of(18, 0), LocalTime.of(9, 0))
        ));

        assertThatThrownBy(() -> service.replaceHours(tenantId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void registersHolidayClosed() {
        when(exceptions.existsByTenantIdAndDate(tenantId, LocalDate.of(2026, Month.DECEMBER, 25))).thenReturn(false);
        when(exceptions.save(any(ScheduleException.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleExceptionResponse response = service.addException(tenantId,
                new ScheduleExceptionRequest(LocalDate.of(2026, Month.DECEMBER, 25), ExceptionType.CLOSED, null, null, "Navidad"));

        assertThat(response.type()).isEqualTo(ExceptionType.CLOSED);
        assertThat(response.opensAt()).isNull();
        assertThat(response.description()).isEqualTo("Navidad");
    }

    @Test
    void rejectsSpecialHoursWithoutTimes() {
        when(exceptions.existsByTenantIdAndDate(any(), any())).thenReturn(false);

        var request = new ScheduleExceptionRequest(LocalDate.of(2026, Month.DECEMBER, 24), ExceptionType.SPECIAL_HOURS, null, null, "Vispera");
        assertThatThrownBy(() -> service.addException(tenantId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsDuplicateExceptionDate() {
        when(exceptions.existsByTenantIdAndDate(tenantId, LocalDate.of(2026, Month.JANUARY, 1))).thenReturn(true);

        var request = new ScheduleExceptionRequest(LocalDate.of(2026, Month.JANUARY, 1), ExceptionType.CLOSED, null, null, "Anio nuevo");
        assertThatThrownBy(() -> service.addException(tenantId, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void failsToRemoveMissingException() {
        UUID id = UUID.randomUUID();
        when(exceptions.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeException(tenantId, id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }
}
