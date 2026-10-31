---
name: implement-loop
description: "Implement the result of /to-spec and /to-tickets in a sequential manner."
disable-model-invocation: true
metadata:
  credits:
    skill: implement-spec
    author: Matt Pocock
    url: "https://github.com/mattpocock/skills/tree/main/skills/engineering/implement-spec/SKILL.md"
---

You have been provided a spec, perhaps with extra instructions. This spec should have tickets associated with it, describing how to implement the spec. The goal is to implement the entire spec on a single **temporary integration branch**, with every ticket resolved the way the issue tracker closes work.

The issue tracker should have been provided to you. If not, tell the user to run `/setup-matt-pocock-skills`.

The tickets are not a list of steps. They are a **task graph** with blocking relationships between them. This means there is always a **frontier** of tickets which are ready to be grabbed. However, the tickets shall be executed in a **sequential manner** to avoid conflicting edits of the same file. In other words, run `/implement-ticket` for each ticket in its own worktree, one at a time.

## Steps

You must not deviate from this sequence and the instructions of each step because it is designed to control the context window.

1. Read the spec and tickets to understand the task graph.
2. Switch to the main integration branch of the repo (e.g. develop) or stop in case anything is changed or new.
3. Create the **temporary integration branch** named after the spec.
4. Pick one and only ticket from the **frontier** (e.g. choose the ticket with the smallest number), create a worktree on a branch named after the ticket based on the **temporary integration branch**.
5. Run `/implement-ticket` in that worktree.
6. Once the `/implement-ticket` run completes, rebase its branch to the **temporary integration branch**.
7. If the issue tracker closes work through PRs, or the user asks for one, check if a draft PR already exists. If not, open one after the first merge and instruct the PR to close the spec and all of its tickets.
8. If this changes the **frontier** of available tickets, go back to step 4.
9. If a draft PR exists, mark it ready for review. Otherwise, resolve each ticket the way the issue tracker closes work. Report the **temporary integration branch**.
10. Clean up all ticket branches and worktrees.
