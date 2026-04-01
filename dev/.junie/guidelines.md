# Dev Module Guidelines

## Purpose
Development module to enable CRUD management of Items and bootstrap the development environment.

## Technology Stack
- **Groovy**: Used for scripting and bootstrapping.
- **Docker**: For running PostgreSQL (e.g., v16).

## Key Features
- CRUD operations for Items during development.
- Eclipse launch configurations for Groovy scripts.
- Docker setup for Postgres.

## Guidelines for Junie
- Use `Module.groovy` for development environment setup.
- Follow Docker setup instructions in `README.md` for local testing.
- New bootstrap scripts should be placed in `src/main/module`.
