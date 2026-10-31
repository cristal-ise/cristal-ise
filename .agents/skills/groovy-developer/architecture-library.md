# Library Architecture Rules

You are building a Groovy Library. Follow these architecture-specific rules in addition to the common Groovy rules.

## Public API Design
- Design a **minimal, intuitive public API**. Every exported symbol is a commitment — keep the surface area small.
- Follow **semantic versioning strictly**: breaking changes = major, new features = minor, bug fixes = patch.
- Write **comprehensive documentation**: README with quick start, API reference, migration guides between major versions.

## Distribution
- Ship both ESM and CJS (for JS/TS) or the idiomatic package format for your language. Support tree-shaking.
- Use the **facade pattern**: expose a clean public API that hides internal complexity. Internal modules should not be importable.
- **Deprecate before removing**. Mark APIs as deprecated for at least one major version before removal. Include the migration path in the deprecation message.
- Write **examples for every public function**. Examples serve as both documentation and regression tests.

## Dependencies
- **Minimise dependencies**. Every dependency is a liability — it can break, have vulnerabilities, or conflict with user deps.
- Version your error types. Users may match on error kinds, so changing error variants is a breaking change.
- Support both sync and async patterns where applicable. Do not force async on users who do not need it.
- Provide TypeScript types (or equivalent type definitions) even if the library is written in plain JS. Types are documentation.
- Use **feature flags** or optional peer dependencies for heavy optional functionality. Keep the core lightweight.

## Release Management
- Write a **CHANGELOG.md** that explains what changed and why, not just a list of commits. Link to relevant issues.
- Run CI against **multiple runtime versions** (Node 18/20/22, Python 3.10/3.12, etc.) to ensure broad compatibility.
- **Publish pre-release versions** (alpha, beta, rc) for major changes. Let users test before committing.
- Monitor **bundle size** in CI. Alert on significant increases. Provide a size badge in the README.
- Write **property-based tests** for core algorithms. Edge cases in libraries affect all downstream users.
