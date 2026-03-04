# Core Library Guidelines (WebUI)

## Purpose
Shared library containing core logic, services, models, and UI components used by both `admin` and `webui` apps.

## Key Components
- **Services**: `LookupService`, `LoginService`, etc. for backend communication.
- **Models**: TypeScript interfaces for Items, Domains, Descriptions, and Activities.
- **Components**: Shared UI elements (e.g., domain tree, search inputs).

## Technology Stack
- **Angular 19**.
- **RxJS**: For reactive data streams.
- **Tailwind CSS**.

## Guidelines for Junie
- All common services and models MUST reside in this library.
- Ensure all services have corresponding unit tests in `src/lib/services/*.spec.ts`.
- Follow consistent naming conventions for models and interfaces.
- Use `lib/shared` for truly generic UI components.
