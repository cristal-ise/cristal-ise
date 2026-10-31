# Spock Testing Rules

**Core principle:** Data-driven testing isn't optional. Use `where:` blocks for similar tests with different inputs. Similar tests with different inputs = one parameterized test with `where:` block.

## The Iron Rule

Writing 3+ similar tests = **YOU MUST use where: block**

No exceptions:
- Not "I'll refactor later"
- Not "Copy-paste is faster"
- Not "These are slightly different"
- Not "I'm under time pressure"

**Why:** Refactoring to `where:` takes 2 minutes. Maintaining 10 separate tests takes hours.

## Red Flags - STOP and Use where: Block

You're about to violate the Iron Rule if:
- "I'm writing my 3rd test with same structure"
- "Just need to change the input value"
- "Copy-paste-modify is fastest"
- "Each test is simple enough"
- "I'll consolidate later"

**All of these mean: Use where: block NOW.**

## Spock Block Structure

| Block     | Purpose                       | Example                                       |
|-----------|-------------------------------|-----------------------------------------------|
| `given:`  | Setup, stubs                  | `repository.findById(1) >> Optional.of(user)` |
| `when:`   | Execute action                | `service.processOrder(orderId)`               |
| `then:`   | Assertions, mock verification | `1 * service.save(_)`                         |
| `expect:` | Single-line assertion         | `calculator.add(2, 3) == 5`                   |
| `where:`  | Data table for parameters     | `a \| b \| sum`                               |

## Mock vs Stub

- **Stub** → Return fake data → Goes in `given:` → Use `>>`

  ```groovy
  given:
  repository.findById(1) >> Optional.of(user)  // Stub
  ```

- **Mock** → Verify interaction → Goes in `then:` → Use `*`

  ```groovy
  then:
  1 * emailService.sendWelcome(user)  // Mock verification
  ```

## where: Block Syntax

```groovy
where:
columnA | columnB | expected
value1  | value2  | result1
value3  | value4  | result2
```

Use `#variable` in test names to show which parameter is tested:

```groovy
def 'should validate #email as #validity'() {
    expect:
    validator.isValid(email) == isValid

    where:
    email              | validity | isValid
    'user@example.com' | 'valid'  | true
    'invalid'          | 'invalid'| false
}
```

## Common Mistakes

| Mistake                       | Fix                                                                   |
|-------------------------------|-----------------------------------------------------------------------|
| Writing 3+ similar tests      | Use `where:` block                                                    |
| Stub in `then:` block         | Move to `given:`                                                      |
| Mock verification in `given:` | Move to `then:`                                                       |
| Test name: `testCalculate()`  | Use full sentence: `"should calculate discount for premium customer"` |
| Hardcoded timestamps          | Use `LocalDateTime.of(2025, 1, 15, 10, 30)`                           |
| Magic numbers                 | Use named variables or data table columns                             |

## Testing Strategy

### Integration Tests
- Test **only happy path** with typical example
- Focus on external interfaces
- Keep mocking minimal

### Unit Tests
- Cover **edge cases, errors, boundaries**
- Use `where:` blocks for variations
- One behavior per test

## Validation Example

```groovy
def 'should reject invalid email: #reason'() {
    expect:
    !validator.isValid(email)

    where:
    email              | reason
    null               | 'null'
    ''                 | 'empty'
    'no-at-sign'       | 'missing @'
    'a' * 255 + '@x'   | 'too long'
}

def 'should accept valid email: #email'() {
    expect:
    validator.isValid(email)

    where:
    email << ['user@example.com', 'a@b.co', 'user+tag@example.com']
}
```

## Rationalization Table

| Excuse                         | Reality                                                     |
|--------------------------------|-------------------------------------------------------------|
| "Copy-paste is faster"         | Refactoring takes 2 min, maintaining duplicates takes hours |
| "I'll consolidate later"       | Later never comes, duplication stays                        |
| "These are slightly different" | Different inputs = perfect for `where:` block               |
| "I'm under time pressure"      | Bad tests slow you down more than writing good ones         |
| "Each test is simple"          | Simple + duplicated = maintenance nightmare                 |
| "I need more coverage"         | 10 separate tests ≠ better than 1 parameterized test        |

## Red Flags Checklist

Before writing a test, check:
- [ ] Am I testing similar behavior with different inputs?
- [ ] Does this look like my previous 2 tests?
- [ ] Am I about to copy-paste-modify?
- [ ] Could these be rows in a data table?

**If ANY are true → Use where: block**

## Additional Spock Rules

- Structure tests with `given:`, `when:`, `then:` blocks — use `expect:` for pure assertions with no state changes.
- Parameterise with `where:` data tables: `where: a | b | expected; 1 | 2 | 3; 4 | 5 | 9` — each column row is a separate test case.
- Create mocks with `Mock(ServiceClass)` — stub returns with `service.method() >> returnValue` and verify calls with `1 * service.save(_)`.
- Use `Spy(RealClass)` for partial mocking — calls through to real methods unless explicitly stubbed with `>>` or `>>>` chained closures.
- Assert exceptions with `thrown(IllegalArgumentException)` in `then:` — use `notThrown(Exception)` for the absence of exceptions.
- Add `@Unroll` to expand parameterised test names: `@Unroll "adding #a to #b gives #expected"` — uses `#variable` interpolation in the method name.
- Use `with(object) { name == "Alice"; age == 30 }` for grouped property assertions — cleaner than multiple bare assertions.
- Extend `Specification` in every test — add `@Subject SomeClass subject = new SomeClass()` to make the class under test explicit.
- Use `@Shared` for expensive fixtures: `@Shared DatabaseHelper db = new DatabaseHelper()` — initialise in `setupSpec()`, clean up in `cleanupSpec()`.
- Write argument matchers in interaction constraints: `1 * repo.save({ User u -> u.name == "Alice" })` — closure is evaluated per invocation.
- Use `GroovyMock()` when mocking Groovy classes with dynamic dispatch or final methods that Java mocking cannot handle.
- Combine `where:` tables with `@Unroll` and `@FailsWith` to document and test known-failing edge cases explicitly.
- Integrate Spock with JaCoCo via Gradle (`jacocoTestReport` task) to generate coverage reports for Spock-based test suites.
- Use `DetachedMockFactory` to create Spock mocks inside Spring `@Configuration` classes for integration tests that use the Spring context.

## General Testing Rules

- Write unit tests for every new function or method immediately after implementation.
- Run the full unit test suite before committing — never push code with failing tests.
- Test one behavior per test case. Keep tests fast, isolated, and deterministic.
- Follow the Arrange-Act-Assert pattern: set up inputs, call the function, verify the output.
- Mock external dependencies (APIs, databases, file system) — unit tests validate your logic in isolation.
- Name tests descriptively: `should return empty array when no items match filter`.
- Test edge cases: empty inputs, nulls, boundary values, error conditions — not just the happy path.
- Run unit tests after every code change during development for fast feedback.
- Aim for high coverage on business logic (80%+), but don't chase 100% — test behavior, not implementation details.
- Use test factories or builders to create consistent test data — avoid hardcoded inline objects.
- Keep tests independent — no test should depend on another test's state or execution order.
- When a bug is found, write a failing test first that reproduces it, then fix the code.
- Organize tests to mirror source structure: `src/utils/parse.groovy` → `test/utils/parseSpec.groovy`.
- Use parameterized/table-driven tests for functions with many input/output combinations.
