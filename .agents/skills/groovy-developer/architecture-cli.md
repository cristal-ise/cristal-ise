# CLI Architecture Rules

You are building a Groovy CLI application. Follow these architecture-specific rules in addition to the common Groovy rules.

## Command Structure
- Structure CLI apps with a clear **command → handler → output pipeline**. Separate argument parsing from business logic.
- Use **subcommands** for complex CLIs. Each subcommand should have its own help text, flags, and validation.

## Exit Codes & Output
- Exit with **meaningful codes**: 0 for success, 1 for general errors, 2 for usage errors. Document exit codes.
- Write to **stdout** for output data, **stderr** for logs/progress/errors. This enables piping and redirection.

## Configuration
- Implement a **config hierarchy**: CLI flags > env vars > config file > defaults. Use XDG directories for config files.
- Validate all inputs **early** and fail fast with clear error messages that include the invalid value and expected format.

## User Experience
- Add **--json** or **--output=json** flag for machine-readable output. Human-readable by default, structured when piped.
- Support **--verbose/-v** and **--quiet/-q** flags. Default output should be minimal but informative.
- Add **shell completion scripts** (bash, zsh, fish). Most CLI frameworks generate these automatically.
- Use **progress bars** for long operations. Detect TTY and suppress progress in non-interactive mode.
- Write **man pages** or generate them from help text. Provide --help at every subcommand level.

## Advanced Features
- Implement **graceful shutdown**: catch SIGINT/SIGTERM, clean up temp files, release locks, flush buffers.
- Use a **plugin system** for extensibility: allow users to add custom subcommands via a known directory or config.
- Add **--dry-run** flag for destructive operations. Show what would happen without making changes.
- Support **stdin** for input when no file argument is given. This enables piping from other commands.
- Include a **self-update mechanism** or version check that warns when a newer version is available.

## Testing
- Test CLI integration by capturing stdout/stderr and asserting on output format and exit codes.
