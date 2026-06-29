package co.com.hermes.calendar.scheduling.storage;

/** Contenido y metadatos de un objeto descargado del almacén. */
public record StoredObject(byte[] content, String contentType, long size) {
}
