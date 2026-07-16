# CRISTAL-iSE Project Guidelines

## Agent skills

### Issue tracker

Issues are tracked on GitHub via the `gh` CLI. External PRs are not a request surface. See `docs/agents/issue-tracker.md`.

### Triage labels

Using default triage labels (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Multi-context layout using `CONTEXT-MAP.md` at the root. See `docs/agents/domain.md`.

## Project Overview
CRISTAL-iSE is a description-driven software platform for No-Code/Low-Code application development. It manages business objects called Items, which are configured via descriptions.

## Technology Stack
- **Language**: Java 21, Groovy 4.
- **Build System**: Maven.
- **Frameworks**: Vert.x, Lombok.
- **Testing**: Spock (Groovy-based), JUnit 5.
- **Database Access**: jOOQ.
- **UI**: Swing (Legacy GUI), Angular (vibeui).
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
- `vibeui`: Angular-based modern UI.
- `dev`: Development and bootstrap utilities.

## General Guidelines
- **Java Engineering**: Apply modern Java standards (Java 21+) and follow the `java-engineer` skill rules. For complex Java tasks, delegate to the `@java-engineer` subagent.
- **Coding Style**: Use Lombok for boilerplate. Follow existing Java/Groovy style.
- **Logging**: Use SLF4J with Logback.
- **Testing**: Prefer Spock for unit and integration tests. Ensure new features have tests in `testing` module if they require a full environment.
- **Build**: Use `mvn clean install` from the root to build all Maven modules.
- **Dependencies**: Manage dependencies in the root `pom.xml` under `<dependencyManagement>`.

## Development Environment
- Use Docker for local services (e.g., PostgreSQL 16).
- Use this URL: `jdbc:postgresql://localhost:5432/integtest`

## Database Schema (PostgreSQL)
The project uses a PostgreSQL database with the following key tables:
- `ITEM`: Stores essential information about the Item, i.e. UUID, whether it is an Agent or Item.
- `ITEM_PROPERTY`: Stores key-value metadata of Items, used for identification and typing. Mandatory properties are `Module`, `Name` and `Type`.
- `LIFECYCLE`: Stores lifecycle instances as XML (UUID, name).
- `COLLECTION`: Manages logical groupings and relations between Items.
- `DOMAIN_PATH`: DomainPath represents the user, a.k.a domain, defined structured (path) name of Items.
- `EVENT`: Provides a full audit trail and history log of all system state changes.
- `JOB`: A Job represents a possible transition by a particular Agent of an Activity of the Workflow of an Item.
- `OUTCOME`: Stores structured data (XML) produced during business processes.
- `ATTACHMENT`: Stores binary data associated with OUTCOME.
- `VIEWPOINT`: Stores the named version of an Outcome.
- `ROLE_PATH`: Stores roles for role-based access control.
- `ROLE_PERMISSION`: Store instance-level access control triplets (instance:type:activity) of Apache Shiro granted to a Role.
