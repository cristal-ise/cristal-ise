# GUI Module Guidelines

## Purpose
An administrative Swing-based UI for CRISTAL. Provides a low-level view of Items and workflow management.

## Technology Stack
- **Swing**: Java standard library for GUI.
- **CRISTAL Client API**: Used for communicating with the kernel.

## Key Features
- Domain tree browsing and searching.
- Item property overview and modification.
- Graphical workflow visualization and activity execution.
- Outcome data browsing and re-assignment.

## Build and Run
- `mvn clean install` to build.
- Run the GUI by executing the main class (typically specified in `README.md` or `pom.xml`).

## Guidelines for Junie
- Modifying the Swing GUI should be done carefully to maintain compatibility with different JRE versions.
- New GUI components should follow existing layout and styling.
- This is a legacy UI; major new features should instead be implemented in the modern `webui`.
