package org.kirya343.features.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.kirya343.features.storage.config.S3Properties;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@RequiredArgsConstructor
@Service
public class S3StorageService {

    private final S3Client s3Client;

    private final S3Properties s3Properties;
    
    public String upload(
        InputStream inputStream,
        String fileName,
        long size,
        String contentType,
        String dir
    ) throws IOException {

        String newFileName = fileName != null ? fileName : UUID.randomUUID().toString();

        String key = dir + "/" + newFileName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .contentType(contentType)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();

        s3Client.putObject(
                request,
                RequestBody.fromInputStream(inputStream, size)
        );

        return key;
    }

    public void delete(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(objectKey)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (NoSuchKeyException e) {
            // файл уже отсутствует — ничего не делаем
        }
    }

    public InputStream download(String objectKey) throws IOException {
        try {
            return s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(objectKey)
                    .build()
            );
        } catch (NoSuchKeyException e) {
            throw new IOException(
                "S3 object not found: " + objectKey,
                e
            );
        }
    }

    public List<S3Object> listFiles(String prefix) {

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(s3Properties.bucket())
                .prefix(prefix)
                .build();

        return s3Client.listObjectsV2(request).contents();
    }

    public byte[] downloadBytes(String objectKey) throws IOException {
        try (InputStream input = download(objectKey)) {
            return input.readAllBytes();
        }
    }
}
