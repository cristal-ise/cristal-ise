# Testing Module Guidelines

## Purpose
CRISTAL-iSE functional testing project. Contains integration and functional tests for the entire system.

## Technology Stack
- **Docker/Docker Compose**: Used to start full test environments (PostgreSQL, Vert.x instances).
- **Spock**: Testing framework.
- **jib-maven-plugin**: Used to build Docker images of the testing project.

## How to Run
- Build the local image: `mvn -pl testing jib:dockerBuild`.
- Setup the environment: `docker compose -p integtest up -d`.
- Run specific tests in IDE or via Maven: `mvn test -pl testing`.

## Configuration
- System properties required for testing:
  - `-Dvertx.hazelcast.config=src/main/config/cluster-config.xml`
  - `-Dlogback.configurationFile=src/main/config/logback.xml`

## Guidelines for Junie
- Add new functional tests to this module.
- Ensure the Docker environment is properly configured for testing new features.
- Follow the existing pattern for database initialization and cleanup in tests.
