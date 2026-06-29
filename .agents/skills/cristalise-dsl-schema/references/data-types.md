# Data Types and Multiplicity

## Supported Types
- `string`
- `integer`
- `decimal`
- `boolean`
- `date`
- `time`
- `dateTime`
- `anyType`

## Multiplicity Formats
| Value | Description |
|---|---|
| `1` | Exactly one (Mandatory) |
| `0..1` | Zero or one (Optional) |
| `1..*` | One or more |
| `*` or `0..*` | Zero or more |
| `n` | Exactly n |

## Common Constraints
- `values`: List of allowed values (Enum).
- `pattern`: Regex for validation.
- `default`: Default value.
- `range`: e.g., `[0..100]` for numeric limits.
