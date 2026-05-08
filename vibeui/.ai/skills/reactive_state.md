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

## Best Practices
- Keep state transformations pure and predictable.
- Use the `async` pipe when working with Observables.
- Design services around a single responsibility.
