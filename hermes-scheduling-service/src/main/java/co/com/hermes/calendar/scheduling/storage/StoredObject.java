package co.com.hermes.calendar.scheduling.storage;

import java.util.Arrays;
import java.util.Objects;

/** Contenido y metadatos de un objeto descargado del almacén. */
public record StoredObject(byte[] content, String contentType, long size) {
    @Override
    public boolean equals(Object o) {
        return o instanceof StoredObject(byte[] otherContent, String otherType, long otherSize)
                && size == otherSize
                && Objects.equals(contentType, otherType)
                && Arrays.equals(content, otherContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentType, size) * 31 + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "StoredObject[contentType=" + contentType + ", size=" + size + "]";
    }
}
