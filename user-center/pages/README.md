# user-center-pages

Admin frontend for the `user-center` project, built with React 19 + Vite 8 + Ant Design 6. Build artifacts are deployed into the parent Spring Boot project (`demo/user-center`) under `resources/static` and `resources/templates/admin`, where the entry page is rendered by Thymeleaf.

## Tech Stack

- React 19 + React Router 8
- Vite 8 (build / dev server)
- Ant Design 6 + @ant-design/icons 6
- ESLint 10
- i18next 26 + react-i18next 17 + @fluent/langneg 0.7

## Requirements

- Node.js >= 22.22.0 (required by `react-router` 8 and `vite` 8)
- npm >= 10

## Install

```bash
npm install
```

## Development

Start the Vite dev server (defaults to http://localhost:5173 ):

```bash
npm run dev
```

> Note: `index.html` contains Thymeleaf expressions (e.g. `th:text="${euler.ctx.__SITE_NAME}"`), which are only resolved when the page is rendered by the `user-center` Spring Boot service. They are NOT processed when opened directly via `npm run dev`; the dev server is only intended for component-level debugging.

## Lint

```bash
npm run lint
```

## Build

```bash
npm run build
```

The output is emitted to `dist/`:

- `dist/index.html` - entry page with Thymeleaf placeholders
- `dist/assets/` - bundled JS / CSS / static resources

## Deploy to user-center

A helper script [`build.sh`](./build.sh) is provided to build and copy the output into the parent Spring Boot project:

```bash
./build.sh
```

What the script does:

1. Runs `npm run build` to generate `dist/`.
2. Cleans `../src/main/resources/static/` and `../src/main/resources/templates/admin/`.
3. Copies `dist/assets` into `../src/main/resources/static`.
4. Copies `dist/index.html` into `../src/main/resources/templates/admin` as the Thymeleaf template.

> Make sure `npm install` has been executed and the current working directory is `demo/user-center/pages` before running the script.

## Preview

To preview the static build output locally (without Thymeleaf rendering):

```bash
npm run preview
```

## Project Layout

```
pages/
├── src/
│   ├── admin/          # Admin pages
│   │   ├── AdminLayout.jsx  # Top header + left sider shell (light theme)
│   │   └── User.jsx         # User management page
│   ├── i18n/           # Internationalization runtime
│   │   ├── index.js         # i18next init, setLocale, getAntdLocale
│   │   ├── match.js         # SUPPORTED_LOCALES + @fluent/langneg matcher
│   │   └── locales/         # zh-Hans.json, en.json
│   ├── assets/         # Static assets
│   ├── index.css       # Global styles (light theme baseline)
│   └── main.jsx        # Application entry (BrowserRouter + nested routes)
├── index.html          # Vite entry template (with Thymeleaf placeholders)
├── vite.config.js      # Vite configuration
├── eslint.config.js    # ESLint configuration
├── build.sh            # Build and deploy script
└── package.json
```

## Routing

All admin pages are mounted under `/admin/console` (the React Router
`basename`) and wrapped by `AdminLayout`:

- `/admin/console` -> redirects to `/admin/console/user`
- `/admin/console/user` -> user management page
- `/admin/console/oauth2/client` -> OAuth2 client management page

XHR calls from these pages target `/admin/api/**` on the same origin and
rely on session + CSRF token; cross-origin access is intentionally not
permitted. Public Bearer-only APIs live under `/api/**` and follow a
completely separate authentication and CORS policy.

The layout provides a fixed top header (collapse toggle, breadcrumb-ish title, user dropdown) and a left navigation sider with a light color scheme (`#ffffff` surfaces on a `#f5f7fa` body).

## Internationalization

The admin console ships with two locales:

| Tag | Language |
|---|---|
| `zh-Hans` | Simplified Chinese |
| `en` | English |

The active locale is exposed consistently across the application:

- i18next instance language (`i18n.language`)
- Resource files under [src/i18n/locales/](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/i18n/locales)
- `localStorage` key `admin.locale`
- `document.documentElement.lang`
- LocaleSwitcher menu `key` in the top header

Default locale is `en`. On first load the runtime resolves the active locale through `@fluent/langneg`, which carries a CLDR likely-subtags subset and therefore performs script-aware matching out of the box. Examples:

| Browser request | Resolved |
|---|---|
| `zh-CN`, `zh-SG`, `zh-Hans-*` | `zh-Hans` |
| `zh-Hans` | `zh-Hans` |
| `en-US`, `en-GB`, `en-AU`, `en` | `en` |
| `zh-TW`, `zh-HK`, `zh-Hant` | `en` (matching strategy will not silently fall through to Simplified Chinese) |
| anything else | `en` |

The user can override the auto-detected locale via the globe icon in the top header; the choice is persisted in `localStorage` and restored on the next visit.

### Adding a new locale

1. Create `src/i18n/locales/<tag>.json` with the same key tree as the existing files.
2. Register the tag in `SUPPORTED_LOCALES` inside [src/i18n/match.js](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/i18n/match.js).
3. Import the resource file and add a row to the antd locale mapping in [src/i18n/index.js](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/i18n/index.js) (`getAntdLocale`).
4. Add a `language.<tag>` translation in every locale file so the LocaleSwitcher can display the native name.
