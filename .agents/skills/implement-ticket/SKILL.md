---
name: implement-ticket
description: "Implement one ticket without user intervention."
---

Communication to and from subagents should be sparse. Communicate primarily through **context pointers**: to the spec, tickets, research notes, and previous commits. Don't duplicate information already available via pointers.

## Steps
You must not deviate from this sequence and the instructions of each step because it is designed to control the context of the current agent and its subagent. Also, for the request 'Spawn a subagent named `agenName`' use the agent type you think is best suited for the task and start it with a fresh context window.

1. Check the current branch and stop in case anything is changed or new and ask the user to resolve it.
2. Check if the current branch is named after the ticket being implemented. Stop in case of a wrong branch and ask the user to switch to the correct branch.
3. Spawn a subagent named `tdd-developer` to implement the given ticket. Use /tdd at pre-agreed seams. Run typechecking and tests regularly.
4. Wait for `tdd-developer` to complete before continuing, then commit changes.
5. /code-review changes of the last commit save it to docs/reviews
6. Spawn a subagent named `review-developer` to implement all the findings of code-review using /tdd.
7. Wait for `review-developer` to complete before continuing, then commit changes.
8. Spawn a subagent named `static-fixer` to resolve all the findings of static code analysis.
9. Wait for `static-fixer` to complete before continuing, then commit changes.
10. Spawn a subagent named `coverage-fixer` to use the test coverage report and improve tests to achieve coverage numbers listed below.
    - 100% for line, method and class
    - +95% for branch and instruction for each class or the current values if that is higher
11. Wait for `coverage-fixer` to complete before continuing, then commit changes.
12. Spawn a subagent named `test-hardener` to use the mutation testing report and improve tests to achieve coverage numbers listed below.
    - Mutation Coverage +95% and Test Strength +95% or the current values if that is higher 
13. Wait for `test-hardener` to complete before continuing, then commit changes.
14. Spawn a subagent named `test-consolidator` to consolidate tests: find duplicates and common bases to reduce redundancy. Make sure to
    - meet all **Acceptance Criteria** specified in the Specification and in the actual Issue.
    - keep the current **Test coverage** for each package
    - do **NOT** execute mutation testing during test consolidation
15. Wait for `test-consolidator` to complete before continuing, then commit changes.
