# Using Github Actions

Github action simply runs Docker containers. So it is easy to perform the 
same build locally that Github will do. To handle this, there are 
two different docker-compose files: one for Github and one for local.
The Dockerfile is the same for both. 

## Testing the build locally
To run the build locally, start from the root folder of this repo and run the following command:
```bash
docker-compose -f docker/github/docker-compose.yaml run unit-test
```

Note that Github action will run basically the same commands.

## Testing the build in Github Actions
Creating a PR against the main branch will trigger the Github action.
