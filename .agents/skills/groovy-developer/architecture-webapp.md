# Web App Architecture Rules

You are building a Groovy Web Application. Follow these architecture-specific rules in addition to the common Groovy rules.

## Architecture Layers
- **Separate UI components, business logic, and data fetching** into distinct layers.
- Choose a rendering strategy **intentionally**: SSR for SEO, CSR for interactivity, SSG for static content.

## Rendering
- Use **server-side rendering (SSR)** or **static generation (SSG)** for initial page loads — hydrate on the client for interactivity.
- Implement **client-side routing** with proper loading and error states for each route.

## State Management
- Use a **state management** approach appropriate to complexity — local state first, global store when needed.
- Optimise **bundle size** with code splitting, lazy loading, and dynamic imports.

## Performance & Caching
- Implement **proper caching strategies**: browser cache headers, service worker, and CDN.
- Design for **progressive enhancement** — core functionality should work without JavaScript.

## Error Handling
- Use **structured error boundaries** to prevent full-page crashes from component errors.
