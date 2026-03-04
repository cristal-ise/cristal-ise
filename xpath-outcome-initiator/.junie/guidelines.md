# XPath Outcome Initiator Module Guidelines

## Purpose
Module for XML/XPath-based outcome generation. It provides implementations of `OutcomeInitiator` for CRISTAL kernel.

## Key Components
- `EmptyOutcomeInitiator`: Generates empty XML from schema.
- `XPathOutcomeInitiator`: Updates generated XML based on XPath expressions in Jobs.

## Technology Stack
- **Apache XMLBeans**: Used for XML manipulation.

## Configuration
- `XPathOutcomeInitiator.PropertyNamePrefix`: Configures the prefix (e.g., `xpath:`) used in activities.

## Guidelines for Junie
- Use this module for activities that need to dynamically update outcomes based on Job data.
- Ensure XPath expressions are valid and follow standard syntax.
- Refer to `SampleXmlUtil` for XML generation logic.
