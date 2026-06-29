package co.com.hermes.calendar.catalog.offering;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Servicio ofertado por un establecimiento (tenant). */
@Entity
@Table(name = "offerings")
public class Offering {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tenant_slug", length = 80)
    private String tenantSlug;

    @Column(name = "tenant_name", length = 160)
    private String tenantName;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 80)
    private String category;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modality modality;

    @Column(name = "price_amount", precision = 12, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", length = 3)
    private String priceCurrency;

    @Column(name = "requires_online_payment", nullable = false)
    private boolean requiresOnlinePayment;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "offering_id", nullable = false)
    @OrderBy("displayOrder ASC")
    private List<OfferingRequirement> requirements = new ArrayList<>();

    protected Offering() {
    }

    /** Atributos descriptivos del servicio (salvo identidad, tenant y precio). */
    public record OfferingDetails(String name, String description, String category, int durationMinutes,
                                  Modality modality) {
    }

    /** Precio y modalidad de cobro del servicio. */
    public record Pricing(BigDecimal priceAmount, String priceCurrency, boolean requiresOnlinePayment) {
    }

    public static Offering create(UUID id, CallerTenant tenant, OfferingDetails details, Pricing pricing) {
        Offering offering = new Offering();
        offering.id = id;
        offering.tenantId = tenant.id();
        offering.tenantSlug = tenant.slug();
        offering.tenantName = tenant.name();
        offering.applyDetails(details);
        offering.applyPricing(pricing);
        offering.active = true;
        offering.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        offering.updatedAt = offering.createdAt;
        return offering;
    }

    private void applyDetails(OfferingDetails details) {
        this.name = details.name();
        this.description = details.description();
        this.category = details.category();
        this.durationMinutes = details.durationMinutes();
        this.modality = details.modality();
    }

    private void applyPricing(Pricing pricing) {
        this.priceAmount = pricing.priceAmount();
        this.priceCurrency = pricing.priceCurrency();
        this.requiresOnlinePayment = pricing.requiresOnlinePayment();
    }

    public void update(CallerTenant tenant, OfferingDetails details, Pricing pricing) {
        this.tenantSlug = tenant.slug();
        this.tenantName = tenant.name();
        applyDetails(details);
        applyPricing(pricing);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void replaceRequirements(List<OfferingRequirement> newRequirements) {
        this.requirements.clear();
        this.requirements.addAll(newRequirements);
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void changeActive(boolean active) {
        this.active = active;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public Modality getModality() {
        return modality;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public String getPriceCurrency() {
        return priceCurrency;
    }

    public boolean isRequiresOnlinePayment() {
        return requiresOnlinePayment;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<OfferingRequirement> getRequirements() {
        return requirements;
    }
}
