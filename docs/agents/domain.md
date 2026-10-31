# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`GLOSSARY.md`** at the repo root, if it exists.
- **`CONTEXT-MAP.md`** at the repo root, if it exists — read the linked `CONTEXT.md` files relevant to the topic.
- **`docs/adr/`** — read ADRs that touch the area you're about to work in.

If any of these files don't exist, proceed silently. Don't suggest creating them upfront; `/domain-modeling` creates documentation lazily when terms or decisions get resolved.

## File structure

The default layout is single-context: a root `GLOSSARY.md` and `docs/adr/`. The existing `CONTEXT-MAP.md` and module `CONTEXT.md` files remain available as legacy domain documentation.

## Use the glossary's vocabulary

When your output names a domain concept, use the term defined in the relevant domain documentation. Don't drift to synonyms the glossary explicitly avoids.

If the concept isn't documented yet, reconsider whether it belongs to the project language or note a real gap for `/domain-modeling`.

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding it.
