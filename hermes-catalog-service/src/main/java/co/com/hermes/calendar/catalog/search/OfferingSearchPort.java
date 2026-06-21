package co.com.hermes.calendar.catalog.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de búsqueda de servicios. Aísla el motor de búsqueda del resto del servicio: hoy una
 * implementación MySQL ({@code MySqlOfferingSearch}); mañana se puede enchufar Meilisearch/OpenSearch
 * sin tocar controllers ni dominio.
 */
public interface OfferingSearchPort {

    Page<OfferingSearchResult> search(OfferingSearchCriteria criteria, Pageable pageable);
}
