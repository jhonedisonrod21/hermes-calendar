package co.com.hermes.calendar.scheduling.appointment;

/** Ciclo de vida de una cita. PENDING_PAYMENT y CONFIRMED retienen el cupo (no se puede re-reservar). */
public enum AppointmentStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    NO_SHOW,
    EXPIRED;

    public boolean holdsSlot() {
        return this == PENDING_PAYMENT || this == CONFIRMED;
    }
}
