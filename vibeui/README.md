# VibeUI - Premium Angular Admin Dashboard

VibeUI is a state-of-the-art Angular 21+ administrative dashboard designed with a focus on premium aesthetics, performance, and developer experience. It features a modern design system leveraging Glassmorphism, a dual-theme engine, and seamless integration with PrimeNG and Tailwind CSS 4.

## 🚀 Key Features

-   **Modern Tech Stack**: Built with Angular 21, Signals, and Zoneless change detection.
-   **Premium Design**: Professional Glassmorphism effects with a curated Slate-based color palette.
-   **Dual-Theme System**: Native support for Light and Dark modes with seamless transitions.
-   **API Integration**: Automated TypeScript service generation from OpenAPI 3.1 specifications.
-   **UI Components**: Powered by PrimeNG 21 for high-performance, accessible widgets and charts.
-   **Responsive Layout**: Fully responsive sidebar, topbar, and dashboard architecture.

---

## 🛠 Development Commands

### Environment Setup
Before starting, ensure all dependencies are installed:
```bash
npm install
```

### Local Development Server
Launch the development server with hot-reloading:
```bash
npm run start
# or
ng serve
```
Navigate to `http://localhost:4200/`.

### Real-time Build Watcher
To watch for changes and rebuild the project in development mode:
```bash
npm run watch
```

---

## ⚙️ Configuration

### Backend API Strategy
VibeUI is configured to communicate with its backend using a hybrid approach for flexibility and security:

-   **Local Development**: `proxy.conf.json` is used to map `/api` requests to `http://localhost:8081` (default backend port). This avoids CORS issues during development.
-   **Environment-Specific URLS**: The `apiUrl` is managed in `src/environments/environment.ts`. In production builds, these are swapped with the appropriate values from `environment.prod.ts`.

### Environment Management
| File | Purpose |
| :--- | :--- |
| `src/environments/environment.ts` | Default development configuration. |
| `src/environments/environment.prod.ts` | Optimized production settings. |
| `src/environments/environment.test.ts` | Unit testing configuration. |

---

## 🔧 Maintenance Commands

### API Client Generation
VibeUI uses OpenAPI Generator to maintain synchronization with backend endpoints. When the API specification (`openapi/OpenAPI.yaml`) changes, regenerate the services:
```bash
npm run generate:api
```
This command generates TypeScript-Angular services in `src/app/api` with `providedIn: 'root'` and single request parameter configuration.

### Code Quality & Scaffolding
Standard Angular CLI commands are used for scaffolding:
```bash
ng generate component components/my-component
ng generate service services/my-service
```
Prettier is used for code formatting:
```bash
npx prettier --write .
```

---

## 🧪 Testing

VibeUI utilizes **Vitest** for blistering fast unit testing, replacing the traditional Karma/Jasmine setup for better performance and DX.

### Run Unit Tests
```bash
npm run test
```

### End-to-End Testing (Optional)
If configured, run E2E suites:
```bash
ng e2e
```

---

## 📦 Deployment

### Production Build
Compile the application with full optimizations for production deployment:
```bash
npm run build
```
The output artifacts will be stored in the `dist/vibeui/` directory. The build includes:
-   AOT (Ahead-of-Time) compilation.
-   Tree-shaking and minification.
-   Production-ready environment configurations.

### Environment Management
Configurations are managed via files in `src/environments/`:
-   `environment.ts`: Development defaults.
-   `environment.prod.ts`: Production settings.
-   `environment.test.ts`: Test environment settings.

---

## 🎨 Design System

VibeUI follows a strict design protocol to maintain a premium feel:
-   **Styling**: Powered by Tailwind CSS 4 and the `@primeng/themes` preset.
-   **Themes**: Theme variables are defined in `src/index.css` using CSS custom properties.
-   **Icons**: Integration with `primeicons`.
-   **Typography**: Optimized for readability using modern sans-serif fonts.

---

## 📖 Project Evolution

Major development milestones and historical steps are documented separately to keep the main guide focused on project maintenance and operations.

See the [Development Steps](DEVELOPMENT_STEPS.md) for a detailed timeline.
