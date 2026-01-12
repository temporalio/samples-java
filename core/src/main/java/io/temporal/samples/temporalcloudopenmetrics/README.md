# Temporal Cloud OpenMetrics → Prometheus → Grafana (Step-by-step)

This demo shows how to **scrape Temporal Cloud OpenMetrics(https://docs.temporal.io/cloud/metrics/openmetrics/)** 
with **Prometheus** and **visualize them in Grafana**.

It uses the Grafana Temporal mixin dashboard template:
https://github.com/grafana/jsonnet-libs/blob/master/temporal-mixin/dashboards/temporal-overview.json

Once imported/provisioned, the dashboard lets you view the key Temporal metrics in a ready-made layout.

Grafana dashboard view :-
![Grafana dashboard 1](docs/images/img1.png)
![Grafana dashboard 2](docs/images/img2.png)

Prometheus -

![Prometheus](docs/images/img3.png)

---

## 1) Create Service Account + API Key (Temporal Cloud)

OpenMetrics auth reference:
https://docs.temporal.io/production-deployment/cloud/metrics/openmetrics/api-reference#authentication

In Temporal Cloud UI:
- **Settings → Service Accounts**
- Create a service account with **Metrics Read-Only** role
- Generate an **API key** ( copy this, it will be needed later)
---


## 2) Create the `secrets/` folder + API key file

From the repo root (same folder as `docker-compose.yml`), run:

```
cd temporalcloudopenmetrics
mkdir -p secrets
echo "put your CLOUD API KEY HERE" > secrets/temporal_cloud_api_key
```
now the folder will look like below and temporal_cloud_api_key will have the above api key that we generated in step 1

```
temporalcloudopenmetrics/
├── docker-compose.yml
├── prometheus/
├── grafana/
├── secrets/
│   └── temporal_cloud_api_key
```

## 3) Configure TemporalConnection.java

Edit `TemporalConnection.java` and set your defaults:

```
public static final String NAMESPACE = env("TEMPORAL_NAMESPACE", "<namespace>.<account-id>");
public static final String ADDRESS   = env("TEMPORAL_ADDRESS", "<namespace>.<account-id>.tmprl.cloud:7233");
public static final String CERT      = env("TEMPORAL_CERT", "/path/to/client.pem");
public static final String KEY       = env("TEMPORAL_KEY",  "/path/to/client.key");
public static final String TASK_QUEUE = env("TASK_QUEUE", "openmetrics-task-queue");
public static final int WORKER_SECONDS = envInt("WORKER_SECONDS", 60);
```

## 4) 4) Update Prometheus scrape config

prometheus/config.yml
Update it to use your namespace
```
    params:
      namespaces: [ '<namespace>.<account-id>' ]
```


## 5) Start Prometheus + Grafana

docker compose up -d
docker compose ps


## 6) View Grafana dashboard

http://localhost:3001/

- Username: admin
- Password: admin

You should see the Temporal Cloud OpenMetrics dashboard.

## 7) Verify metrics in Prometheus

Prometheus: http://localhost:9093/

Go to:
Status → Targets (make sure the scrape target is UP)
Graph tab (search for Temporal metrics and run a query)

## 8) Ran the sample and view the cloud metrics 

- `./gradlew -q execute -PmainClass=io.temporal.samples.temporalcloudopenmetrics.WorkerMain`
- `./gradlew -q execute -PmainClass=io.temporal.samples.temporalcloudopenmetrics.Starter`