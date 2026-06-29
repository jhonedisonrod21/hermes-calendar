package co.com.hermes.calendar.scheduling.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del almacén S3 ({@code hermes.storage.s3.*}). En el perfil local se inyecta el
 * endpoint de MinIO con credenciales estáticas y acceso path-style; en AWS basta el bucket y la
 * región (endpoint/credenciales vacíos -> S3 real con la cadena de credenciales por defecto del rol).
 *
 * @param bucket          bucket donde se guardan los anexos
 * @param region          región AWS (p. ej. us-east-1)
 * @param endpoint        endpoint S3 a sobrescribir (MinIO en local); vacío en AWS
 * @param accessKey       access key estática (MinIO en local); vacía en AWS (usa rol/IMDS)
 * @param secretKey       secret key estática (MinIO en local); vacía en AWS
 * @param pathStyleAccess true para MinIO (no soporta virtual-host buckets); false en AWS
 */
@ConfigurationProperties("hermes.storage.s3")
public record StorageProperties(
        String bucket,
        String region,
        String endpoint,
        String accessKey,
        String secretKey,
        boolean pathStyleAccess
) {
}
