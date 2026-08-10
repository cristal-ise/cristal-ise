# Trigger Module Guidelines

## Purpose
Provides Description-Driven Trigger functionality based on Quartz Scheduler. It allows scheduling state transitions and activity executions.

## Technology Stack
- **Quartz Scheduler**: Core scheduling engine.
- **CRISTAL-iSE Activity StateMachine**: Integration for state-based triggers.

## Key Features
- Dynamic recreation of Quartz Jobs from the persistent JobList of CRISTAL-iSE Agent.
- Use of naming conventions for transitions (e.g., `On`, `Duration`, `Unit` suffixes) for Quartz Job creation.

## Configuration
- `Trigger.Enabled` (default: true): Master switch for the module.
- `Trigger.agent` (default: `triggerAgent`): Name of the agent.
- `Trigger.StateMachine.name`: Required StateMachine to retrieve from the backend.

## Guidelines for Junie
- Triggers are persistent in the `JobList` of the Agent.
- Ensure the appropriate `IntervalUnit` is used for time calculations.
- Check the `module.xml` for agent definitions.
