## Custom Change Version Search Attribute Sample

This sample shows how to upsert custom search attribute when adding a version change to your workflow code.
It is a current workaround until feature https://github.com/temporalio/sdk-java/issues/587 is implemented.
Purpose of upserting a custom search attribute when addint new versions is to then be able to use
visibility api to search for running/completed executions which are on a specific version. It is also useful to see
if there are no running executions on specific change version in order to remove certain no longer used versioned change
if/else block from your workflow code, so it no longer has to be maintained.

To run this sample:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.customchangeversion.CustomChangeVersionStarter
```

After running this sample you can go to your Web UI or use Temporal CLI to search for specific CustomChangeVersion, for example:

```
temporal workflow list -q "CustomChangeVersion='add-v3-activity-change-1'"         
```