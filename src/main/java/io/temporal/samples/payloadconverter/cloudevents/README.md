# Custom Payload Converter (CloudEvents)

The sample demonstrates creating and setting a custom Payload Converter.

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
./gradlew -q execute -PmainClass=io.temporal.samples.payloadconverter.cloudevents.Starter
```
