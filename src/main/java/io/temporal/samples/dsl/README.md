# DSL Sample

This sample demonstrates of workflow execution where execution semantics are 
provided by a DSL.

This sample uses the CNCF Serverless Workflow (serverlessworkflow.io) specification DSL and its Java SDL
which helps us parse the DSL into an object model as well as provides validation.

Since this is just a sample, it only provides ability to use some parts of the SW DSL.

You can see the JSON and YAML DSL workflows in /src/main/resources/dsl/, namely
`customerapplication.json` and `customerapplication.yml`. The
`datainput.json` file in this directory is the JSON used as input to our workflow. 

You can play with the value of `age` of the customer in `datainput.json` to set it below 20 years of age 
to see how their application will be rejected in that case.

Note that with most DSLs the workflow data is JSON, meaning that in order to manipulate this data 
we need to use some sort of expression languages inside the DSL to define data manipulation expressions.
For this sample we use JsonPath, but other expression languages can be plugged in as well.

## Run the sample

1. Start the Worker:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.dsl.Worker
```

2. Start the Starter

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.dsl.Starter
```

If the age of the customer is set to >= 20 the application will be approved, if set to < 20 it will be rejected.
The results of the workflow will include the updated `applicationStatus`, and an added array which shows 
all the activities that were executed (corresponds to actions in the DSL), for example:

```json
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
```

Is a result of a run where customer age was >= 20. In this case the decision was set to "APPROVED".




