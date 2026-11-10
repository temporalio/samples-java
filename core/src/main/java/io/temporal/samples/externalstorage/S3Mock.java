package io.temporal.samples.externalstorage;

import java.net.URI;
import java.util.Properties;
import org.gaul.s3proxy.BlobStores;
import org.gaul.s3proxy.S3Proxy;
import org.gaul.s3proxy.auth.AuthenticationType;
import org.gaul.s3proxy.blobstore.BlobStore;

/** A local S3 server that keeps objects in memory until it stops. */
public class S3Mock {

  public static void main(String[] args) throws Exception {
    BlobStore blobStore = BlobStores.create("transient", new Properties());
    blobStore.createContainer(StorageConfig.S3_BUCKET);

    S3Proxy proxy =
        S3Proxy.builder()
            .blobStore(blobStore)
            .awsAuthentication(
                AuthenticationType.AWS_V2_OR_V4,
                StorageConfig.S3_ACCESS_KEY,
                StorageConfig.S3_SECRET_KEY)
            .endpoint(URI.create(StorageConfig.S3_ENDPOINT))
            .build();
    proxy.start();
    System.out.println(
        "Mock S3 listening on "
            + StorageConfig.S3_ENDPOINT
            + ", bucket "
            + StorageConfig.S3_BUCKET);
  }
}
