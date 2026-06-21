-- Horario de trabajo del establecimiento (tenant): franjas por día de la semana. Varias filas por
-- día modelan turno partido / hueco de almuerzo (p. ej. 08:00-12:00 y 14:00-18:00 = almuerzo libre).
CREATE TABLE IF NOT EXISTS business_hours (
    id CHAR(36) PRIMARY KEY,
    tenant_id CHAR(36) NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    opens_at TIME NOT NULL,
    closes_at TIME NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_business_hours_tenant ON business_hours (tenant_id);

-- Excepciones por fecha: feriados/cierres (CLOSED) u horarios especiales (SPECIAL_HOURS).
CREATE TABLE IF NOT EXISTS schedule_exceptions (
    id CHAR(36) PRIMARY KEY,
    tenant_id CHAR(36) NOT NULL,
    exception_date DATE NOT NULL,
    type VARCHAR(20) NOT NULL,
    opens_at TIME,
    closes_at TIME,
    description VARCHAR(200),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE (tenant_id, exception_date)
);

CREATE INDEX idx_schedule_exceptions_tenant ON schedule_exceptions (tenant_id);
