package co.com.hermes.calendar.catalog.search;

import co.com.hermes.calendar.catalog.offering.Modality;
import co.com.hermes.calendar.catalog.offering.Offering;
import co.com.hermes.calendar.catalog.offering.OfferingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Implementación MySQL del puerto de búsqueda: filtros con JPA Specification sobre la fuente de
 * verdad (sin índice secundario que sincronizar). Para relevancia/typo-tolerance a escala se
 * reemplazaría por FULLTEXT o un motor (Meilisearch/OpenSearch) sin tocar el resto.
 */
@Component
public class MySqlOfferingSearch implements OfferingSearchPort {

    private final OfferingRepository offerings;

    public MySqlOfferingSearch(OfferingRepository offerings) {
        this.offerings = offerings;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferingSearchResult> search(OfferingSearchCriteria criteria, Pageable pageable) {
        Specification<Offering> spec = activeOnly();
        if (StringUtils.hasText(criteria.q())) {
            spec = spec.and(textMatches(criteria.q().trim()));
        }
        if (StringUtils.hasText(criteria.category())) {
            spec = spec.and(categoryEquals(criteria.category().trim()));
        }
        if (criteria.modality() != null) {
            spec = spec.and(modalityMatches(criteria.modality()));
        }
        return offerings.findAll(spec, pageable).map(OfferingSearchResult::from);
    }

    private static Specification<Offering> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    private static Specification<Offering> textMatches(String q) {
        String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("description")), like),
                cb.like(cb.lower(root.get("category")), like));
    }

    private static Specification<Offering> categoryEquals(String category) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), category.toLowerCase(Locale.ROOT));
    }

    private static Specification<Offering> modalityMatches(Modality modality) {
        // BOTH cubre presencial y virtual: filtrar por IN_PERSON o VIRTUAL incluye los servicios BOTH.
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("modality"), modality),
                cb.equal(root.get("modality"), Modality.BOTH));
    }
}
