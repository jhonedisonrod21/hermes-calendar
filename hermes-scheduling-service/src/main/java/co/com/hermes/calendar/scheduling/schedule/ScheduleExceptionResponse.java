package co.com.hermes.calendar.scheduling.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Vista de una excepción de calendario.")
public record ScheduleExceptionResponse(
        UUID id,
        LocalDate date,
        ExceptionType type,
        LocalTime opensAt,
        LocalTime closesAt,
        String description,
        OffsetDateTime createdAt
) {

    public static ScheduleExceptionResponse from(ScheduleException exception) {
        return new ScheduleExceptionResponse(
                exception.getId(),
                exception.getDate(),
                exception.getType(),
                exception.getOpensAt(),
                exception.getClosesAt(),
                exception.getDescription(),
                exception.getCreatedAt()
        );
    }
}
