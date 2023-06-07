# Using Codec to encode / decode failure messages

The sample demonstrates how to set up a simple codec for encoding/decoding failure messages
In this sample we set encodeFailureAttributes = true to our CodecDataConverter meaning we want to 
encode / decode failure messages as well.
All it does is add a "Customer: " prefix to the message. You can expand on this to add any type of 
encoding that you might want to use.

Our workflow does simple customer age check validation and fails if their age is < 21.
In the Starter then we print out that the failure message client received on execution failure
was indeed encoded using our codec.

## Running

1. Start Temporal Server with "default" namespace enabled.
   For example using local Docker:

```bash
git clone https://github.com/temporalio/docker-compose.git
cd  docker-compose
docker-compose up
```

2. Run the following command to start the sample:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.encodefailures.Starter
```
