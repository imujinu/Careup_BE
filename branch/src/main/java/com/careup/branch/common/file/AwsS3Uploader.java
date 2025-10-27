package com.careup.branch.common.file;

import com.careup.branch.common.exception.FileHandlingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwsS3Uploader {

    private static final String PATH_IMAGE = "image";
    private static final String PATH_AUDIO = "audio";
    private static final String PATH_DOCUMENT = "document";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static:${cloud.aws.region:ap-northeast-2}}")
    private String region;

    public String uploadFile(final String tableName, final Long objectSeq, final MultipartFile multipartFile) {
        try {
            final String contentType = multipartFile.getContentType();
            final String originalFilename = multipartFile.getOriginalFilename();
            final String key = createFilePath(contentType, tableName, objectSeq, originalFilename);

            final PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(req, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));
            final String url = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(key)).toExternalForm();
            return URLDecoder.decode(url, StandardCharsets.UTF_8);
        } catch (FileHandlingException e) {
            throw e;
        } catch (Exception e) {
            log.error("S3 업로드 실패", e);
            throw new FileHandlingException("s3 파일 업로드에 실패했습니다.");
        }
    }

    public String uploadFile(final String tableName,
                             final Long objectSeq,
                             final InputStream inputStream,
                             final long contentLength,
                             final String contentType,
                             final String originalFilename) {
        try {
            final String key = createFilePath(contentType, tableName, objectSeq, originalFilename);

            final PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(req, RequestBody.fromInputStream(inputStream, contentLength));
            final String url = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(key)).toExternalForm();
            return URLDecoder.decode(url, StandardCharsets.UTF_8);
        } catch (FileHandlingException e) {
            throw e;
        } catch (Exception e) {
            log.error("S3 업로드 실패(InputStream)", e);
            throw new FileHandlingException("s3 파일 업로드에 실패했습니다.");
        }
    }

    public String generatePresignedUrl(final String fileUrl) {
        return generatePresignedUrl(fileUrl, Duration.ofMinutes(1));
    }

    public String generatePresignedUrl(final String fileUrl, final Duration ttl) {
        if (fileUrl == null || fileUrl.isBlank()) return null;
        try {
            final String key = toS3Key(fileUrl);

            final GetObjectRequest get = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            final GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(get)
                    .build();

            final PresignedGetObjectRequest p = s3Presigner.presignGetObject(presign);
            return p.url().toExternalForm();
        } catch (Exception e) {
            log.error("S3 pre-signed URL 생성 실패: {}", fileUrl, e);
            return null;
        }
    }

    public boolean existsByUrl(final String fileUrl) {
        try {
            final String key = toS3Key(fileUrl);
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("S3 존재 여부 확인 실패: {}", fileUrl, e);
            return false;
        }
    }

    public void deleteByUrl(final String fileUrl) {
        try {
            final String key = toS3Key(fileUrl);
            deleteByKey(key);
        } catch (FileHandlingException e) {
            throw e;
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", fileUrl, e);
            throw new FileHandlingException("s3 파일 삭제에 실패했습니다.");
        }
    }

    public void deleteByKey(final String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패(key): {}", key, e);
            throw new FileHandlingException("s3 파일 삭제에 실패했습니다.");
        }
    }

    public String toS3Key(final String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new FileHandlingException("S3 URL 형식이 올바르지 않습니다.");
        }
        final String decoded = URLDecoder.decode(fileUrl, StandardCharsets.UTF_8);
        final String vh = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        final String pathStyle = "https://s3." + region + ".amazonaws.com/" + bucket + "/";

        if (decoded.startsWith(vh)) return decoded.substring(vh.length());
        if (decoded.startsWith(pathStyle)) return decoded.substring(pathStyle.length());

        log.error("S3 URL 형식이 올바르지 않습니다. fileUrl={}", fileUrl);
        throw new FileHandlingException("S3 URL 형식이 올바르지 않습니다.");
    }

    private String createFilePath(final String contentType,
                                  final String tableName,
                                  final Long objectSeq,
                                  final String originalFilename) {
        if (contentType == null || originalFilename == null) {
            throw new FileHandlingException("파일 메타정보가 누락되었습니다.");
        }
        final String lowerCt = contentType.toLowerCase(Locale.ROOT);
        final String folder =
                lowerCt.startsWith("image/") ? PATH_IMAGE :
                        lowerCt.startsWith("audio/") ? PATH_AUDIO :
                                lowerCt.equals("application/pdf") ? PATH_DOCUMENT :
                                        null;

        if (folder == null) {
            log.error("지원하지 않는 Content-Type: {}", contentType);
            throw new FileHandlingException(contentType + "은(는) 지원하지 않는 파일 형식입니다.");
        }

        final String safeName = originalFilename.replaceAll("[\\r\\n]", "");
        return String.format("%s/%s/%d/%s_%s", folder, tableName, objectSeq, UUID.randomUUID(), safeName);
    }
}
