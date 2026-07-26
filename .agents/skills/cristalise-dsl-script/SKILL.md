---
name: cristalise-dsl-script
description: Complete reference for defining Script Items in CRISTAL-iSE DSL. Use when writing Groovy or JavaScript scripts for routing, validation, or computations, or when another skill needs scripting pattern details.
---

# CRISTAL-iSE Script DSL

**Script** is a Description Item that encapsulates executable code using JSR-223 API. This skill is the complete reference for defining Script resources using the CRISTAL-iSE Groovy DSL. Use **Groovy** as the primary language.

**Bold terms** are defined in [GLOSSARY.md](GLOSSARY.md).

## When to Use This Skill

- Creating new Script resources for custom business logic
- Defining executable code for GET endpoints
- Implementing aggregate scripts for Item data processing
- Creating query list scripts for data lookup operations
- Reusing common script logic through includes
- Understanding Script parameter contracts and binding variables

## Quick Start

Start with 'Minimal Example' in [EXAMPLES.md](EXAMPLES.md).

## MUST DO

- **Always specify script language** explicitly using the language parameter in `script()` method
- **Use external file references** for maintainable scripts rather than inline closures for complex logic
- **Define input/output parameters** to declare script contracts and dependencies
- **Use relative paths with `moduleDir`** for portable script references within modules
- **Include dependent scripts** using `include()` for code reuse across scripts
- **Match output variable names** between DSL output declarations and actual script code

## MUST NOT DO

- **Do not hardcode absolute file paths** - use `moduleDir` for portability across environments
- **Do not omit input/output declarations** - they document the script's contract and enable type checking
- **Do not use inline closures for complex scripts** - use external files for maintainability and debugging
- **Do not assume binding variables exist** - declare them as inputs if your script depends on them

## Workflow: The script-dsl Cycle

1. **Design the contract**: Define input parameters and output types based on script requirements
2. **Select language**: Choose Groovy as the primary language (JSR-223 supports others)
3. **Implement script logic**: Write the actual script code in external .groovy file
4. **Define script resource**: Create the Script resource using DSL with proper file reference and parameter declarations
5. **Verify script**: Test the script executes correctly and produces expected outputs

## Reference Guide

| Topic | Description | File |
|-------|-------------|------|
| **Script Definition** | Script resource DSL syntax, constructor, parameters | [references/script-definition.md](references/script-definition.md) |
| **Script Parameters** | input, output, param methods and type declarations | [references/parameters.md](references/parameters.md) |
| **File References** | Using file paths, moduleDir, and file-based script approach | [references/file-references.md](references/file-references.md) |
| **Script Inclusion** | include() method for script reuse | [references/includes.md](references/includes.md) |
| **Binding Variables** | Automatic binding variables (item, job, agent, etc.) | [references/binding-variables.md](references/binding-variables.md) |
| **Error Handling** | Error output, exceptions, validation | [references/error-handling.md](references/error-handling.md) |

## Setup & Verification

1. **Verify JSR-223 language availability** - Ensure Groovy language engine is on classpath
2. **Test file accessibility** - Confirm script files are readable from moduleDir paths
3. **Validate Script XML** - ScriptBuilder automatically validates XML structure
4. **Test script execution** - Run script with sample inputs to verify outputs

**Completion Criterion**: Script resource creates successfully, file references resolve correctly, and script executes without errors producing expected outputs.

## See Also

- [writing-cristalise-dsl](../writing-cristalise-dsl/SKILL.md) — Router for all DSL skills
- [cristalise-dsl-item](../cristalise-dsl-item/SKILL.md) — Items and Agents that use Scripts
- [cristalise-dsl-workflow](../cristalise-dsl-workflow/SKILL.md) — Workflow DSL for routing script usage
- [kernel/CONTEXT.md](../../../kernel/CONTEXT.md) — Domain definitions of Leading Words
