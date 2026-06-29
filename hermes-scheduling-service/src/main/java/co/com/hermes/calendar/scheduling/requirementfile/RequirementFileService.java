package co.com.hermes.calendar.scheduling.requirementfile;

import co.com.hermes.calendar.scheduling.storage.ObjectStoragePort;
import co.com.hermes.calendar.scheduling.storage.StoredObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Gestión de los anexos de archivo de las citas: subida (a S3/MinIO), fijación a una cita al reservar
 * y descarga con control de acceso (dueño de la cita o su establecimiento). Los bytes nunca tocan la
 * base de datos: viven en el almacén de objetos y aquí solo se persisten metadatos.
 */
@Service
public class RequirementFileService {

    /** Tamaño máximo por anexo. El límite "duro" lo aplica también la config multipart de Spring. */
    static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    /** Solo se admiten PDF: se visualizan/descargan/imprimen con el visor del front. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf");

    private final ObjectStoragePort storage;
    private final AppointmentRequirementFileRepository files;

    public RequirementFileService(ObjectStoragePort storage, AppointmentRequirementFileRepository files) {
        this.storage = storage;
        this.files = files;
    }

    /** Sube un anexo y lo deja PENDING a nombre del llamante. Devuelve la referencia para la reserva. */
    @Transactional
    public RequirementFileResponse upload(UUID ownerUserId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds 10 MB");
        }
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported file type: " + contentType);
        }

        UUID id = UUID.randomUUID();
        String objectKey = "requirement-files/" + ownerUserId + "/" + id;
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file", ex);
        }
        storage.put(objectKey, bytes, contentType);

        AppointmentRequirementFile entity = AppointmentRequirementFile.pending(
                id, objectKey, safeFilename(file.getOriginalFilename()), contentType, file.getSize(), ownerUserId);
        files.save(entity);
        return new RequirementFileResponse(id, entity.getFilename(), contentType, entity.getSizeBytes());
    }

    /**
     * Valida que el anexo exista, esté PENDING y sea del cliente que reserva, y devuelve su nombre
     * (para guardarlo como valor visible del requisito). Se usa al armar la cita, antes de fijarlo.
     */
    @Transactional(readOnly = true)
    public String pendingFilename(UUID fileId, UUID ownerUserId, String reqKey) {
        return files.findByIdAndOwnerUserIdAndStatus(fileId, ownerUserId, AppointmentRequirementFile.Status.PENDING)
                .map(AppointmentRequirementFile::getFilename)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid or already used file for requirement: " + reqKey));
    }

    /**
     * Fija un anexo PENDING (propiedad del cliente que reserva) a una cita concreta. Lo invoca la
     * reserva. Devuelve el nombre del archivo, que se guarda como valor visible del requisito.
     */
    @Transactional
    public String attach(UUID fileId, UUID ownerUserId, UUID appointmentId, UUID tenantId, String reqKey) {
        AppointmentRequirementFile file = files
                .findByIdAndOwnerUserIdAndStatus(fileId, ownerUserId, AppointmentRequirementFile.Status.PENDING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid or already used file for requirement: " + reqKey));
        file.attachTo(appointmentId, tenantId, reqKey);
        return file.getFilename();
    }

    /**
     * Descarga un anexo de una cita. Acceso: el dueño del anexo (cliente que reservó) o el
     * establecimiento dueño de la cita. El front identifica qué requisitos son FILE por su tipo.
     */
    @Transactional(readOnly = true)
    public DownloadedFile download(UUID appointmentId, String reqKey, UUID callerUserId, UUID callerTenantId) {
        AppointmentRequirementFile file = files.findByAppointmentIdAndReqKey(appointmentId, reqKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        boolean isOwner = file.getOwnerUserId().equals(callerUserId);
        boolean isTenant = callerTenantId != null && callerTenantId.equals(file.getTenantId());
        if (!isOwner && !isTenant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this attachment");
        }
        StoredObject object = storage.get(file.getObjectKey());
        return new DownloadedFile(file.getFilename(), object.contentType(), object.content());
    }

    /** Nombre seguro para mostrar/descargar: sin rutas ni saltos, acotado en longitud. */
    private static String safeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "anexo";
        }
        String base = original.replace("\\", "/");
        base = base.substring(base.lastIndexOf('/') + 1).strip();
        base = base.replaceAll("[\\r\\n\"]", "_");
        return base.length() > 200 ? base.substring(0, 200) : base;
    }

    /** Contenido y metadatos para servir una descarga. */
    public record DownloadedFile(String filename, String contentType, byte[] content) {
    }
}
