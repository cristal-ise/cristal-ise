# Permission Reference

**Permission** is a Shiro WildcardPermission string that controls access to Activities, Scripts, and Queries on Items. In CRISTAL-iSE, Permissions follow the `domain:action:target` format mapped to:

- `domain` = **Item Type** (can be overridden by `BuiltInItemProperties.SECURITY_DOMAIN`)
- `action` = **Activity/Query/Script Name** (can be overridden by `BuiltInVertexProperties.SECURITY_ACTION`)
- `target` = **Item UUID or Name** that the operation is executed on

## Syntax

### String Format

Two syntax forms are supported:

**Simple string:**
```groovy
Permission('domain:action:target')
```

**Example:**
```groovy
Permission('Customer:Create:*')  // Allow Create on any Customer
Permission('printer:print:lp7200')  // Allow print on specific printer
```

### Map Format

**Explicit map:**
```groovy
Permission(domain: 'Batch', actions: 'Review', targets: '*')
```

**Note:** The map uses singular `domain`/`action`/`target` keys but accepts comma-separated values for multiple entries in a single part.

## WildcardPermission Rules

### Wildcards

- `*` = matches any value in that part
- `?` = matches a single character (rarely used in CRISTAL-iSE)

**Examples:**
```groovy
Permission('printer:*')        // Any action on printer domain
Permission('*:view')          // View action on any domain
Permission('Customer:*:*')    // Any action on any Customer
```

### Comma-Separated Values

Multiple values can be specified in a single part using commas:

```groovy
Permission('printer:print,query')  // Print OR query on printer
Permission('Customer:Create,Read,Update,Delete:*')  // CRUD on any Customer
```

### Missing Parts

Omitted trailing parts imply wildcard:

```groovy
Permission('printer')       // Equivalent to: printer:*:*
Permission('printer:print') // Equivalent to: printer:print:*
```

**Important:** You cannot omit middle parts. `printer:lp7200` is NOT equivalent to `printer:*:lp7200`.

## CRISTAL-iSE Domain Mapping

| Part | Default Value | Override Property |
|------|---------------|-------------------|
| `domain` | Item Type | `BuiltInItemProperties.SECURITY_DOMAIN` |
| `action` | Activity/Query/Script Name | `BuiltInVertexProperties.SECURITY_ACTION` |
| `target` | Item UUID | N/A (always the target Item) |

## Permission Examples

| Use Case | Permission String | Meaning |
|----------|-------------------|---------|
| Create any Customer | `Customer:Create:*` | Any user can create any Customer |
| View specific Customer | `Customer:Read:12345` | View Customer with UUID 12345 |
| Full access to printer | `printer:*:lp7200` | Any operation on printer lp7200 |
| Admin access | `*:*:*` | Full system access (Admin Role) |
| Manage Batch jobs | `Batch:Manage:*` | Manage any Batch job |
| Review or Approve | `Batch:Review,Approve:*` | Review OR Approve any Batch |

## Implication Logic

Permissions are evaluated by **implication**, not equality. A user with `Customer:*` can perform `Customer:Create:12345`.

**Best Practice:** Use the **most specific** permission string possible for runtime checks to avoid unintended access.

## See Also

- [Apache Shiro WildcardPermission](https://shiro.apache.org/permissions.html) — Full syntax reference
- [kernel/CONTEXT.md](../../../kernel/CONTEXT.md) — CRISTAL-iSE Permission domain definitions
