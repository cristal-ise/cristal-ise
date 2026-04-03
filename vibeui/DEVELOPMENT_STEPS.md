# Project Evolution & Historical Development Steps

This document outlines the major milestones in the development of the VibeUI project.

### 📜 Development Timeline

1.  **Foundation**: Initialized Angular 21 project with standalone components, Signals, and zoneless detection.
2.  **Architecture**: Implemented core Layout components (Sidebar, Topbar, Footer) and configured application routing.
3.  **UI/UX Refinement**: Integrated PrimeNG 21 and Tailwind CSS 4. Applied glassmorphism effects and specialized chart visualizations.
4.  **Theme System**: Engineered a professional, theme-aware color system with a Slate-based palette supporting seamless Light/Dark mode transitions.
5.  **Data Layer**: Integrated OpenAPI 3.1 generator for automated, type-safe communication with backend services.
6.  **Authentication & Services**: Connected the Frontend Login component to the backend authentication system. Implemented Base64 credential encoding and robust JSON error handling for JAX-RS responses. Also added logout functionality.
7.  **Route Protection & Navigation**: Implemented a global `AuthGuard` and a centralized `AuthService` using Signals. This ensures that only authenticated users can access the dashboard. Also enhanced the navigation flow to automatically redirect users to their intended destination after a successful login.
8.  **Session Timeout & Auto-Logout**: Engineered a configurable, performance-optimized idle `SessionTimeoutService` running outside Angular's detection loops. Added preemptive, sticky PrimeNG Toast alerts notifying users of imminent expiration, alongside automated logouts that gracefully push inactive users back to the login interface with persistent localized warnings. `SessionTimeoutService` only monitors activity on `AuthGuard`-protected routes through dynamic router event tracking.
9.  **Internationalization (i18n)**: Integrated `@jsverse/transloco` for robust multi-language support (English, French, German, Hungarian). Engineered a custom `TranslocoHttpLoader` supporting a dual-source strategy (Local JSON + Future Remote REST API). Refactored all application components (Landing, Login, Dashboard, Settings, Sidebar, Topbar) to use the `TranslocoPipe` and dynamic interpolation for parameterized strings. Centralized language-switching logic into a reusable `LanguageSelector` component.

---
*For development, maintenance, and deployment commands, please refer back to the [README.md](README.md).*
