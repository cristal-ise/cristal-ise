# Error Handling Reference

**Leading word: relentless** — be relentless about proper error handling.

Reached from the `groovy-developer` skill when writing, reviewing, or refactoring error paths in Groovy code.

## Mechanics

Groovy-specific techniques for catching, wrapping, and structuring errors:

- Use **`try/catch/finally`** with specific exception types — catch the most specific type first, not `Exception` or `Throwable`.
- Use **`?.` safe navigation** to avoid `NullPointerException` when traversing optional object chains: `user?.address?.city`.
- Prefer returning **`Optional.empty()`** or a **null-object pattern** over returning `null` from public methods that may not produce a result.
- Use Groovy's **`multicatch`** syntax (`catch (IOException | SQLException e)`) to handle multiple exception types with shared recovery logic.
- **Log exceptions** with full stack traces using `log.error('Operation failed', e)` rather than `e.printStackTrace()` for consistent log formatting.
- Use **`@Throws(IOException)`** annotations on methods that throw checked exceptions to maintain Java interoperability in mixed codebases.
- Implement service-layer methods with a **result wrapper object** (`Result.success(value)` / `Result.failure(error)`) to make error paths explicit in the return type.
- Use Groovy's **`withCloseable {}`** and **`withStream {}`** patterns for resource management instead of explicit `try/finally` close blocks.
- Define **domain-specific exception hierarchies** (`AppException`, `ValidationException`, `ServiceException`) rather than reusing generic Java exceptions.
- Use **`@EqualsAndHashCode`** on exception classes when they need to be compared in test assertions or used as Map keys.
- Wrap **external API calls** in a `try/catch` that rethrows as a domain exception — this decouples callers from third-party exception hierarchies.
- Use **`Thread.setDefaultUncaughtExceptionHandler`** in long-running Groovy services to log unhandled exceptions from background threads.

## Strategy

Policy for deciding what kind of error channel each failure uses:

- **Combine exceptions** for unexpected failures with **Result/Either types** for expected business errors.
- Use **exceptions** for infrastructure failures (network, I/O, OOM) and **result types** for domain validation errors.
- **Log or propagate every exception** — always attach context (operation name, input values, timestamps) when re-throwing or wrapping.
- Reserve **exceptions for truly exceptional conditions** (infrastructure failures, programming errors); use **Result/Either/Option types** for expected business failures (validation, not-found, permission denied).
- **Wrap third-party exceptions at module boundaries** — translate external errors into domain-specific error types that callers can pattern-match on.
- Use a **centralized error handler** for cross-cutting concerns (logging, monitoring, user-facing messages).
- Prefer **typed error enums** or **discriminated unions** over generic error strings — this enables exhaustiveness checks at compile time and prevents unhandled error paths.
- For **async operations**, ensure errors propagate correctly through promise chains — unhandled rejections should crash the process in production rather than silently failing.
- In **retry logic**, distinguish transient errors (network timeout, 503) from permanent errors (400, 404) — only retry transient failures with exponential backoff and a maximum retry count.
- **Document error contracts** at API boundaries: which errors each function can return and what callers should do about them.
- Define an **error taxonomy**: separate **Recoverable** (retry, fallback) from **Fatal** (crash, alert) from **Expected** (return to caller) errors.
- Attach **structured context** to all errors: operation name, relevant input IDs, timestamp, and correlation/trace ID for distributed tracing.
- Implement a **centralized error handler middleware** for cross-cutting concerns: structured logging, metrics emission, and user-friendly message formatting.
