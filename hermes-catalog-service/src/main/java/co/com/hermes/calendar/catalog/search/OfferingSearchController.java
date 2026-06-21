package co.com.hermes.calendar.catalog.search;

import co.com.hermes.calendar.catalog.offering.Modality;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Búsqueda pública de servicios ofertados (cross-tenant, solo activos). Tras el gateway queda en
 * {@code /catalog/search}. Accesible a cualquier usuario autenticado (incluido el GUEST_USER): no
 * exige rol de tenant ni acota por tenant — es exploración de catálogo, no gestión.
 */
@RestController
@RequestMapping("/search")
@Tag(name = "Catalog search", description = "Búsqueda pública de servicios ofertados activos.")
@SecurityRequirement(name = "bearer-jwt")
public class OfferingSearchController {

    private final OfferingSearchPort search;

    public OfferingSearchController(OfferingSearchPort search) {
        this.search = search;
    }

    @GetMapping
    @Operation(summary = "Busca servicios ofertados activos")
    public Page<OfferingSearchResult> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "modality", required = false) Modality modality,
            Pageable pageable
    ) {
        return search.search(new OfferingSearchCriteria(q, category, modality), pageable);
    }
}
