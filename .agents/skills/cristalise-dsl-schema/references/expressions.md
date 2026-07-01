# Expressions in Schema DSL

Expressions allow you to define fields whose values are automatically computed based on other fields in the same record.

## Properties

| Property | Type | Description |
|---|---|---|
| `name` | String | Overwrites generated name (default: `<schemaName><fieldName>UpdateExpression`). |
| `version` | Integer | (Optional) Version of the generated script (defaults to Schema version). |
| `inputFields` | List<String> | Fields used for computation. Change triggers the script. |
| `imports` | List<String> | Required Java/Groovy classes. |
| `expression` | String | The Groovy logic. |
| `compileStatic` | Boolean | Whether to use `@CompileStatic` (default: `true`). |
| `loggerPrefix` | String | (Optional) Prefix for the logger (e.g., `org.cristalise.template`). |
| `loggerName` | String | (Optional) Explicit logger name. |

## Injected Variables

The following variables are available in the Groovy expression:
- **Input Fields**: Variables named after the `inputFields`.
- `item`: `ItemProxy` of the current item.
- `agent`: `AgentProxy` of the current user.
- `schema`: The `Schema` object.
- `builder`: `OutcomeBuilder` instance.

## Limitations
- Input fields must be at the same level as the computed field.
- Only `UpdateScript` is currently generated.
