# jOOQDB Module Guidelines

## Purpose
Implementation of CRISTAL-iSE ClusterStorage and Lookup interfaces based on jOOQ. This module provides persistence for CRISTAL Items in relational databases.

## Technology Stack
- **jOOQ**: Used for generating SQL and querying the database.
- **PostgreSQL**: Primarily used for production and functional tests.
- **H2**: Used for some local tests (check configuration).

## Key Components
- Implementation of `ClusterStorage`.
- Implementation of `Lookup` interface.

## Build and Testing
- `mvn clean install` to build the module.
- Functional tests are located in the `testing` module.

## Guidelines for Junie
- Use jOOQ DSL for all database queries.
- Ensure SQL is compatible across different database dialects (PostgreSQL, MySQL, H2).
- When schema changes are required, look for jOOQ code generation settings.
- Check the `deploy/` folder for SQL scripts or Docker compose files.
