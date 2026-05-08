# Skill: Component Architecture

Guidelines for building modern, standalone Angular components.

## Core Principles
- **Standalone Components**: Always use standalone components. Do NOT use `NgModules`.
- **Decorator Settings**: Do NOT set `standalone: true` in `@Component` (handled by the framework version).
- **Change Detection**: Always set `changeDetection: ChangeDetectionStrategy.OnPush`.
- **Control Flow**: Use native control flow (`@if`, `@for`, `@switch`) instead of structural directives (`*ngIf`, etc.).

## Inputs and Outputs
- Use `input()` and `input.required()` for component inputs.
- Use `output()` for component outputs.
- Reference: [Angular Inputs](https://angular.dev/guide/components/inputs), [Angular Outputs](https://angular.dev/guide/components/outputs)

## Templates and Styles
- Prefer inline templates for small components.
- Use relative paths for external templates and styles.
- Avoid `ngClass` and `ngStyle`; use `class` and `style` bindings instead.
- Use `NgOptimizedImage` for static images (except base64).
