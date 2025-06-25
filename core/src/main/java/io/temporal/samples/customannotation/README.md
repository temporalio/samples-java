# Custom annotation

The sample demonstrates how to create a custom annotation using an interceptor. In this case the annotation allows specifying an exception of a certain type is benign.

This samples shows a custom annotation on an activity method, but the same approach can be used for workflow methods or Nexus operations.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.customannotation.CustomAnnotation
```
