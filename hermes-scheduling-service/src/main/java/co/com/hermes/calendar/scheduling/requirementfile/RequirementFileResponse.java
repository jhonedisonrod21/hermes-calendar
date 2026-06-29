package co.com.hermes.calendar.scheduling.requirementfile;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Referencia a un anexo recién subido. El {@code fileId} se envía como valor del requisito al reservar. */
@Schema(description = "Anexo subido, listo para referenciarse en la reserva.")
public record RequirementFileResponse(UUID fileId, String filename, String contentType, long size) {
}
