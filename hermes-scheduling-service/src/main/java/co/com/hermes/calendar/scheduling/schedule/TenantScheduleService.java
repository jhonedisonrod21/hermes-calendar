package co.com.hermes.calendar.scheduling.schedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Horario laboral y excepciones (feriados/cierres) de un establecimiento. El {@code tenantId}
 * proviene siempre del token; las operaciones nunca cruzan a otro establecimiento.
 */
@Service
public class TenantScheduleService {

    private final BusinessHoursRepository businessHours;
    private final ScheduleExceptionRepository exceptions;

    public TenantScheduleService(BusinessHoursRepository businessHours, ScheduleExceptionRepository exceptions) {
        this.businessHours = businessHours;
        this.exceptions = exceptions;
    }

    @Transactional(readOnly = true)
    public List<BusinessHoursDto> getHours(UUID tenantId) {
        return businessHours.findByTenantIdOrderByDayOfWeekAscOpensAtAsc(tenantId).stream()
                .map(BusinessHoursDto::from)
                .toList();
    }

    /** Reemplaza el horario semanal completo. */
    @Transactional
    public List<BusinessHoursDto> replaceHours(UUID tenantId, BusinessScheduleRequest request) {
        request.hours().forEach(slot -> validateRange(slot.opensAt(), slot.closesAt()));
        businessHours.deleteByTenantId(tenantId);
        List<BusinessHours> saved = businessHours.saveAll(request.hours().stream()
                .map(slot -> BusinessHours.of(UUID.randomUUID(), tenantId, slot.dayOfWeek(), slot.opensAt(), slot.closesAt()))
                .toList());
        return saved.stream().map(BusinessHoursDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<ScheduleExceptionResponse> listExceptions(UUID tenantId, Pageable pageable) {
        return exceptions.findByTenantIdOrderByDateAsc(tenantId, pageable).map(ScheduleExceptionResponse::from);
    }

    @Transactional
    public ScheduleExceptionResponse addException(UUID tenantId, ScheduleExceptionRequest request) {
        if (exceptions.existsByTenantIdAndDate(tenantId, request.date())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An exception already exists for this date");
        }
        LocalTime opensAt = null;
        LocalTime closesAt = null;
        if (request.type() == ExceptionType.SPECIAL_HOURS) {
            if (request.opensAt() == null || request.closesAt() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "opensAt and closesAt are required for SPECIAL_HOURS");
            }
            validateRange(request.opensAt(), request.closesAt());
            opensAt = request.opensAt();
            closesAt = request.closesAt();
        }
        ScheduleException exception = ScheduleException.of(
                UUID.randomUUID(), tenantId, request.date(), request.type(), opensAt, closesAt, request.description());
        return ScheduleExceptionResponse.from(exceptions.save(exception));
    }

    @Transactional
    public void removeException(UUID tenantId, UUID id) {
        ScheduleException exception = exceptions.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exception not found"));
        exceptions.delete(exception);
    }

    private static void validateRange(LocalTime opensAt, LocalTime closesAt) {
        if (!opensAt.isBefore(closesAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "opensAt must be before closesAt");
        }
    }
}
