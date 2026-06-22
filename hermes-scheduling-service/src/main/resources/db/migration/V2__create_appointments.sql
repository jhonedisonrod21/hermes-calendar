-- Citas reservadas. tenant_id proviene del servicio (offering), no del cliente. El cliente
-- (customer_user_id) es el usuario que reserva (p. ej. un GUEST_USER).
CREATE TABLE IF NOT EXISTS appointments (
    id CHAR(36) PRIMARY KEY,
    tenant_id CHAR(36) NOT NULL,
    offering_id CHAR(36) NOT NULL,
    customer_user_id CHAR(36) NOT NULL,
    slot_start DATETIME(6) NOT NULL,
    slot_end DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    price_amount DECIMAL(12,2),
    price_currency VARCHAR(3),
    requires_online_payment BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6),
    -- Clave de cupo "activo": solo definida para citas que retienen el cupo (PENDING_PAYMENT/CONFIRMED).
    -- El índice único sobre ella impide la doble reserva del mismo (servicio, hora) sin bloquear el
    -- re-uso del cupo tras una cancelación (NULL no colisiona en UNIQUE).
    active_slot_key VARCHAR(90) GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING_PAYMENT','CONFIRMED')
             THEN CONCAT(offering_id, ':', slot_start) END) STORED,
    UNIQUE KEY uq_appointments_active_slot (active_slot_key)
);

CREATE INDEX idx_appointments_tenant ON appointments (tenant_id);
CREATE INDEX idx_appointments_offering_start ON appointments (offering_id, slot_start);

-- Valores de los anexos exigidos por el servicio, capturados al reservar.
CREATE TABLE IF NOT EXISTS appointment_requirement_values (
    id CHAR(36) PRIMARY KEY,
    appointment_id CHAR(36) NOT NULL,
    req_key VARCHAR(80) NOT NULL,
    value VARCHAR(1000),
    UNIQUE (appointment_id, req_key),
    CONSTRAINT fk_arv_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE
);
