---
name: writing-cristalise-dsl
description: Reference for writing CRISTAL-iSE DSL files. Use when developing CRISTAL-iSE applications and need to write workflows, schemas, modules, items, scripts, or queries using the Groovy DSL.
---

A **CRISTAL-iSE DSL file** defines the structure and behavior of CRISTAL-iSE applications using the Groovy-based Domain Specific Language. This skill is the **router** that directs you to the appropriate cristalise-dsl-* skill based on what you're writing.

**Bold terms** are defined in the individual DSL skill files or in their referenced glossaries.

## Purpose

This router skill cures the **cognitive load** of remembering which cristalise-dsl-* skill to use for each type of DSL file. It provides a single entry point for all DSL writing tasks in CRISTAL-iSE application development.

## DSL Skill Catalog

| Skill | Domain | Use When | Status |
|-------|--------|----------|--------|
| **[cristalise-dsl-workflow](#cristalise-dsl-workflow)** | Workflow definitions | Creating or modifying Item lifecycle workflows with Activities, Splits, Joins | **Complete** |
| **[cristalise-dsl-schema](#cristalise-dsl-schema)** | Schema/Outcome definitions | Developing or modifying Outcome structures, structs, fields, UI hints | **Complete** |
| **[cristalise-dsl-module](#cristalise-dsl-module)** | Module definitions | Defining Item containers, dependencies, and module-level constructs | **Placeholder** |
| **[cristalise-dsl-item](#cristalise-dsl-item)** | Item definitions | Creating and configuring individual Items and their properties | **Placeholder** |
| **[cristalise-dsl-script](#cristalise-dsl-script)** | Script definitions | Writing Groovy/JavaScript scripts for routing, validation, computations | **Complete** |
| **[cristalise-dsl-query](#cristalise-dsl-query)** | Query definitions | Defining queries for listOfValues, dynamic data fetching | **Placeholder** |

## Individual Skill Reference

### cristalise-dsl-module

**Status**: Placeholder — Content TBD

Use when defining **Module** constructs — containers for organizing Items, managing dependencies, and establishing module-level configurations. This skill will provide:

- Module definition syntax and structure
- Dependency management between modules
- Module-level properties and configurations
- Best practices for module organization

**Location**: [`../cristalise-dsl-module/SKILL.md](../cristalise-dsl-module/SKILL.md)

**Leading Words**: Module, dependency, container, configuration

---

### cristalise-dsl-item

**Status**: Placeholder — Content TBD

Use when creating and configuring individual **Items** — the business objects at the heart of CRISTAL-iSE. This skill will provide:

- Item definition syntax
- Property configuration (Module, Name, Type)
- Item lifecycle integration
- Relationship definitions between Items

**Location**: [`../cristalise-dsl-item/SKILL.md](../cristalise-dsl-item/SKILL.md)

**Leading Words**: Item, property, Type, Module, Name, relationship

---

### cristalise-dsl-workflow

**Status**: Complete

Use when creating or modifying **Workflow** definitions — the core construct for defining Item lifecycles as dependency graphs of **Activities**. This skill provides:

- Quick start examples for minimal and complex workflows
- Complete reference for all workflow **patterns** (AndSplit, OrSplit, XOrSplit, Loop)
- Core concepts: Workflow, Layout, Activity, Binding, Block, Split, Join, Alias, Routing
- Key rules and anti-patterns
- Error handling guide
- 20+ complete, runnable examples
- 40+ defined terms in glossary

**Location**: [`../cristalise-dsl-workflow/SKILL.md](../cristalise-dsl-workflow/SKILL.md)

**Leading Words**: Workflow, Layout, Activity, Binding, Split, Join, Block, Alias, Routing

---

### cristalise-dsl-schema

**Status**: Complete

Use when developing or modifying **Schema** definitions for CRISTAL-iSE **Outcomes**. This skill ensures the generated XSD is valid and the WebUI renders correctly. This skill provides:

- MUST DO/MUST NOT DO rules for schema definitions
- Workflow for the schema-dsl cycle
- Reference guide for structural elements, expressions, UI customization, data types
- Setup and verification steps

**Location**: [`../cristalise-dsl-schema/SKILL.md](../cristalise-dsl-schema/SKILL.md)

**Leading Words**: Schema, struct, field, attribute, expression, dynamicForms, listOfValues, warning, multiplicity

---

### cristalise-dsl-script

**Status**: Complete

Use when writing **Script** definitions for routing logic, validation rules, computations, and other dynamic behaviour. This skill provides:

- Complete Script resource DSL syntax
- File-based script approach with external references
- Input/output parameter contracts
- Binding variables (item, agent, job)
- Script inclusion and reuse patterns
- Error handling with ErrorInfo
- Standard script patterns (Aggregate, QueryList)
- MVEL2 template integration

**Location**: [`../cristalise-dsl-script/SKILL.md](../cristalise-dsl-script/SKILL.md)

**Leading Words**: Script, Groovy, routing, expression, binding, variable, input, output, parameter

---

### cristalise-dsl-query

**Status**: Placeholder — Content TBD

Use when defining **Query** constructs for dynamic data fetching, listOfValues population, and runtime data access. This skill will provide:

- Query definition syntax
- listOfValues configuration
- Query parameter binding
- Performance considerations
- Common query patterns

**Location**: [`../cristalise-dsl-query/SKILL.md](../cristalise-dsl-query/SKILL.md)

**Leading Words**: Query, listOfValues, parameter, data, fetch, dynamic

## How to Use This Router

When writing CRISTAL-iSE DSL files, use this table to find the right skill:

### Quick Decision Guide

| What You're Writing | Use This Skill | Leading Word |
|---------------------|----------------|--------------|
| Item lifecycle with Activities, Splits, Joins | cristalise-dsl-workflow | Workflow |
| Data structure for business Outcomes | cristalise-dsl-schema | Schema |
| Container for organizing Items | cristalise-dsl-module | Module |
| Individual business objects | cristalise-dsl-item | Item |
| Routing logic, validation, computations | cristalise-dsl-script | Script |
| Dynamic data sources, listOfValues | cristalise-dsl-query | Query |

### Invocation Strategy

- This router skill is **model-invoked** — the agent can reach it autonomously when you mention DSL writing tasks
- All cristalise-dsl-* skills are also **model-invoked** — the agent can reach them directly when their leading words appear
- This design enables both direct access and router-mediated discovery

### Shared Principles Across DSL Skills

All cristalise-dsl-* skills follow consistent patterns:

- **Model-invoked**: Can be reached autonomously when leading words appear
- **Progressive disclosure**: Core concepts inline, details behind pointers
- **Leading words**: Each skill identifies its anchoring vocabulary
- **Predictable structure**: Quick Start, Core Concepts, Reference Guide, Best Practices, Anti-Patterns, Error Handling

## When to Use This Skill

- Starting to write any CRISTAL-iSE DSL file and need to find the right reference
- Unsure which cristalise-dsl-* skill covers your specific DSL writing task
- Exploring the DSL documentation catalog for CRISTAL-iSE development
- When the agent mentions writing workflows, schemas, modules, items, scripts, or queries

## When to Use Individual Skills

| Task | Use Skill |
|------|-----------|
| Define Item lifecycle with Activities, Splits, Joins | cristalise-dsl-workflow |
| Create data structure for business Outcomes | cristalise-dsl-schema |
| Organize Items into logical containers with dependencies | cristalise-dsl-module |
| Configure individual business objects and their properties | cristalise-dsl-item |
| Write Groovy/JavaScript for routing, validation, or computations | cristalise-dsl-script |
| Define dynamic data sources for listOfValues or runtime access | cristalise-dsl-query |

## See Also

- [writing-great-skills](../writing-great-skills/SKILL.md) — The blueprint for skill writing principles
- [cristalise-dsl-workflow](../cristalise-dsl-workflow/SKILL.md) — Complete workflow DSL reference
- [cristalise-dsl-schema](../cristalise-dsl-schema/SKILL.md) — Complete schema DSL reference
- [cristalise-dsl-module](../cristalise-dsl-module/SKILL.md) — Module DSL reference (placeholder)
- [cristalise-dsl-item](../cristalise-dsl-item/SKILL.md) — Item DSL reference (placeholder)
- [cristalise-dsl-script](../cristalise-dsl-script/SKILL.md) — Complete Script DSL reference
- [cristalise-dsl-query](../cristalise-dsl-query/SKILL.md) — Query DSL reference (placeholder)
