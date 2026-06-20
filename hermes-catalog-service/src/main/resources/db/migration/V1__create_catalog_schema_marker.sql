CREATE TABLE IF NOT EXISTS catalog_schema_marker (
    id SMALLINT PRIMARY KEY,
    description VARCHAR(120) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

INSERT IGNORE INTO catalog_schema_marker (id, description)
VALUES (1, 'Hermes catalog service schema initialized');
