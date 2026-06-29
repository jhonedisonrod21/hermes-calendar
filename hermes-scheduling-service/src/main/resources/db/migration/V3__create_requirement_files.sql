-- Anexos de tipo FILE de las citas. Los bytes viven en el almacén de objetos (S3/MinIO); aquí solo
-- guardamos los metadatos y la clave del objeto. Ciclo de vida:
--   PENDING  -> recién subido por su dueño, aún no ligado a una cita
--   ATTACHED -> fijado a una cita (appointment_id + req_key) al reservar
-- Los PENDING que nunca se adjuntan son huérfanos y pueden purgarse en segundo plano (fuera de alcance).
CREATE TABLE IF NOT EXISTS appointment_requirement_files (
    id CHAR(36) PRIMARY KEY,
    object_key VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    owner_user_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    appointment_id CHAR(36),
    tenant_id CHAR(36),
    req_key VARCHAR(80),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_arf_object_key (object_key)
);

CREATE INDEX idx_arf_appointment ON appointment_requirement_files (appointment_id, req_key);
CREATE INDEX idx_arf_owner_status ON appointment_requirement_files (owner_user_id, status);
