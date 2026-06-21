package co.com.hermes.calendar.catalog.offering;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Anexo/dato obligatorio (u opcional) que debe aportarse al reservar el servicio. */
@Entity
@Table(name = "offering_requirements")
public class OfferingRequirement {

    @Id
    private UUID id;

    @Column(name = "req_key", nullable = false, length = 80)
    private String key;

    @Column(nullable = false, length = 160)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequirementType type;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected OfferingRequirement() {
    }

    public OfferingRequirement(UUID id, String key, String label, RequirementType type, boolean required, int displayOrder) {
        this.id = id;
        this.key = key;
        this.label = label;
        this.type = type;
        this.required = required;
        this.displayOrder = displayOrder;
    }

    public UUID getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public RequirementType getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
