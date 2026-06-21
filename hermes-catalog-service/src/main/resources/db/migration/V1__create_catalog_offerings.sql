-- Catálogo de servicios ofertados por cada establecimiento (tenant). Multi-tenant: toda fila
-- lleva tenant_id y el servicio filtra siempre por el tenant del token.
CREATE TABLE IF NOT EXISTS offerings (
    id CHAR(36) PRIMARY KEY,
    tenant_id CHAR(36) NOT NULL,
    -- Datos públicos del establecimiento denormalizados (capturados del JWT al crear/editar) para
    -- mostrar en la búsqueda pública sin llamar a tenant-service. Consistencia eventual ante renombrado.
    tenant_slug VARCHAR(80),
    tenant_name VARCHAR(160),
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(80),
    duration_minutes INT NOT NULL,
    modality VARCHAR(20) NOT NULL,
    price_amount DECIMAL(12,2),
    price_currency VARCHAR(3),
    requires_online_payment BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)
);

CREATE INDEX idx_offerings_tenant ON offerings (tenant_id);

-- Anexos/datos obligatorios al reservar este servicio (p. ej. "copia de documento" = FILE,
-- "matricula del vehiculo" = TEXT). Definicion tipada; los valores se capturan al agendar.
CREATE TABLE IF NOT EXISTS offering_requirements (
    id CHAR(36) PRIMARY KEY,
    offering_id CHAR(36) NOT NULL,
    req_key VARCHAR(80) NOT NULL,
    label VARCHAR(160) NOT NULL,
    type VARCHAR(20) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    UNIQUE (offering_id, req_key),
    CONSTRAINT fk_offering_requirements_offering
        FOREIGN KEY (offering_id) REFERENCES offerings(id) ON DELETE CASCADE
);
