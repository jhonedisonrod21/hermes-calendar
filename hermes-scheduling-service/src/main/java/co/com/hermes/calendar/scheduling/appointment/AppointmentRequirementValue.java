package co.com.hermes.calendar.scheduling.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Valor aportado por el cliente para un anexo exigido por el servicio. */
@Entity
@Table(name = "appointment_requirement_values")
public class AppointmentRequirementValue {

    @Id
    private UUID id;

    @Column(name = "req_key", nullable = false, length = 80)
    private String key;

    @Column(length = 1000)
    private String value;

    protected AppointmentRequirementValue() {
    }

    public AppointmentRequirementValue(UUID id, String key, String value) {
        this.id = id;
        this.key = key;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
