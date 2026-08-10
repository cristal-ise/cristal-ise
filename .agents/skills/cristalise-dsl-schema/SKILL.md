---
name: cristalise-dsl-schema
description: Complete reference for defining Schema Items in CRISTAL-iSE DSL. Use when developing or modifying Schema definitions for Outcomes, or when another cristalise-dsl skill references schema.
---

# CRISTAL-iSE Schema DSL

Use `schema-dsl` when defining or modifying CRISTAL-iSE Outcomes. This skill ensures the generated XSD is valid and the WebUI renders correctly.

## MUST DO

- **Always wrap fields in a `struct`**. Do not define `field` or `attribute` at the top level of a `Schema`.
- **Set `useSequence: true`** on `struct` whenever the order of elements matters (e.g., for logical UI flow).
- **Define unique `name` and `version`** for every `Schema`.
- **Prefer `field` over `attribute`** for new definitions unless explicitly targeting XML attributes.
- **Use `listOfValues`** for fields requiring dynamic selection (Queries/Scripts).
- **Apply `warning`** for business logic validation (regex `pattern` or JS `expression`).
- **Use `expression`** for computed fields; mark these as `disabled: true` in `dynamicForms`.

## MUST NOT DO

- **Do not hardcode labels** in `documentation`; use `dynamicForms(label: '...')` for UI display.
- **Do not use complex Groovy logic** inside `warning` if it can be handled by a server-side script.
- **Do not skip `documentation`** for major structural elements.

## Workflow: The `schema-dsl` Cycle

1. **Model the outcome**: Define the `struct` hierarchy based on the business requirements.
2. **Apply types**: Assign appropriate `type` and `multiplicity` to each `field`.
3. **Add UI Hints**: Use `dynamicForms` to create a `tight` layout (group related fields, set grid widths).
4. **Implement Logic**: Add `expression` for derivations and `warning` for constraints.
5. **Verify**: Check that the generated XML structure matches the business `outcome` requirements.

## Reference Guide

| Topic | Description | File |
|---|---|---|
| **Structural Elements** | Details for `struct`, `attribute`, `field`, and `reference`. | `references/structural-elements.md` |
| **Expressions** | Computed fields, injected variables, and update scripts. | `references/expressions.md` |
| **UI Customization** | `dynamicForms` layout, widgets, and `listOfValues`. | `references/ui-customization.md` |
| **Data Types** | Supported types and multiplicity formats. | `references/data-types.md` |
| **Examples** | Reference implementations for common patterns. | `EXAMPLES.md` |

## Setup & Verification

1. **Verify availability** of `org.cristalise.dsl.persistency.outcome.SchemaBuilder`.
2. **Test generation** using `org.cristalise.dsl.test.builders.SchemaTestBuilder`.
3. **Completion Criterion**: The generated `sb.schema.schemaData` contains all defined elements with correct XSD types and multiplicities.

## See Also

- [writing-cristalise-dsl](../writing-cristalise-dsl/SKILL.md) — Router for all DSL skills
- [cristalise-dsl-item](../cristalise-dsl-item/SKILL.md) — Items and Agents that use Schemas
- [cristalise-dsl-module](../cristalise-dsl-module/SKILL.md) — Modules define Roles
- [kernel/CONTEXT.md](../../../kernel/CONTEXT.md) — Domain definitions of Leading Words
