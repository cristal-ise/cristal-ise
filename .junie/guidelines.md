# CRISTAL-iSE Project Guidelines

## Project Overview
CRISTAL-iSE is a description-driven software platform for No-Code/Low-Code application development. It manages business objects called Items, which are configured via descriptions.

## Technology Stack
- **Language**: Java 21, Groovy 4.
- **Build System**: Maven.
- **Frameworks**: Vert.x, Lombok.
- **Testing**: Spock (Groovy-based), JUnit 5.
- **Database Access**: jOOQ.
- **UI**: Swing (Legacy GUI), Angular (WebUI).
- **Messaging/Communication**: Vert.x Event Bus.

## Project Structure
- `kernel`: Core library, APIs for client and server.
- `jooqdb`: jOOQ-based storage implementation.
- `in-memory-lookup`: In-memory storage for testing.
- `xpath-outcome-initiator`: XML/XPath based outcome generation.
- `restapi`: JAX-RS wrapper for CRISTAL Client API.
- `dsl`: Domain Specific Language for CRISTAL objects.
- `trigger`: Quartz-based scheduling.
- `gui`: Swing-based administrative UI.
- `testing`: Integration testing project.
- `webui`: Angular-based modern UI (NX workspace).
- `dev`: Development and bootstrap utilities.

## General Guidelines for Junie
- **Coding Style**: Use Lombok for boilerplate. Follow existing Java/Groovy style.
- **Logging**: Use SLF4J with Logback.
- **Testing**: Prefer Spock for unit and integration tests. Ensure new features have tests in `testing` module if they require a full environment.
- **Build**: Use `mvn clean install` from the root to build all Maven modules.
- **Dependencies**: Manage dependencies in the root `pom.xml` under `<dependencyManagement>`.

## Development Environment
- Use Docker for local services (e.g., PostgreSQL 16).
- Standard PostgreSQL port: 5432.
- Default credentials: `postgres`/`cristal`.
