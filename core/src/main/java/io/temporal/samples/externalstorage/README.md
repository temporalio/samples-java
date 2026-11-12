# External Storage

> **Note:** External storage is experimental and may change in the future.

This sample offloads large payloads to S3-compatible object storage with the SDK's external storage
support and the `temporal-payload-storage-s3driver-awssdkv2` module. The SDK's `ZlibPayloadCodec`
compresses every payload first, so what is stored inline in Temporal and in S3 is compressed.

**Scenario:** The workflow receives an order count and calls a `fetchOrders` activity that returns
that many orders. The list, about 400 KiB even after compression, is passed to a second
`summarizeOrders` activity, and the workflow returns its small `BatchSummary`.

The codec compresses each payload, and the SDK then compares the compressed size against the default
256 KiB threshold. Payloads still above it are stored in S3 and replaced in history by a small
reference. The workflow's own input and result are a few bytes and stay inline.

A mock S3 server ([S3Mock.java](S3Mock.java)), built on [S3Proxy](https://github.com/gaul/s3proxy),
is included, so the sample runs locally without an AWS account or Docker. To use real S3 instead,
see [Using real S3](#using-real-s3).

## Running this sample

1. `temporal server start-dev` to start the Temporal server.
2. In another terminal, start the mock S3 server. It listens on `127.0.0.1:9100`, serves the
   `temporal-payloads` bucket, and keeps objects in memory until it stops.

   ```bash
   ./gradlew -q :core:execute -PmainClass=io.temporal.samples.externalstorage.S3Mock
   ```

3. In another terminal, start the worker:

   ```bash
   ./gradlew -q :core:execute -PmainClass=io.temporal.samples.externalstorage.ExternalStorageWorker
   ```

4. In another terminal, run the workflow:

   ```bash
   ./gradlew -q :core:execute -PmainClass=io.temporal.samples.externalstorage.Starter
   ```

   Example output:

   ```text
   Started workflow, run ID: <run-id>
   Processed 200 orders weighing 5000.0 kg in total
   ```

5. Run `temporal workflow show` to see how the payloads were stored:

   ```bash
   temporal workflow show --workflow-id external-storage-workflow-id
   ```

   | Payload                  | Type                     | Storage           |
   | ------------------------ | ------------------------ | ----------------- |
   | Workflow input           | `int`                    | Inline            |
   | `fetchOrders` input      | `int`                    | Inline            |
   | `fetchOrders` result     | `List<Order>` (~400 KiB) | **External (S3)** |
   | `summarizeOrders` input  | `List<Order>` (~400 KiB) | **External (S3)** |
   | `summarizeOrders` result | `BatchSummary`           | Inline            |
   | Workflow result          | `BatchSummary`           | Inline            |

   The two offloaded payloads are the order list passed from one activity to the next, which is
   what external storage is for: large data that flows between activities without bloating
   workflow history. Both references name the same S3 object: the driver keys objects by a hash of
   their content, so when the workflow passes the list on, it finds the object already stored and
   skips the upload.

## How it works

[StorageConfig.java](StorageConfig.java) adds the codec and external storage to the
`WorkflowClientOptions` that both the worker and the starter use. The worker uses its client's
configuration, so it needs no other setup.

```java
S3StorageDriver driver =
    S3StorageDriver.newBuilder()
        .setClient(new S3AsyncClientAdapter(s3Client))
        .setBucket(bucket)
        .build();

WorkflowClientOptions.newBuilder(options)
    .setDataConverter(
        new CodecDataConverter(
            DefaultDataConverter.newDefaultInstance(),
            Collections.singletonList(new ZlibPayloadCodec())))
    .setExternalStorage(ExternalStorage.newBuilder().setDriver(driver).build())
    .build();
```

When encoding a payload, the SDK:

1. Serializes the value to a `Payload`.
2. Runs the codec, which compresses the payload.
3. Compares the compressed size against the threshold (default 256 KiB, set with
   `ExternalStorage.Builder.setPayloadSizeThreshold`).
4. If it is at or above the threshold, stores the compressed payload in S3 and replaces it with a
   reference.

Decoding reverses these steps, retrieving from S3 and then decompressing.

Every process that reads or writes this workflow's payloads must use the same codec and external
storage configuration, so each can read what the others wrote.

### Using real S3

Set `EXTERNAL_STORAGE_S3_BUCKET` to your bucket's name in the worker's and the starter's terminals,
and skip the mock S3 server:

```bash
export EXTERNAL_STORAGE_S3_BUCKET=<your-bucket>
```

The S3 client then takes its region and credentials from the usual AWS configuration, such as the
`AWS_REGION` and `AWS_PROFILE` environment variables or `~/.aws`. Set the variable in both
terminals: a worker and a starter that disagree would write payloads where the other can't read
them.

### Why a codec

External storage works without a codec. Remove `ZlibPayloadCodec` and the order list is still
offloaded, only uncompressed. The sample uses one because many applications already run a codec,
and the two interact:

- The threshold applies to the encoded payload. Compression alone can keep a payload small enough to
  stay inline, so the threshold is a limit on the compressed size, not the size of your data.
- The driver stores the encoded payload. With an encryption codec in place of `ZlibPayloadCodec`,
  the objects in S3 are encrypted too.
- The reference that replaces an offloaded payload in history is not passed through the codec, so
  the bucket and key it names are visible to anyone who can read the history.
