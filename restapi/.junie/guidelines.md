# REST API Module Guidelines

## Purpose
A JAX-RS (Jersey) wrapper for the CRISTAL Client API. Provides HTTP endpoints for managing Items, performing searches, and executing workflows.

## Technology Stack
- **JAX-RS (Jersey)**: RESTful web services.
- **JSON**: Primary data exchange format.
- **XML**: Supported via `Accept: text/xml` header.

## Key Endpoints
- `/login`: To get a login cookie.
- `/domain`: Browsing and searching the domain tree.
- `/item/{uuid}`: Managing specific Items.
- `/item/{uuid}/workflow`: Workflow and activity execution.

## Configuration
- `REST.requireLoginCookie` (default: true): Security setting for API access.
- `REST.URI` (default: http://localhost:8081/): Default Jersey server URI.

## Testing
- Use `curl` for quick testing (see `README.md` for examples).
- For unit/integration tests, check `src/test`.

## Guidelines for Junie
- All REST endpoints should handle both JSON and XML where appropriate.
- Ensure proper error handling and return codes (401 for unauthorized, 404 for missing items, etc.).
- When adding new endpoints, follow existing URL structure.
