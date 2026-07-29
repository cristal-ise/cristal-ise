# Context Map

## Contexts

- [kernel](./kernel/CONTEXT.md) — Core domain model implementing the Description-Driven Framework with Items, Agents, Activities, and Workflows
- [jooqdb](./jooqdb/CONTEXT.md) — jOOQ-based persistence implementation
- [in-memory-lookup](./in-memory-lookup/CONTEXT.md) — In-memory persistence implementation for functional testing
- [xpath-outcome-initiator](./xpath-outcome-initiator/CONTEXT.md) — XPath-based outcome processing and initiation
- [gui](./gui/CONTEXT.md) — Graphical user interface components
- [dsl](./dsl/CONTEXT.md) — Groovy DSL wrapper for kernel concepts
- [restapi](./restapi/CONTEXT.md) — REST API endpoints and kernel API wrappers
- [trigger](./trigger/CONTEXT.md) — Quartz job scheduling integration with StateMachine extensions
- [dev](./dev/CONTEXT.md) — CRUD Factory and scaffold generation for Item development
- [testing](./testing/CONTEXT.md) — Integrated functional tests

## Relationships

- **kernel** is the foundational context that all other contexts depend on
- **jooqdb → kernel**: Persistence implementation using jOOQ
- **in-memory-lookup → kernel**: Alternative in-memory persistence
- **xpath-outcome-initiator → kernel**: XPath processing for outcomes
- **gui → kernel**: Direct UI interaction with core domain
- **dsl → kernel**: Textual DSL wrapper for all kernel concepts
- **restapi → kernel**: REST endpoints exposing kernel API
- **trigger → kernel**: Quartz integration for scheduled Activity execution
- **dev → kernel, dsl**: Development utilities using both core and DSL
- **testing → all contexts**: Comprehensive functional tests integrating all modules
