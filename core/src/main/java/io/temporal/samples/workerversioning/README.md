# Worker Versioning

This sample demonstrates how to use Temporal's Worker Versioning feature to safely deploy updates to workflow and activity code. It shows the difference between auto-upgrading and pinned workflows, and how to manage worker deployments with different build IDs.

The sample creates multiple worker versions (1.0, 1.1, and 2.0) within one deployment and demonstrates:
- **Auto-upgrading workflows**: Automatically and controllably migrate to newer worker versions
- **Pinned workflows**: Stay on the original worker version throughout their lifecycle
- **Compatible vs incompatible changes**: How to make safe updates using `Workflow.getVersion`

## Steps to run this sample:

1) Run a [Temporal service](https://github.com/temporalio/samples-java/tree/main/#how-to-use).

2) Start the main application (this will guide you through the sample):
    ```bash
    ./gradlew -q execute -PmainClass=io.temporal.samples.workerversioning.Starter
    ```
3) Follow the prompts to start workers in separate terminals:
   - When prompted, run: `./gradlew -q execute -PmainClass=io.temporal.samples.workerversioning.WorkerV1`
   - When prompted, run: `./gradlew -q execute -PmainClass=io.temporal.samples.workerversioning.WorkerV1_1`
   - When prompted, run: `./gradlew -q execute -PmainClass=io.temporal.samples.workerversioning.WorkerV2`

Follow the prompts in the example to observe auto-upgrading workflows migrating to newer workers
while pinned workflows remain on their original versions.

