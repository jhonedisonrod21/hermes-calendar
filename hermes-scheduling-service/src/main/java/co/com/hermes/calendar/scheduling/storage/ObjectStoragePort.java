package co.com.hermes.calendar.scheduling.storage;

/**
 * Puerto de almacenamiento de objetos (anexos de archivo). Agnóstico del proveedor: la única
 * implementación habla S3, que en local apunta a MinIO y en AWS a S3 real, sin que el resto de la
 * aplicación lo note (mismo patrón que {@code PaymentGatewayPort} con el simulador de pagos).
 */
public interface ObjectStoragePort {

    /** Sube (o reemplaza) un objeto bajo la clave dada. */
    void put(String key, byte[] content, String contentType);

    /** Descarga un objeto por su clave. Lanza si no existe. */
    StoredObject get(String key);

    /** Borra un objeto. Idempotente: no falla si la clave no existe. */
    void delete(String key);
}
