# Demo List Workflows

The sample demonstrates:
1) Setting custom search attributes for a workflow
2) Using ListWorkflowExecutionsRequest and use custom search attribute query to list
workflow executions that match that query

## Running

1. Unlike the other examples, this one has to be started with Elasticsearch 
capabilities enabled. If you are using docker you can do that with:

```bash
git clone https://github.com/temporalio/docker-compose.git
cd  docker-compose
docker-compose -f docker-compose-cas-es.yml up
```

2. 
Run the following command to start the sample:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.listworkflows.Starter
```
