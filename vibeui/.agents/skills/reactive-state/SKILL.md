---
name: reactive-state
description: Guidelines for managing state using Angular Signals, computed signals, effects, services, dependency injection, and modern reactive patterns.
---

# Skill: Reactive State Management

Guidelines for managing state using Angular Signals and modern reactive patterns.

## Signals
- Use `signal()` for local component state.
- Use `computed()` for derived state and memoization.
- Use `effect()` sparingly for side effects.

## State Mutations
- Use `.set()` to replace signal values.
- Use `.update()` to compute new values based on current ones.
- **NEVER** use `.mutate()` (deprecated/removed in newer versions).

## Services
- Use `providedIn: 'root'` for singleton services.
- Use the `inject()` function for dependency injection.
- Reference: [Angular Signals](https://angular.dev/essentials/signals)

## Backend Services
- Services interacting with the backend must base their REST API calls on the definitions described by the OpenAPI specification files in the `openapi` directory.
- Use `npm run generate:api` to regenerate/update the API client code in the `src/app/api` directory when definitions in the `openapi` directory change.

## Best Practices
- Keep state transformations pure and predictable.
- Use the `async` pipe when working with Observables.
- Design services around a single responsibility.
