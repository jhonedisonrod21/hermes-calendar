package co.com.hermes.calendar.scheduling.requirementfile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppointmentRequirementFileRepository extends JpaRepository<AppointmentRequirementFile, UUID> {

    /** Un anexo aún sin adjuntar, propiedad del llamante (para validarlo al reservar). */
    Optional<AppointmentRequirementFile> findByIdAndOwnerUserIdAndStatus(
            UUID id, UUID ownerUserId, AppointmentRequirementFile.Status status);

    /** El anexo ya fijado a una cita y a una clave de requisito (para la descarga). */
    Optional<AppointmentRequirementFile> findByAppointmentIdAndReqKey(UUID appointmentId, String reqKey);
}
