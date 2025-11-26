package com.ducnhu.catalog.minio;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MinioUploader {
    private final MinioClient client;
    private final MinioProperties props;

    /**
     * Đảm bảo bucket tồn tại (idempotent)
     */
    public void ensureBucket() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(props.getBucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(props.getBucket()).build());
            }
        } catch (Exception exception) {
            throw new RuntimeException("MinIO ensureBucket failed", exception);
        }
    }

    public void uploadFile(File file, String objectName) {
        try {
            client.uploadObject(UploadObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectName)
                    .filename(file.getAbsolutePath())
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO upload failed", e);
        }
    }

    /**
     * Tạo presigned URL (tải về trực tiếp, có hạn)
     */
    public String presign(String objectName, int minutes) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(props.getBucket())
                    .object(objectName)
                    .expiry(minutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO presign failed", e);
        }
    }
}
