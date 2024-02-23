# CRISTAL-iSE functional testing project

## Run tests in your IDE with docker-compose
1. build local docker image of testing project
   - `mvn -pl testing jib:dockerBuild`
1. create src/main/docker/.env file with content similat to this:
   - `APP_VOLUMES_ROOT=~/workspace/cristal-ise/testing`
1. use src/main/docker/docker-compose.yml to start the required services
   - `docker compose -p integtest up -d`
1. select the junit test class to run and add these java system properties to its run config:
   ```properties
   -Dvertx.hazelcast.config=src/main/config/cluster-config.xml
   -Dlogback.configurationFile=src/main/config/logback.xml
   ```
   - in case of intellij you can create a junit template for the testing project
1. execute the test

