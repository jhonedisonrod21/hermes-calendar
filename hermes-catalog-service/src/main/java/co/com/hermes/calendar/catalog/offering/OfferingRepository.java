package co.com.hermes.calendar.catalog.offering;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface OfferingRepository extends JpaRepository<Offering, UUID>, JpaSpecificationExecutor<Offering> {

    Page<Offering> findByTenantId(UUID tenantId, Pageable pageable);

    /** Búsqueda acotada al tenant: evita acceso cruzado entre establecimientos (IDOR). */
    Optional<Offering> findByIdAndTenantId(UUID id, UUID tenantId);
}
