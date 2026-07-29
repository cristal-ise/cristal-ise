---
name: cristalise-dsl-query
description: Complete reference for defining Query Items in CRISTAL-iSE DSL. Use when creating queries for listOfValues, dynamic data fetching, or runtime data access, or when another skill needs query definition details.
---

# CRISTAL-iSE Query DSL

**Query** defines executable data retrieval operations in CRISTAL-iSE. Queries are used for dynamic data fetching, populating listOfValues in PropertyDescs, and accessing runtime data. A Query encapsulates a query string in a specific language (currently only SQL supported) with optional parameters, root/record element configurations, and dialect specifications.

## When to Use This Skill

- Defining Query Items for data retrieval
- Creating queries for listOfValues
- Configuring query parameters and their types
- Setting query language and dialect
- Customizing XML output structure with rootElement and recordElement

## Quick Start

### Minimal Query with SQL

```groovy
Query('CustomerList', 0) {
    query(language: 'sql') {
        "SELECT * FROM customers"
    }
}
```

### Query with parameters

```groovy
Query('CustomerById', 0) {
    parameter(name: 'customerId', type: 'java.lang.String')
    parameter(name: 'status',   type: 'java.lang.String')
    
    query(language: 'sql') {
        """SELECT * FROM customers 
            WHERE id = '@{customerId}' 
            AND status = '@{status}'"""
    }
}
```

### Query with dialect and custom XML elements

```groovy
Query('PostgresItems', 0) {
    rootElement('ItemsResult')
    recordElement('CustomerItem')
    
    query(language: 'sql', dialect: 'postgres') {
        """SELECT ip."UUID", ip."VALUE" as "Name" 
            FROM "ITEM_PROPERTY" ip 
            WHERE ip."NAME" = 'Name'"""
    }
}
```

## Core Concepts

| Concept           | Description                                                                                   |
|-------------------|-----------------------------------------------------------------------------------------------|
| **query**         | Container for executable query definitions with language, parameters, and output configuration |
| **language**      | Query language type.                                                                          |
| **dialect**       | Optional language dialect (e.g., `postgres`, `mysql`, `oracle`)                               |
| **parameter**     | Input parameter to be used to contruct the final query                                        |
| **rootElement**   | XML root element name for query results                                                       |
| **recordElement** | XML element name for each result record                                                       |

## Key Rules

1. **language** is mandatory: Supported languages:`sql`
2. **parameter** type must be fully qualified: Use full Java class names (e.g., `java.lang.String`, `java.lang.Integer`)
3. **parameter** name and type are mandatory
4. **parameters** are substituted: Use MVEL2 template syntax `@{paramName}`
5. **rootElement** defaults to `QueryResult`
6. **recordElement** defaults to `Record`
7. **version** defaults to 0
8. **namespace** inherited from Module

## Best Practices

### Query Definition
- Use explicit version numbers (typically 0 for new queries)
- Place queries in logical modules based on their usage domain
- Group related queries together in Module's Queries block
- Use descriptive names that indicate the query's purpose

### Parameters
- Use meaningful parameter names that describe their purpose
- Always specify parameter types explicitly
- Use `java.lang.String` for text parameters
- Use appropriate numeric types for numeric parameters
- Order parameters logically (most used first, or in execution order)

### Query Strings
- Use only triple double-quote strings (""") for multi-line queries
- Escape special characters appropriately for the target language
- Keep queries focused on a single responsibility

### XML Output
- Customize `rootElement` and `recordElement` when the defaults don't fit your use case
- Ensure output structure matches what consumers expect
- Consider the downstream processing of query results

## Anti-Patterns

**AVOID:**
- Hardcoded values in queries that should be parameters
- Overly complex queries that do too much
- Queries without proper parameter substitution
- Using reserved SQL/XML keywords as parameter names without escaping

## Error Handling

| Error                                                      | Cause                                      | Fix                                   |
|------------------------------------------------------------|--------------------------------------------|---------------------------------------|
| `Query data incomplete, must specify language`             | Missing language attribute                 | Provide language parameter            |
| `Incomplete Query Parameter: must have name and type`      | Parameter missing name or type             | Provide both name and type attributes |
| `Query:... incomplete XML data, missing 'value' attribute` | rootElement or recordElement without value | Provide value attribute               |
| `QueryParsingException`                                    | Malformed query XML or invalid structure   | Fix query XML structure               |

## Information Hierarchy

```
cristalise-dsl-query/
└── SKILL.md
    ├── When to Use This Skill
    ├── Quick Start
    ├── Core Concepts
    ├── Key Rules
    ├── Best Practices
    ├── Anti-Patterns
    └── Error Handling
```

## See Also

- [writing-cristalise-dsl](../writing-cristalise-dsl/SKILL.md) — Router for all DSL skills
- [cristalise-dsl-module](../cristalise-dsl-module/SKILL.md) — Module DSL reference
- [cristalise-dsl-item](../cristalise-dsl-item/SKILL.md) — Item DSL reference
- [kernel/CONTEXT.md](../../../kernel/CONTEXT.md) — Domain definitions of Leading Words
