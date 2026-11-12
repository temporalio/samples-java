package io.temporal.samples.externalstorage;

import io.temporal.client.WorkflowClientOptions;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.payload.codec.ZlibPayloadCodec;
import io.temporal.payload.storage.ExternalStorage;
import io.temporal.payload.storage.s3driver.S3StorageDriver;
import io.temporal.payload.storage.s3driver.awssdkv2.S3AsyncClientAdapter;
import java.net.URI;
import java.util.Collections;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Every process in this sample must agree on the codec and storage configuration, so each can read
 * what the others wrote.
 */
public final class StorageConfig {

  /** Set to a bucket name to use real S3 instead of the local mock. */
  static final String BUCKET_ENV_VAR = "EXTERNAL_STORAGE_S3_BUCKET";

  static final int MOCK_PORT = 9100;
  static final String MOCK_BUCKET = "temporal-payloads";
  static final String MOCK_ACCESS_KEY = "test";
  static final String MOCK_SECRET_KEY = "test";

  private StorageConfig() {}

  /** Adds the zlib codec and S3 external storage to the given client options. */
  public static WorkflowClientOptions withExternalStorage(WorkflowClientOptions options) {
    String bucket = System.getenv(BUCKET_ENV_VAR);
    if (bucket != null && !bucket.isEmpty()) {
      // Region and credentials come from the usual AWS configuration, such as environment
      // variables or ~/.aws.
      return withExternalStorage(options, S3AsyncClient.create(), bucket);
    }
    return withExternalStorage(options, mockS3Client(MOCK_PORT), MOCK_BUCKET);
  }

  /** A client for the local mock S3 server started by {@link S3Mock}. */
  static S3AsyncClient mockS3Client(int port) {
    return S3AsyncClient.builder()
        .endpointOverride(URI.create("http://127.0.0.1:" + port))
        .region(Region.US_EAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(MOCK_ACCESS_KEY, MOCK_SECRET_KEY)))
        // Virtual-hosted addressing would need temporal-payloads.127.0.0.1 to resolve.
        .forcePathStyle(true)
        .build();
  }

  static WorkflowClientOptions withExternalStorage(
      WorkflowClientOptions options, S3AsyncClient s3Client, String bucket) {
    S3StorageDriver driver =
        S3StorageDriver.newBuilder()
            .setClient(new S3AsyncClientAdapter(s3Client))
            .setBucket(bucket)
            .build();

    return WorkflowClientOptions.newBuilder(options)
        .setDataConverter(
            new CodecDataConverter(
                DefaultDataConverter.newDefaultInstance(),
                Collections.singletonList(new ZlibPayloadCodec())))
        .setExternalStorage(ExternalStorage.newBuilder().setDriver(driver).build())
        .build();
  }
}
