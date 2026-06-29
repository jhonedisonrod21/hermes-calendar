package co.com.hermes.calendar.catalog.search;

import co.com.hermes.calendar.catalog.offering.CallerTenant;
import co.com.hermes.calendar.catalog.offering.Modality;
import co.com.hermes.calendar.catalog.offering.Offering;
import co.com.hermes.calendar.catalog.offering.OfferingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MySqlOfferingSearchTest {

    private final OfferingRepository offerings = mock(OfferingRepository.class);
    private final MySqlOfferingSearch search = new MySqlOfferingSearch(offerings);

    @Test
    @SuppressWarnings("unchecked")
    void returnsPublicProjectionWithEstablishment() {
        UUID tenantId = UUID.randomUUID();
        Offering offering = Offering.create(
                UUID.randomUUID(),
                new CallerTenant(tenantId, "cafe-central", "Cafe Central"),
                new Offering.OfferingDetails("Limpieza dental", "Profilaxis", "Odontologia", 30, Modality.IN_PERSON),
                new Offering.Pricing(null, null, false));
        when(offerings.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(offering)));

        Page<OfferingSearchResult> page = search.search(
                new OfferingSearchCriteria("dental", null, null), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        OfferingSearchResult result = page.getContent().get(0);
        assertThat(result.name()).isEqualTo("Limpieza dental");
        assertThat(result.tenantSlug()).isEqualTo("cafe-central");
        assertThat(result.tenantName()).isEqualTo("Cafe Central");
        assertThat(result.tenantId()).isEqualTo(tenantId);
    }
}
