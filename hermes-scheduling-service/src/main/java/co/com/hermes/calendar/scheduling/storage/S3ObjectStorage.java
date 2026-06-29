package co.com.hermes.calendar.scheduling.storage;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Implementación S3 del puerto de almacenamiento (MinIO en local, S3 real en AWS). */
@Component
public class S3ObjectStorage implements ObjectStoragePort {

    private final S3Client s3;
    private final StorageProperties props;

    public S3ObjectStorage(S3Client s3, StorageProperties props) {
        this.s3 = s3;
        this.props = props;
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(props.bucket())
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content));
    }

    @Override
    public StoredObject get(String key) {
        try {
            ResponseBytes<GetObjectResponse> object = s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(key)
                    .build());
            GetObjectResponse meta = object.response();
            return new StoredObject(object.asByteArray(), meta.contentType(), meta.contentLength());
        } catch (NoSuchKeyException ex) {
            throw new IllegalStateException("Object not found in storage: " + key, ex);
        }
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(props.bucket())
                .key(key)
                .build());
    }
}
