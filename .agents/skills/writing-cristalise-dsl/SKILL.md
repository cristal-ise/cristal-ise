---
name: writing-cristalise-dsl
description: Router skill for writing CRISTAL-iSE DSL files. Use when developing CRISTAL-iSE applications and need to write DomainContext, Property, Workflows, Schemas, Modules, Items, Agents, Roles, Scripts, Queries, or Domains using the Groovy DSL.
---

A **CRISTAL-iSE DSL file** defines the structure and behavior of CRISTAL-iSE applications using the Groovy-based Domain Specific Language. This skill is the **router** that directs you to the appropriate cristalise-dsl-* skill based on what you're writing.

**Bold terms** are defined in the individual DSL skill files or in their referenced glossaries.

## Purpose

This router skill cures the **cognitive load** of remembering which cristalise-dsl-* skill to use for each type of DSL file. It provides a single entry point for all DSL writing tasks in CRISTAL-iSE application development.

## DSL Skill Catalog

@formatter:off
| Skill | Domain | Use When | Status |
|-------|--------|----------|--------|
| **[cristalise-dsl-workflow](#cristalise-dsl-workflow)** | Workflow definitions | Creating or modifying Item lifecycle workflows with Activities, Splits, Joins | **Complete** |
| **[cristalise-dsl-schema](#cristalise-dsl-schema)** | Schema/Outcome definitions | Developing or modifying Outcome structures, structs, fields, UI hints | **Incomplete** |
| **[cristalise-dsl-property](#cristalise-dsl-property)** | Property definitions | Creating or modifying PropertyDescriptionList Items | **Placeholder** |
| **[cristalise-dsl-module](#cristalise-dsl-module)** | Module definitions | Defining Item containers, dependencies, and module-level constructs | **Placeholder** |
| **[cristalise-dsl-item](#cristalise-dsl-item)** | Item definitions | Creating and configuring individual Items, Agents, Entities, Resources, and their properties | **Complete** |
| **[cristalise-dsl-script](#cristalise-dsl-script)** | Script definitions | Writing Groovy/JavaScript scripts for routing, validation, computations | **Complete** |
| **[cristalise-dsl-query](#cristalise-dsl-query)** | Query definitions | Defining queries for listOfValues, dynamic data fetching | **Placeholder** |
| **[cristalise-dsl-role](#cristalise-dsl-role)** | Role definitions | Defining Roles and Permissions for access control | **Complete** |
| **[cristalise-dsl-domain](#cristalise-dsl-domain)** | Domain definitions | Creating or modifying DomainContext Items | **Placeholder** |
@formatter:on

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

### cristalise-dsl-property

**Status**: Placeholder — Content TBD

Use when creating or modifying **Property** constructs — mutable and immutable key-value metadata pairs that define Item configuration, classification, and behavior. This skill will provide:

- Property vs InmutableProperty syntax and usage
- Property configuration patterns
- Type constraints and validation rules
- Best practices for property organization

**Location**: [`../cristalise-dsl-property/SKILL.md`](../cristalise-dsl-property/SKILL.md)

**Leading Words**: Property, InmutableProperty, metadata, configuration, mutable, immutable

---

### cristalise-dsl-item

**Status**: Complete

Use when creating and configuring individual **Items** — the business objects at the heart of CRISTAL-iSE. This skill provides:

- Complete Item definition syntax and constructor attributes
- Property configuration (Module, Name, Type, InmutableProperty, Property)
- Outcome reference syntax for Schema binding
- Dependency and DependencyDescription for relationship definitions
- Full ImportItem Description Item reference
- Item lifecycle integration with Workflow

**Location**: [`../cristalise-dsl-item/SKILL.md](../cristalise-dsl-item/SKILL.md)

**Leading Words**: Item, Agent, Entity, Resource, Description, CRUD, property, Type, Module, Name, relationship

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

---

### cristalise-dsl-role

**Status**: Complete

Use when defining **Role** constructs — named collections of Permissions that can be assigned to Agents for access control. This skill provides:

- Complete Role definition syntax and attributes
- Permission configuration (string and map format)
- Shiro WildcardPermission syntax reference
- CRISTAL-iSE domain:action:target mapping
- Best practices for restrictive permissions

**Location**: [`../cristalise-dsl-role/SKILL.md](../cristalise-dsl-role/SKILL.md)`

**Leading Words**: Role, Permission, jobList, Shiro, WildcardPermission

---

### cristalise-dsl-domain

**Status**: Placeholder — Content TBD

Use when creating or modifying **DomainContext** constructs — domain namespaces and context for organizing Items and managing domain-level configurations. This skill will provide:

- DomainContext definition syntax and structure
- Domain namespace configuration
- Domain-level properties and permissions
- Best practices for domain organization

**Location**: [`../cristalise-dsl-domain/SKILL.md`](../cristalise-dsl-domain/SKILL.md)

**Leading Words**: DomainContext, domain, namespace, context, configuration

---

## How to Use This Router

When writing CRISTAL-iSE DSL files, use this table to find the right skill:

### Quick Decision Guide

| What You're Writing | Use This Skill | Leading Word |
|---------------------|----------------|--------------|
| Item lifecycle with Activities, Splits, Joins | cristalise-dsl-workflow | Workflow |
| Data structure for business Outcomes | cristalise-dsl-schema | Schema |
| Property and metadata configurations | cristalise-dsl-property | Property, InmutableProperty |
| Container for organizing Items | cristalise-dsl-module | Module |
| Individual business objects | cristalise-dsl-item | Item, Agent, Entity, Resource, Description, CRUD |
| Routing logic, validation, computations | cristalise-dsl-script | Script |
| Dynamic data sources, listOfValues | cristalise-dsl-query | Query |
| Role and Permission definitions | cristalise-dsl-role | Role |
| Domain namespaces and context | cristalise-dsl-domain | DomainContext |

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
- When the agent mentions writing workflows, schemas, properties, modules, items, scripts, queries, roles, or domains

## When to Use Individual Skills

| Task | Use Skill |
|------|-----------|
| Define Item lifecycle with Activities, Splits, Joins | cristalise-dsl-workflow |
| Create data structure for business Outcomes | cristalise-dsl-schema |
| Configure Property and InmutableProperty metadata | cristalise-dsl-property |
| Organize Items into logical containers with dependencies | cristalise-dsl-module |
| Configure individual business objects and their properties | cristalise-dsl-item |
| Write Groovy/JavaScript for routing, validation, or computations | cristalise-dsl-script |
| Define dynamic data sources for listOfValues or runtime access | cristalise-dsl-query |
| Define Roles and Permissions for access control | cristalise-dsl-role |
| Define DomainContext and domain namespaces | cristalise-dsl-domain |

## See Also

- [writing-great-skills](../writing-great-skills/SKILL.md) — The blueprint for skill writing principles
- [cristalise-dsl-workflow](../cristalise-dsl-workflow/SKILL.md) — Complete workflow DSL reference
- [cristalise-dsl-schema](../cristalise-dsl-schema/SKILL.md) — Complete schema DSL reference
- [cristalise-dsl-property](../cristalise-dsl-property/SKILL.md) — Property DSL reference (placeholder)
- [cristalise-dsl-module](../cristalise-dsl-module/SKILL.md) — Module DSL reference (placeholder)
- [cristalise-dsl-item](../cristalise-dsl-item/SKILL.md) — Complete Item DSL reference
- [cristalise-dsl-script](../cristalise-dsl-script/SKILL.md) — Complete Script DSL reference
- [cristalise-dsl-query](../cristalise-dsl-query/SKILL.md) — Query DSL reference (placeholder)
- [cristalise-dsl-role](../cristalise-dsl-role/SKILL.md) — Complete Role DSL reference
- [cristalise-dsl-domain](../cristalise-dsl-domain/SKILL.md) — Domain DSL reference (placeholder)
