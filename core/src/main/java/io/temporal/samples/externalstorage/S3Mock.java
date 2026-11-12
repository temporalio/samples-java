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
    S3Proxy proxy = start(StorageConfig.MOCK_PORT);
    System.out.println(
        "Mock S3 listening on http://127.0.0.1:"
            + proxy.getPort()
            + ", bucket "
            + StorageConfig.MOCK_BUCKET);
  }

  /** Starts the server on the given port, or on a free one if the port is 0. */
  static S3Proxy start(int port) throws Exception {
    BlobStore blobStore = BlobStores.create("transient", new Properties());
    blobStore.createContainer(StorageConfig.MOCK_BUCKET);

    S3Proxy proxy =
        S3Proxy.builder()
            .blobStore(blobStore)
            .awsAuthentication(
                AuthenticationType.AWS_V2_OR_V4,
                StorageConfig.MOCK_ACCESS_KEY,
                StorageConfig.MOCK_SECRET_KEY)
            .endpoint(URI.create("http://127.0.0.1:" + port))
            .build();
    proxy.start();
    while (!"STARTED".equals(proxy.getState())) {
      Thread.sleep(10);
    }
    return proxy;
  }
}
