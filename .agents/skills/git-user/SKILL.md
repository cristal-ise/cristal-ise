---
name: git-user
description: Conventional Commits and GitFlow branching strategy. Use when writing commit messages, creating branches for tickets, or preparing pull requests.
---

# Git User

Git workflow reference for Conventional Commits and GitFlow branching strategy. Consult this skill when creating branches, formatting commits, and preparing changes for review and release.

## When to Use

- Writing or formatting commit messages
- Creating a new branch for a ticket, issue, or spec
- Managing branch lifecycles (feature, chore, hotfix, release candidate)
- Preparing a pull request and cleaning up branch diffs
- Automating version bumps and changelog generation

## Conventional Commits

Format all commits strictly following the **Conventional Commits** specification:

```
<type>(<scope>): <subject>

[optional body]

[optional footer(s)]
```

### Commit Types & Semantic Versioning

Leading word: **semantic** — choose the type that signals the exact release impact:

| Type       | Description                                             | Release Bump    |
|------------|---------------------------------------------------------|-----------------|
| `feat`     | New user-facing feature                                 | Minor           |
| `fix`      | Bug fix                                                 | Patch           |
| `chore`    | Maintenance, dependencies, or tooling                   | None / Internal |
| `docs`     | Documentation changes only                              | None            |
| `style`    | Formatting, whitespace, code styling                    | None            |
| `refactor` | Code change that neither fixes a bug nor adds a feature | None            |
| `test`     | Adding or updating tests                                | None            |
| `perf`     | Performance improvement                                 | Patch           |

### Format Rules

- **Subject line**: Keep under 50 characters, use the imperative mood (e.g., `add user authentication`, `fix memory leak in parser`), and omit trailing punctuation.
- **Scope**: Optional noun describing the affected codebase section in parentheses (e.g., `feat(auth):`, `fix(api):`).
- **Body**: Optional section separated by a blank line, wrapped at 72–80 characters. Explain the *what* and *why* behind the change rather than repeating the implementation diff.
- **Breaking changes**: Signal breaking changes with `!` before the colon (e.g., `feat(api)!: drop legacy endpoint`) or with a `BREAKING CHANGE:` footer explaining the migration path. Triggers a **Major** version bump.
- **Issue references**: Link issue trackers in the footer (e.g., `Refs: #123`, `Closes: JIRA-456`).

## Branch Strategy (GitFlow)

Leading word: **isolated** — keep work isolated on dedicated branches that branch from and return to designated integration targets.

### Branch Roles & Topology

| Branch            | Base Branch | Merges Into          | Purpose                                     | Protected |
|-------------------|-------------|----------------------|---------------------------------------------|-----------|
| `master` / `main` | —           | —                    | Production-ready code.                      | yes       |
| `develop`         | `master`    | `master`             | Integration branch for pre-production code. | yes       |
| `feature/*`       | `develop`   | `develop`            | New features and capabilities.              | no        |
| `chore/*`         | `develop`   | `develop`            | Maintenance and dependency updates.         | no        |
| `hotfix/*`        | `master`    | `master` & `develop` | Immediate fixes for production regressions. | no        |
| `rc/*`            | `develop`   | `master` & `develop` | Release candidate preparation and metadata. | no        |

### Branch Naming Convention

Construct branch names using the prefix, ticket identifier, and a concise slug:

`<type>/<ticket-id>-<short-description>`

Examples:
- `feature/3-guarded-conditional-branching`
- `chore/42-bump-dependencies`
- `hotfix/108-fix-null-pointer-auth`
- `rc/v1.2.0`

## Workflows & Completion Criteria

### 1. Create a Branch

1. Check out the appropriate base branch (`develop` for features/chores, `master` for hotfixes):
   ```bash
   git checkout develop && git pull origin develop
   ```
2. Create and switch to the named branch following the naming convention:
   ```bash
   git checkout -b feature/<ticket>-<slug>
   ```

**Completion criterion**: Branch exists locally, branches from the latest upstream base commit, and matches `<type>/<ticket>-<slug>`.

### 2. Commit Changes

1. Stage atomic changes representing a single logical unit of work.
2. Commit with a message adhering to Conventional Commits:
   ```bash
   git commit -m "type(scope): imperative subject"
   ```

**Completion criterion**: Subject is ≤ 50 characters in imperative mood without trailing period, references relevant ticket in footer if required, and commit contains only changes relevant to the stated message.

### 3. Prepare a Pull Request

1. Sync local branch with the latest upstream `develop`:
   ```bash
   git fetch origin && git merge origin/develop
   ```
2. Clean up commit history locally using interactive rebase if needed:
   ```bash
   git rebase -i origin/develop
   ```
3. Run project verification suite (linters, typecheck, tests) and confirm clean diff.
4. Push the branch to remote:
   ```bash
   git push origin <branch-name>
   ```

**Completion criterion**: Branch is rebased or cleanly merged with latest `develop`, all local commits follow Conventional Commits, test suite passes, and branch diff is free of unintended changes or merge artifacts.

## Guardrails

- **Shared branch protection**: Push to shared branches (`master`, `develop`) exclusively through approved pull requests after passing CI.
- **Force-push restriction**: Push using standard `git push`. Restrict `--force-with-lease` strictly to unmerged personal feature branches after interactive rebasing.
- **Branch cleanup**: Delete feature, chore, and hotfix branches upon successful merge to prevent repository clutter.
