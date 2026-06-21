package co.com.hermes.calendar.scheduling.schedule;

/** Tipo de excepción de calendario para una fecha concreta. */
public enum ExceptionType {
    /** Día no laborable (feriado o cierre). */
    CLOSED,
    /** Horario distinto al habitual para esa fecha. */
    SPECIAL_HOURS
}
