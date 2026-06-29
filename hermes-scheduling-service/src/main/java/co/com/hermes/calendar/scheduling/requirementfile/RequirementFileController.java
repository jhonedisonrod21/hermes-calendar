package co.com.hermes.calendar.scheduling.requirementfile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.UUID;

/**
 * Anexos de archivo de las citas. Tras el gateway: {@code /scheduling/...}.
 * <ul>
 *   <li>Subida ({@code POST /requirement-files}): cualquier usuario autenticado (incl. GUEST_USER que
 *       reserva). El archivo queda PENDING a su nombre y se referencia luego en la reserva.</li>
 *   <li>Descarga ({@code GET /appointments/{id}/requirements/{key}/file}): el servicio valida que el
 *       llamante sea el dueño del anexo o el establecimiento dueño de la cita.</li>
 * </ul>
 */
@RestController
@Tag(name = "Requirement files", description = "Anexos de archivo de las citas (subida y descarga).")
@SecurityRequirement(name = "bearer-jwt")
public class RequirementFileController {

    private final RequirementFileService files;

    public RequirementFileController(RequirementFileService files) {
        this.files = files;
    }

    @PostMapping(path = "/requirement-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Sube un anexo de archivo (queda listo para referenciarse en la reserva)")
    public RequirementFileResponse upload(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam("file") MultipartFile file) {
        return files.upload(callerUserId(jwt), file);
    }

    @GetMapping("/appointments/{appointmentId}/requirements/{reqKey}/file")
    @Operation(summary = "Descarga el anexo de archivo de un requisito de una cita")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID appointmentId,
                                             @PathVariable String reqKey) {
        RequirementFileService.DownloadedFile file =
                files.download(appointmentId, reqKey, callerUserId(jwt), callerTenantId(jwt));
        String encoded = URLEncoder.encode(file.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(parseContentType(file.contentType()))
                .body(new ByteArrayResource(file.content()));
    }

    private static MediaType parseContentType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException _) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /** Usuario del llamante, del token (claim user_id, con fallback a sub). */
    private static UUID callerUserId(Jwt jwt) {
        String userId = jwt.getClaimAsString("user_id");
        if (userId == null || userId.isBlank()) {
            userId = jwt.getSubject();
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException _) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid user identity");
        }
    }

    /** Tenant del llamante si lo hay (un GUEST_USER cliente no lo tiene): null cuando ausente/ inválido. */
    private static UUID callerTenantId(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException _) {
            return null;
        }
    }
}
