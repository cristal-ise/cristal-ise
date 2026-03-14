# Web UI Module Guidelines

## Project Overview
Modern web-based user interface for CRISTAL-iSE, built as an Angular NX workspace.

## Technology Stack
- **Angular 19**: Frontend framework.
- **NX**: Workspace management tool.
- **TypeScript**: Main development language.
- **PrimeNG 19**: UI component library.
- **Tailwind CSS**: Styling.
- **Transloco**: Internationalization.

## Project Structure
- `apps/admin`: Main administrative Angular application.
- `apps/webui`: Main user interface Angular application.
- `libs/core`: Core Angular library (services, models, common components).
- `tools`: Development and build tools.

## Development and Build
- **Install dependencies**: `npm install`.
- **Run applications**: `npx nx serve <app-name>`.
- **Build applications**: `npx nx build <app-name>`.
- **Run tests**: `npx nx test <app-name-or-lib>`.

## Guidelines for Junie
- Follow NX workspace patterns for adding new apps or libraries.
- Use PrimeNG components for UI development.
- Adhere to the established Tailwind CSS utility classes for styling.
- All common logic should be placed in `libs/core`.
- Ensure new services are properly tested in `src/lib/services/*.spec.ts`.
