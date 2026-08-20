# Decoupled Schema Generators for Multi-Format Output

We decided to decouple Schema output generation from DSL parsing by introducing a `SchemaGenerator<T>` strategy pattern in `org.cristalise.dsl.persistency.outcome.generator`, providing dedicated generators for XML Schema (`XsdSchemaGenerator`), JSON Schema (`JsonSchemaGenerator`), and Angular dynamic forms (`NgForgeConfigGenerator`).

## Status

Accepted

## Context

Previously, `SchemaDelegate` directly generated XML Schema (XSD) strings using `groovy.xml.MarkupBuilder` inside `buildXSD(Struct)`. As modern CRISTAL-iSE clients (e.g. VibeUI, Angular dynamic forms using `@ng-forge/dynamic-forms`) and JSON-based payload validation require JSON Schema and dynamic form configurations, hardcoding XSD generation in `SchemaDelegate` prevented multi-target output from a single DSL Schema definition.

## Decision

1. **Retain AST/IR**: Keep the parsed `Struct` object graph as the single canonical in-memory Intermediate Representation produced by both Groovy closure and Tabular (Excel/CSV) parsers.
2. **Strategy Pattern**: Define a `SchemaGenerator<T>` interface accepting `Struct` and `SchemaContext` (`name`, `version`, `module`).
3. **Core Generators**:
   - `XsdSchemaGenerator`: Produces standard XML Schema XSD strings (preserving 100% backward compatibility for kernel Schema resource items).
   - `JsonSchemaGenerator`: Produces standard JSON Schema (Draft 2020-12) documents mapping structs to objects/arrays, fields to typed properties with constraints, and embedding CRISTAL-specific metadata under `x-cristal-*` vendor properties.
   - `NgForgeConfigGenerator`: Produces `@ng-forge/dynamic-forms` configuration JSON trees with controls, labels, options, validation rules, layout grid classes, and nested group structures.
4. **Fluent API & Export**: Extend `SchemaBuilder` with `toXsd()`, `toJsonSchema()`, `toNgForgeJson()`, `generate(SchemaGenerator<T>)`, and file export capabilities.

## Consequences

- DSL Schemas can be defined once and compiled into multiple target formats for backend persistence, validation, and UI rendering.
- New output formats (e.g., TypeScript definitions, OpenAPI schemas) can be added as new `SchemaGenerator` implementations without modifying parsing or existing generators.
- Kernel `Schema` resource creation remains fully backward-compatible.
