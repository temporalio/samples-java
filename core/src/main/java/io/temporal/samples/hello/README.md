## Hello samples

Each Hello World sample  demonstrates one feature of the SDK in a single file.

**Note that the single file format is used for sample brevity and is not something we recommend for real applications.**

To run each hello world sample, use one of the following commands:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloActivity
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloActivityRetry
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloActivityExclusiveChoice
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloAsync
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloParallelActivity
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloAsyncActivityCompletion
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloAsyncLambda
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloCancellationScope
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloDetachedCancellationScope
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloChild
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloCron
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloDynamic
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloException
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloLocalActivity
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloPeriodic
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloPolymorphicActivity
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloQuery
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloSaga
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloSchedules
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloSignal
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloSearchAttributes
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloTypedSearchAttributes
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloSideEffect
./gradlew -q execute -PmainClass=io.temporal.samples.hello.HelloUpdate
```
