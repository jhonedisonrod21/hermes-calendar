# hermes-calendar — Dominio de calendario

Servicios de **dominio de negocio** del calendario. Actualmente son *scaffolds*
(esqueletos) listos para implementar.

## Propósito
Agrupar la lógica de negocio del calendario: catálogo de recursos reservables,
agendamiento/disponibilidad e integraciones externas.

## Servicios
| Servicio | Puerto | Responsabilidad (prevista) |
|----------|--------|----------------------------|
| hermes-catalog-service | 8087 | Catálogo de recursos/servicios reservables |
| hermes-scheduling-service | 8083 | Agendamiento y disponibilidad (incluye Quartz) |
| hermes-integration-hub-service | 8086 | Integraciones externas (mail / WebClient) |

## Consideraciones técnicas
- **Base de datos `HERMES_CALENDAR`** (MySQL). Flyway por servicio.
- Cada servicio es un **resource server JWT**: valida el token emitido por
  `hermes-security`, normalmente a través del `api-gateway`.
- Autorización por roles/permisos presentes en el JWT (claims Hermes).
- **Estado actual:** scaffolds. `catalog` tiene una tabla marcador; los demás aún
  sin migraciones ni entidades. Al implementar: añadir entidades JPA + migraciones
  Flyway + el overlay `{servicio}-dev.yml` con el datasource en fail-fast.
- No depende de `hermes-shared` (no hace llamadas internas IAM).

## Construir / correr
```bash
./gradlew build
# Para incluirlos en el arranque local: ../hermes-stack.sh up con perfil 'all'
```

## Stack
Java 25 · Gradle 9.5 · Spring Boot 4.0.6 / Spring Cloud 2025.1.1.
