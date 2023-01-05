# Custom Payload Converter (Crypto converter)

The sample demonstrates how you can override the default Json Converter to encrypt/decrypt payloads using [jackson-json-crypto](https://github.com/codesqueak/jackson-json-crypto).

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
./gradlew -q execute -PmainClass=io.temporal.samples.payloadconverter.crypto.Starter
```

3. View the history in [Temporal Web UI](http://localhost:8088/)
You will see your workflow inputs fields that were set to be encrypted
in the MyCustomer model class are indeed encrypted, for example:

```json
[
  {
    "name": {
      "salt": "uZnKfjmFzwYsH6ncZBVgvvmAPTw=",
      "iv": "0nK++kg8IgtOJFs+gQ/U0A==",
      "value": "cvXFWXfU8RFKdlWgjrHaog=="
    },
    "age": {
      "salt": "uZnKfjmFzwYsH6ncZBVgvvmAPTw=",
      "iv": "0nK++kg8IgtOJFs+gQ/U0A==",
      "value": "OFA/XDiwep153xZHOECqJA=="
    },
    "approved": {
      "salt": "uZnKfjmFzwYsH6ncZBVgvvmAPTw=",
      "iv": "0nK++kg8IgtOJFs+gQ/U0A==",
      "value": "Tm23RaHHKz2wM56G2Bn6Vw=="
    }
  }
]
```

   
  