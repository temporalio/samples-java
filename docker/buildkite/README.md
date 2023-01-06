# Using BuildKite

BuildKite simply runs Docker containers. So it is easy to perform the 
same build locally that BuildKite will do. To handle this, there are 
two different docker-compose files: one for BuildKite and one for local.
The Dockerfile is the same for both. 

## Testing the build locally
To run the build locally, start from the root folder of this repo and run the following command:
```bash
docker-compose -f docker/buildkite/docker-compose.yaml run unit-test
```

Note that BuildKite will run basically the same commands.

## Testing the build in BuildKite
Creating a PR against the main branch will trigger the BuildKite
build. Members of the Temporal team can view the build pipeline here:
https://buildkite.com/temporal/java-samples
