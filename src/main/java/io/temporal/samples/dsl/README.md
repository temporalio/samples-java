# DSL Sample

This sample shows how to use a DSL on top of Temporal.

The sample uses CNCF Serverless Workflow (serverlessworkflow.io) DSL and its Java SDK,
which helps us parse the DSL into an object model as well as provides DSL validation.

Since this is just a sample, this sample provides only partial implementation of the 
entire Serverless Workflow DSL features.

The sample runs four DSL workflows, `customerapplication/workflow.yml`,
`bankingtransactions/workflow.yml`, `customerapproval/applicantworkflow.yml`,
`customerapproval/approvalworkflow.yml`.

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

You should see results:

```text
Validating workflow: customerapplication
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
Validating workflow: bankingtransactions
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
Validating workflow: applicantworkflow
Validating workflow: approvalworkflow
Starting workflow with id: approvalworkflow and version: 1.0
Starting workflow with id: applicantworkflow and version: 1.0
Workflow results: 
{
  "customer" : {
    "name" : "John",
    "age" : 22
  },
  "results" : [ {
    "type" : "decision",
    "result" : "APPROVED"
  } ]
}
```



