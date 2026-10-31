---
name: groovy-developer
description: Use when working on Groovy projects with Spock testing. Loads common Groovy/Spock rules and architecture-specific guidelines for library, CLI, REST API, and WebApp projects.
---

# Groovy Developer Skill

You are an expert **Groovy** developer working with **Spock** testing framework. This skill consolidates common Groovy coding patterns, performance optimizations, error handling strategies, and Spock testing best practices, with architecture-specific guidelines for library, CLI, REST API, and Web Application projects.

## When to Use

- Writing or refactoring Groovy code
- Designing architecture for Groovy projects
- Writing or reviewing Spock tests
- Working on library, CLI, REST API, or web app projects in Groovy
- Need guidance on Groovy-specific performance optimizations

## Project Context

Identify your project type and follow the corresponding architecture rules:
- **Library** → See [architectures/library.md](architectures/library.md)
- **CLI** → See [architectures/cli.md](architectures/cli.md)
- **REST API** → See [architectures/restapi.md](architectures/restapi.md)
- **Web App** → See [architectures/webapp.md](architectures/webapp.md)

## Code Style & Structure

### Groovy Defaults

Write **idiomatic Groovy** that balances expressiveness with performance:

- Use `camelCase` for methods and variables, `PascalCase` for class names, and `UPPER_SNAKE_CASE` for constants declared with `static final`.
- Prefer Groovy's **optional typing** (`def`) for local variables and closures, but use **explicit types** for public API method signatures.
- Use **single-quote** for string literals instead of double-quotes (see rule bellow).
- Use **GStrings** (`"Hello, ${name}!"`) for interpolation rather than string concatenation — avoid `"${simple_variable}"` and use `"$simple_variable"` instead.
- Use **`?.` safe navigation** and **`?:` Elvis operator** to handle nullable values concisely without explicit null checks.
- Prefer **Groovy collections API** (`collect`, `findAll`, `groupBy`, `inject`) over Java-style `for` loops for data transformation.
- Use **`@CompileStatic`** on performance-sensitive classes to enable static type checking and improve runtime performance.
- Use **`@Canonical`**, **`@Immutable`**, or **`@ToString`** AST transformations instead of manually writing boilerplate `equals`, `hashCode`, and `toString` methods.
- Use **`@TypeChecked`** on classes where dynamic features are not needed to catch type errors at compile time rather than runtime.
- Use **`@SLF4J`** for logging
- Prefer **named argument maps** in method calls (`createUser(name: 'Alice', role: 'admin')`) over positional argument lists for clarity.
- Use **`with {}` blocks** to group multiple operations on the same object instead of repeating the variable name.
- Use **Groovy traits** for interface-like reuse with default method implementations instead of abstract classes when the hierarchy is flat.
- Use **`@groovy.transform.EqualsAndHashCode`** with `includes` or `excludes` parameters to control which fields participate in equality comparison.

### Paradigm Selection

Choose the right paradigm for each responsibility:
- Use **functional patterns** for data transformations and pure logic.
- Use **OOP** for stateful components, services, and domain modeling.
- Choose the paradigm that best fits each module's responsibility.

## Performance Optimization

**Leading word: tight** — aim for tight, deterministic, low-overhead Groovy code.

- Use **`@CompileStatic`** on hot-path classes to bypass Groovy's dynamic dispatch overhead and generate bytecode comparable to compiled Java.
- Prefer **`List.collectMany {}`** over nested `collect {}` with flatten to avoid creating intermediate lists in large data transformations.
- Use **`LazyMap`** and **`@Lazy`** initialization for expensive computed properties that may not always be accessed.
- Avoid overusing **dynamic features** (`methodMissing`, `propertyMissing`) in performance-critical paths — they add method lookup overhead on every call.
- Use **`eachParallel {}`** or Java **`parallelStream()`** for CPU-bound collection operations that can be safely parallelized.
- Cache results of expensive **`@Memoized`** method calls by annotating them with **`@groovy.transform.Memoized`** for pure functions.
- Prefer **`StringBuilder`** over GString concatenation in tight loops — GString creates intermediate strings on each interpolation.
- **Profile before optimizing** with JVisualVM or async-profiler — dynamic dispatch overhead is rarely the bottleneck in I/O-bound code.
- Use **`@CompileStatic`** with type coercion on scripts that process large data files to reduce the cost of repeated dynamic type checking.
- Use Groovy's **`@TailRecursive`** annotation to convert tail-recursive functions into iterative loops and avoid stack overflow on deep recursion.
- Avoid calling **`metaClass`** methods in production code — accessing the meta-object protocol disables compile-time optimizations and adds per-call overhead.
- Use **`GParsPool.withPool { list.collectParallel { } }`** for parallel collection operations rather than manual thread pool management.

## Error Handling

**Leading word: relentless** — be relentless about proper error handling.

See [error-handling.md](error-handling.md) for Groovy error-handling mechanics and strategy, including:
- Catching, wrapping, and structuring errors (specific catch types, multi-catch, result wrappers, domain exception hierarchies)
- Error channel policy (exceptions vs Result/Either, error taxonomy, retry logic, error contracts)

## Testing with Spock

**Leading word: red** — your tests should go red on bugs, and stay red until fixed.

### The Iron Rule

> **Writing 3+ similar tests = YOU MUST use where: block**

No exceptions. Refactoring to `where:` takes 2 minutes. Maintaining 10 separate tests takes hours.

### Spock Essentials

Structure tests properly:
- **`given:`** - Setup, stubs (use `>>` for stubbing)
- **`when:`** - Execute action
- **`then:`** - Assertions, mock verification (use `*` for mocks)
- **`expect:`** - Single-line assertion
- **`where:`** - Data table for parameters

Mock vs Stub:
- **Stub** → Return fake data → Goes in `given:` → Use `>>`
- **Mock** → Verify interaction → Goes in `then:` → Use `*`

### Key Practices

- Use **`@Unroll`** with meaningful names: `@Unroll "adding #a to #b gives #expected"`
- Use **`thrown(ExceptionType)`** in `then:` to assert exceptions
- Use **`@Shared`** for expensive fixtures, initialize in `setupSpec()`, clean up in `cleanupSpec()`
- Use **`GroovyMock()`** when mocking Groovy classes with dynamic dispatch
- Combine **`where:`** tables with **`@Unroll`** and **`@FailsWith`** for edge cases
- Use **`with(object) { assertions }`** for grouped property assertions
- Integrate **Spock with JaCoCo** via Gradle for coverage reports
- Use **`DetachedMockFactory`** for Spock mocks in Spring `@Configuration` classes

### General Testing Rules

- Write unit tests for **every new function** immediately after implementation.
- **Run the full suite before committing** — never push failing tests.
- Test **one behavior per test** case. Keep tests fast, isolated, deterministic.
- Follow **Arrange-Act-Assert** pattern.
- **Mock external dependencies** — unit tests validate logic in isolation.
- Name tests **descriptively**: `should return empty array when no items match filter`.
- Test **edge cases**: empty inputs, nulls, boundary values, error conditions.
- Aim for **80%+ coverage** on business logic, but don't chase 100%.
- Use **test factories/builders** for consistent test data.
- Keep tests **independent** — no cross-test dependencies.
- Write **failing test first** when a bug is found, then fix.
- **Mirror source structure** in test organization.
- Use **parameterized/table-driven tests** for many input combinations.

### Full Spock Reference

See [testing-spock.md](testing-spock.md) for complete Spock testing guidelines including:
- Data-driven testing patterns
- Common mistakes and how to avoid them
- Testing strategy (integration vs unit)
- Validation examples
- Rationalization table for common excuses
- Red flags checklist

## Architecture-Specific Rules

Choose your architecture type and follow the corresponding rules:

| Architecture | File                                               | When to Use                        |
|--------------|----------------------------------------------------|------------------------------------|
| **Library**  | [architecture-library.md](architecture-library.md) | Building reusable Groovy libraries |
| **CLI**      | [architecture-cli.md](architecture-cli.md)         | Command-line applications          |
| **REST API** | [architecture-restapi.md](architecture-restapi.md) | HTTP API services                  |
| **WebApp**   | [architecture-webapp.md](architecture-webapp.md)   | Web applications with UI           |

Each architecture file contains rules specific to that project type. The common rules above apply to all Groovy projects regardless of architecture.
