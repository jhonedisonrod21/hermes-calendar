package co.com.hermes.calendar.catalog.offering;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OfferingAdminServiceTest {

    private final OfferingRepository offerings = mock(OfferingRepository.class);
    private final OfferingAdminService service = new OfferingAdminService(offerings);

    private final UUID tenantId = UUID.randomUUID();
    private final CallerTenant caller = new CallerTenant(tenantId, "cafe-central", "Cafe Central");

    private OfferingRequest request(boolean onlinePayment, BigDecimal price, String currency) {
        return new OfferingRequest(
                "Cita de odontologia general",
                "Revision general",
                "Odontologia",
                30,
                Modality.IN_PERSON,
                price,
                currency,
                onlinePayment,
                List.of(new RequirementDto("vehicle_plate", "Matricula", RequirementType.TEXT, true, 0))
        );
    }

    @Test
    void registersOfferingScopedToTenant() {
        when(offerings.save(any(Offering.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OfferingResponse response = service.create(caller, request(true, new BigDecimal("80000.00"), "cop"));

        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.name()).isEqualTo("Cita de odontologia general");
        assertThat(response.modality()).isEqualTo(Modality.IN_PERSON);
        assertThat(response.priceCurrency()).isEqualTo("COP");
        assertThat(response.active()).isTrue();
        assertThat(response.requirements()).hasSize(1);
        assertThat(response.requirements().get(0).key()).isEqualTo("vehicle_plate");
        assertThat(response.requirements().get(0).required()).isTrue();
    }

    @Test
    void rejectsOnlinePaymentWithoutPrice() {
        assertThatThrownBy(() -> service.create(caller, request(true, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getFailsForOtherTenant() {
        UUID id = UUID.randomUUID();
        when(offerings.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(tenantId, id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deactivatesOffering() {
        UUID id = UUID.randomUUID();
        Offering offering = Offering.create(id, caller, "X", null, null, 30, Modality.VIRTUAL, null, null, false);
        when(offerings.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(offering));

        OfferingResponse response = service.changeActive(tenantId, id, false);

        assertThat(response.active()).isFalse();
        assertThat(offering.isActive()).isFalse();
    }
}
