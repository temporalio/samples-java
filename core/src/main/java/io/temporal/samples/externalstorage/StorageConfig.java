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

  static final int S3_PORT = 5000;
  static final String S3_BUCKET = "temporal-payloads";
  static final String S3_ACCESS_KEY = "test";
  static final String S3_SECRET_KEY = "test";

  private StorageConfig() {}

  /** Adds the zlib codec and S3 external storage to the given client options. */
  public static WorkflowClientOptions withExternalStorage(WorkflowClientOptions options) {
    return withExternalStorage(options, S3_PORT);
  }

  static WorkflowClientOptions withExternalStorage(WorkflowClientOptions options, int s3Port) {
    S3AsyncClient s3Client =
        S3AsyncClient.builder()
            .endpointOverride(URI.create("http://127.0.0.1:" + s3Port))
            .region(Region.US_EAST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(S3_ACCESS_KEY, S3_SECRET_KEY)))
            // Virtual-hosted addressing would need temporal-payloads.127.0.0.1 to resolve.
            .forcePathStyle(true)
            .build();

    S3StorageDriver driver =
        S3StorageDriver.newBuilder()
            .setClient(new S3AsyncClientAdapter(s3Client))
            .setBucket(S3_BUCKET)
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
