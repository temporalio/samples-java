# DSL Sample

This sample shows how to use a DSL on top of Temporal.

The sample uses CNCF Serverless Workflow (serverlessworkflow.io) DSL and its Java SDK,
which helps us parse the DSL into an object model as well as provides DSL validation.

Since this is just a sample, this sample provides only partial implementation of the 
entire Serverless Workflow DSL features.

The sample runs two DSL workflows, namely the `customerapplication/workflow.yml` and
`bankingtransactions/workflow.yml`. Both show different DSL features.

Note that most DSLs, including Serverless Workflow DSL used in this sample represent 
their workflow data as JSON. As such manipulation of this data is done via expression languages
that specilize in manipulating JSON. In this case we use `jq`. You can plug in your expression language
of choice. 

## Run the sample

1. Start the Worker:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.dsl.Worker
```

2. Start the Starter

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.dsl.Starter
```

The started will run first the `customerapplication/workflow.yml` DSL workflow and then the
`bankingtransactions/workflow.yml` DSL workflow. you should see results:


If the age of the customer is set to >= 20 the application will be approved, if set to < 20 it will be rejected.
The results of the workflow will include the updated `applicationStatus`, and an added array which shows 
all the activities that were executed (corresponds to actions in the DSL), for example:

```text
Starting workflow with id: customerapplication and version: 1.0
Query result for customer name: John
Query result for customer age: 22
Workflow results: 
{
  "customer" : {
    "name" : "John",
    "age" : 22
  },
  "results" : [ {
    "type" : "CheckCustomerInfo",
    "result" : "invoked"
  }, {
    "type" : "UpdateApplicationInfo",
    "result" : "invoked"
  }, {
    "type" : "decision",
    "result" : "APPROVED"
  } ]
}
Starting workflow with id: bankingtransactions and version: 1.0
Query result for customer name: John
Query result for customer age: 22
Workflow results: 
{
  "customer" : {
    "name" : "John",
    "age" : 22,
    "transactions" : [ 100, -50, 20 ]
  },
  "results" : [ {
    "type" : "InvokeBankingService",
    "result" : "invoked"
  }, {
    "type" : "InvokeBankingService",
    "result" : "invoked"
  }, {
    "type" : "InvokeBankingService",
    "result" : "invoked"
  } ]
}
```



