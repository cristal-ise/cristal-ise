# Kernel Module Guidelines

## Purpose
The core Java library of CRISTAL, providing client and server APIs. It manages business objects called Items, entirely configured from descriptions.

## Key Concepts
- **Items**: Fundamental business objects in CRISTAL.
- **Descriptions**: Data held in Items that configures other Items.
- **Activities & Lifecycles**: Every state change in an Item is driven by an activity execution.
- **Traceability**: All applications are completely traceable by design.

## Core APIs
- `org.cristalise.kernel.client`: Client-side APIs.
- `org.cristalise.kernel.server`: Server-side logic and storage interfaces.

## Testing
- Unit tests are located in `src/test`.
- Uses Spock for testing.
- For functional tests that require a database, see the `testing` module.

## Dependencies
- Vert.x for messaging and event bus.
- Castor for XML binding (legacy).
- Lombok for boilerplate code.

## Guidelines for Junie
- When modifying core APIs, ensure backward compatibility for descriptions.
- Use `Gateway.init()` to initialize the kernel in standalone clients.
