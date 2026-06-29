package co.com.hermes.calendar.scheduling.requirementfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Anexo de archivo de una cita. Los bytes viven en el almacén de objetos (S3/MinIO); esta entidad
 * guarda los metadatos y la clave del objeto. Nace {@code PENDING} (subido por su dueño) y pasa a
 * {@code ATTACHED} al fijarse a una cita concreta durante la reserva.
 */
@Entity
@Table(name = "appointment_requirement_files")
public class AppointmentRequirementFile {

    public enum Status { PENDING, ATTACHED }

    @Id
    private UUID id;

    @Column(name = "object_key", nullable = false, length = 255)
    private String objectKey;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "req_key", length = 80)
    private String reqKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AppointmentRequirementFile() {
    }

    /** Crea un anexo recién subido (sin ligar a una cita todavía). */
    public static AppointmentRequirementFile pending(UUID id, String objectKey, String filename,
                                                     String contentType, long sizeBytes, UUID ownerUserId) {
        AppointmentRequirementFile file = new AppointmentRequirementFile();
        file.id = id;
        file.objectKey = objectKey;
        file.filename = filename;
        file.contentType = contentType;
        file.sizeBytes = sizeBytes;
        file.ownerUserId = ownerUserId;
        file.status = Status.PENDING;
        file.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        return file;
    }

    /** Fija el anexo a una cita concreta (lo hace la reserva). */
    public void attachTo(UUID appointmentId, UUID tenantId, String reqKey) {
        this.appointmentId = appointmentId;
        this.tenantId = tenantId;
        this.reqKey = reqKey;
        this.status = Status.ATTACHED;
    }

    public UUID getId() {
        return id;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public Status getStatus() {
        return status;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getReqKey() {
        return reqKey;
    }
}
