# Custom annotation

The sample demonstrates how to create a custom annotation using an interceptor. In this case the annotation allows specifying a fixed next retry delay for a certain failure type.

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.customannotation.CustomAnnotation
```
