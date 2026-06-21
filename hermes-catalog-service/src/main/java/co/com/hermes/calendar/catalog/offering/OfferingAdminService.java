package co.com.hermes.calendar.catalog.offering;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Gestión del catálogo de servicios de un establecimiento. Todas las operaciones se acotan al
 * {@code tenantId} del token; nunca se confía en input del cliente para el tenant.
 */
@Service
public class OfferingAdminService {

    private final OfferingRepository offerings;

    public OfferingAdminService(OfferingRepository offerings) {
        this.offerings = offerings;
    }

    @Transactional
    public OfferingResponse create(CallerTenant tenant, OfferingRequest request) {
        String currency = normalizedCurrency(request);
        Offering offering = Offering.create(
                UUID.randomUUID(),
                tenant,
                request.name().trim(),
                request.description(),
                request.category(),
                request.durationMinutes(),
                request.modality(),
                request.priceAmount(),
                currency,
                request.requiresOnlinePayment()
        );
        offering.replaceRequirements(toRequirements(request.requirements()));
        return OfferingResponse.from(offerings.save(offering));
    }

    @Transactional(readOnly = true)
    public Page<OfferingResponse> list(UUID tenantId, Pageable pageable) {
        return offerings.findByTenantId(tenantId, pageable).map(OfferingResponse::from);
    }

    @Transactional(readOnly = true)
    public OfferingResponse get(UUID tenantId, UUID id) {
        return OfferingResponse.from(require(tenantId, id));
    }

    @Transactional
    public OfferingResponse update(CallerTenant tenant, UUID id, OfferingRequest request) {
        Offering offering = require(tenant.id(), id);
        String currency = normalizedCurrency(request);
        offering.update(
                tenant,
                request.name().trim(),
                request.description(),
                request.category(),
                request.durationMinutes(),
                request.modality(),
                request.priceAmount(),
                currency,
                request.requiresOnlinePayment()
        );
        offering.replaceRequirements(toRequirements(request.requirements()));
        return OfferingResponse.from(offering);
    }

    @Transactional
    public OfferingResponse changeActive(UUID tenantId, UUID id, boolean active) {
        Offering offering = require(tenantId, id);
        offering.changeActive(active);
        return OfferingResponse.from(offering);
    }

    private Offering require(UUID tenantId, UUID id) {
        return offerings.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offering not found"));
    }

    private static String normalizedCurrency(OfferingRequest request) {
        if (request.requiresOnlinePayment()
                && (request.priceAmount() == null || request.priceCurrency() == null || request.priceCurrency().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "price amount and currency are required when online payment is enabled");
        }
        return request.priceCurrency() == null ? null : request.priceCurrency().trim().toUpperCase(Locale.ROOT);
    }

    private static List<OfferingRequirement> toRequirements(List<RequirementDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(dto -> new OfferingRequirement(
                        UUID.randomUUID(), dto.key().trim(), dto.label().trim(), dto.type(), dto.required(), dto.displayOrder()))
                .toList();
    }
}
