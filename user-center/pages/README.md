# user-center-pages

Admin frontend for the `user-center` project. Build artifacts are deployed into the parent Spring Boot project (`demo/user-center`) under `resources/static` and `resources/templates/admin`, where the entry page is rendered by Thymeleaf.

The console follows a **dual-stack** architecture:

- **shadcn/ui (Base UI variant) + Tailwind CSS v4** own the shell: sidebar, breadcrumb, dropdown menus, avatar / skeleton / tooltip primitives, and the sign-out confirmation dialog — everything on the `ConsoleLayout` chrome is pure shadcn (no antd).
- **Ant Design 6** owns the feature pages: user management, user detail, and OAuth2 client management.

Splitting responsibilities this way lets the shell benefit from shadcn's modern Tailwind-native aesthetics while feature pages keep antd's rich, business-oriented widgets (Table with server-side pagination, Descriptions, Popconfirm, Modal, complex Form validation, etc.). The antd `<App>` context (message / modal holders) is scoped to the business route tree only via `AntdAppRoute`, so its unlayered global rules never leak onto the shadcn chrome.

## Tech Stack

- React 19 + React Router 8 (`createBrowserRouter` + `RouterProvider`)
- TypeScript 5 (strict) — the whole source tree is `.ts` / `.tsx`; there is no ESLint setup (`npm run lint` runs `tsc --noEmit`)
- Vite 8 (build / dev server)
- shadcn/ui (Base UI variant, style `base-nova`, baseColor `neutral`) + Tailwind CSS v4
- Base UI (`@base-ui/react`) as the accessible primitive layer
- Ant Design 6 + @ant-design/icons 6 (feature pages only)
- lucide-react icons + a local Lucide-compatible custom icon library (`src/icons/`)
- i18next 26 + react-i18next 17 + @fluent/langneg 0.7

## Requirements

- Node.js >= 22.22.0 (required by `react-router` 8 and `vite` 8)
- npm >= 10

## Install

```bash
npm install
```

## Development

Start the Vite dev server (defaults to http://localhost:5173):

```bash
npm run dev
```

> `index.html` contains Thymeleaf expressions (e.g. `th:text="${euler.ctx.__SITE_NAME}"`) which only resolve when the page is rendered by the `user-center` Spring Boot service. The dev server is intended for component-level debugging; without a real backend the `/user` and `/_csrf` requests will fail, the sidebar footer falls back to a static placeholder and the site name falls back to `"User Center"` — this is expected in dev. The app is served under the `/admin/console` router basename, so open http://localhost:5173/admin/console.

## Lint

Type-checks the entire tree via `tsc --noEmit` (there is no separate ESLint setup):

```bash
npm run lint
```

## Build

```bash
npm run build
```

`npm run build` runs `tsc -b` (type-check) followed by `vite build`. Output is emitted to `dist/`:

- `dist/index.html` — entry page with Thymeleaf placeholders preserved
- `dist/assets/` — bundled JS / CSS / static resources

## Deploy to user-center

A helper script [`build.sh`](./build.sh) builds and copies the output into the parent Spring Boot project:

```bash
./build.sh
```

The script runs `npm run build`, wipes `../src/main/resources/static/` and `../src/main/resources/templates/admin/`, then copies `dist/assets` into static resources and `dist/index.html` into the Thymeleaf template directory.

> Assets are emitted with absolute `/assets/…` paths (no Vite `base` override) because the built `index.html` is served by Spring as a Thymeleaf template while `dist/assets` is copied to the static root; a relative `base: './'` would resolve against the `/admin/console` route and 404.

## Project Layout

```
pages/
├── src/
│   ├── admin/                     # Business feature pages (antd)
│   │   ├── User.tsx               # antd — user list + create/reset/enable/disable/delete
│   │   ├── UserDetail.tsx         # antd — user identities + authorities
│   │   ├── OAuth2Client.tsx       # antd — OAuth2 client list
│   │   └── _shared/
│   │       ├── api.ts             # apiGet / apiPost / apiPut / apiDelete + extractApiError (CSRF)
│   │       ├── tableLayout.tsx    # antd Table cell/action scaffold: OverflowTags, RowActions, EditableCell…
│   │       └── tableLayout.css    # cell-scoped styles (affordance reveal on row hover)
│   ├── components/                # Shared, business-agnostic components
│   │   ├── AntdAppRoute.tsx       # antd <App> context boundary for the business route tree
│   │   ├── DataTable.tsx          # generic antd Table wrapper (column settings + filter panel)
│   │   ├── DetailLayout.tsx       # detail-page heading + spacing scaffold
│   │   ├── FilterPanel.tsx        # collapsible antd Form filter bar
│   │   ├── InfoDescriptions.tsx   # antd Descriptions with expand/collapse clamping
│   │   └── ui/                    # shadcn/ui primitives (Base UI variant, TSX)
│   ├── hooks/use-mobile.ts        # shadcn stock, consumed by sidebar.tsx
│   ├── icons/                     # Lucide-compatible custom icon library
│   │   ├── create-icon.tsx        # factory mirroring lucide-react's createLucideIcon
│   │   ├── brand.tsx              # Brand (Euler Project spiral-shell mark)
│   │   ├── shield-lock.tsx        # ShieldLock (shield outline + padlock)
│   │   ├── shield-oauth.tsx       # ShieldOAuth (shield outline + inner "A")
│   │   └── index.ts               # barrel export
│   ├── i18n/                      # Internationalization runtime
│   │   ├── index.ts               # i18next init, setLocale, getAntdLocale
│   │   ├── match.ts               # SUPPORTED_LOCALES + @fluent/langneg matcher
│   │   ├── datetime.ts            # locale-aware date/time formatting + column-width helpers
│   │   └── locales/               # zh-Hans.json, en.json
│   ├── layout/                    # Console chrome (pure shadcn)
│   │   ├── ConsoleLayout.tsx      # shadcn Sidebar shell + frosted inset header + auto breadcrumb
│   │   ├── ConsoleLayout.css      # layout-scoped CSS (icon-rail padding fix, sidebar overscroll)
│   │   ├── LogoutConfirmModal.tsx # shadcn AlertDialog hosting the CSRF logout <form>
│   │   └── _shared/pageTitle.ts   # PageTitleContext + usePageTitle hook
│   ├── lib/utils.ts               # cn() = clsx + tailwind-merge
│   ├── assets/                    # Static assets
│   ├── index.css                  # Tailwind v4 entry + shadcn theme tokens
│   ├── main.tsx                   # ReactDOM entry (mounts RouterProvider)
│   └── router.tsx                 # createBrowserRouter + route metadata (handle.title)
├── index.html                     # Vite entry template (with Thymeleaf placeholders)
├── components.json                # shadcn CLI config (base-nova, tsx=true, iconLibrary=lucide)
├── tsconfig.json                  # strict TS config (@/* → src/*)
├── tsconfig.node.json             # TS config for vite.config.ts
├── vite.config.ts                 # Vite + @tailwindcss/vite + path alias
├── build.sh                       # Build and deploy script
└── package.json
```

## Routing

All admin pages are mounted under `/admin/console` (the React Router `basename`) and wrapped by `ConsoleLayout`. Routes are declared in [src/router.tsx](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/router.tsx) via `createBrowserRouter` (required so `useMatches()` returns the route table's `handle` fields) and mounted by [src/main.tsx](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/main.tsx). The business routes are nested under an `AntdAppRoute` element that provides the antd `<App>` context (message / modal holders) without wrapping the shadcn chrome:

| Path (after basename)    | Element         | `handle.title`                 |
|--------------------------|-----------------|--------------------------------|
| `/`                      | Redirect to `users` | —                          |
| `/users` (index)         | `<User/>`       | `nav.user` (via parent wrapper)|
| `/users/:userId`         | `<UserDetail/>` | `user.detailPage.breadcrumb`   |
| `/oauth2/clients`        | `<OAuth2Client/>` | `nav.oauth2_client`          |

The `users` and `users/:userId` routes are nested under a shared parent Route (with no `element` of its own but with `handle: {title: 'nav.user'}`) so `/users/:userId` naturally contributes both "Users" (link back to list) and "User Detail" (current) to the breadcrumb.

XHR calls from these pages target `/admin/api/**` on the same origin and rely on session + CSRF token; cross-origin access is intentionally not permitted. Public Bearer-only APIs live under `/api/**` and follow a completely separate authentication and CORS policy.

## Console shell (ConsoleLayout)

The layout is a shadcn Sidebar in `collapsible="icon"` mode with a floating content card on the right (sidebar-08 look, sidebar-09 rail width). The whole console is pinned to one viewport (`h-svh overflow-hidden`); long feature pages scroll inside the rounded content card rather than pushing it past the screen:

- **SidebarHeader**: brand tile — the local `Brand` icon + site name (from the `<meta name="site-name">` injected by Thymeleaf, falling back to `"User Center"` in dev).
- **NavMain**: menu tree with two forms depending on sidebar state:
  - Expanded: `<Collapsible>` with the entire parent row acting as the trigger (chevron rotates via `group-aria-expanded/menu-button:rotate-90`).
  - Collapsed (icon rail): parent tile opens a right-anchored `<DropdownMenu>` listing its children, so groups without a route of their own (e.g. "Identity & Access", "OAuth2") remain reachable from the folded rail.
- **NavLocale** (pinned at the bottom of `SidebarContent`): globe button + radio DropdownMenu across `SUPPORTED_LOCALES`.
- **NavUser** (SidebarFooter): avatar + username + role subtitle with a skeleton loading state, and a DropdownMenu with Profile / Sign out.
- **InsetHeader**: `SidebarTrigger` + `Separator` + auto breadcrumb, in a frosted-glass bar (`bg-background/60 backdrop-blur`) that sticks to the top of the page scroll surface; its bottom seam stays transparent at rest and fades in once content slides underneath.

antd's `ConfigProvider` (theme + locale) wraps only the routed feature pages (`<Outlet/>`), never the chrome above it.

### Local CSS overrides

`src/index.css` stays neutral (Tailwind v4 entry + shadcn theme tokens + the `tw-animate-css` / `shadcn/tailwind.css` imports that supply the overlay keyframes and cross-primitive `data-*` variants). Layout-scoped overrides travel with their owner:

- [src/index.css](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/index.css) keeps one `@layer base` rule that restores `border-color: var(--border)` — Tailwind v4 defaults borders to `currentColor`, which would paint a black seam on shadcn Sidebar's `border-r`.
- [src/layout/ConsoleLayout.css](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/layout/ConsoleLayout.css) carries the console chrome fixes: a `@layer utilities` rule that zeros the padding on collapsed `size="lg"` sidebar buttons (shadcn's CVA declares both `p-2!` and `p-0!` with equal specificity, and Tailwind v4's value-ascending emit order makes `p-2!` win, clipping the collapsed avatar / brand tile), plus an `overscroll-behavior: none` rule for the sidebar's own scroll surface.

Rationale for each is inline in the respective CSS file.

## Auto breadcrumb

Breadcrumb data is derived, not imperative — feature pages contribute zero code by default.

1. Each `<Route>` declares an i18n key via `handle: {title: '...'}` in `router.tsx`.
2. `useBreadcrumbTrail(menuItems, override)` in `ConsoleLayout` calls `useMatches()`, filters to titled routes, and composes a trail:
   - The **menu tree** contributes the top-level parent-group segment (e.g. "Identity & Access"). Not navigable — it's a group label.
   - Each **titled ancestor route** becomes a `BreadcrumbLink`.
   - The **deepest titled route** becomes the current `BreadcrumbPage`; its label is `override ?? t(handle.title)`.
3. The separator between the parent-group segment and its child is upgraded from a decorative chevron to a `DropdownMenu` that lists every sibling entry under that group (a Finder-style sibling switcher). Every other separator stays decorative.
4. A layout-scoped `SEARCH_CACHE: Map<pathname, search>` remembers each list page's `?query` and injects it back into link URLs, so returning via a breadcrumb crumb restores state (e.g. `/users?page=3` survives round-trips through `/users/:userId`).

### Dynamic labels for a page

Rare — most pages are fine with the static `handle.title`. When needed (e.g. showing an entity name), a page calls:

```tsx
import {usePageTitle} from '@/layout/_shared/pageTitle';

usePageTitle(user ? user.username : null); // null → falls back to handle.title
```

The hook publishes into `PageTitleContext` (owned by `ConsoleLayout`), automatically clears on unmount, and requires no other plumbing.

## Skeleton loading

The `/user` fetch drives the sidebar footer. To avoid the flash of static fallback text before the real username arrives, `NavUser` renders a shadcn `Skeleton` (avatar block + two text bars) while `userLoading === true`, then swaps to the real content once the fetch resolves. If the fetch fails or the user isn't authenticated, `userLoading` still transitions to `false` and NavUser falls back to the static `header.profile / header.console` labels — so the sidebar never pulses forever.

Skeleton dimensions are aligned pixel-for-pixel with the loaded content, keeping the tile height within ~2px between states so nothing shifts when data lands.

## Sign-out

`layout/LogoutConfirmModal.tsx` is a shadcn `AlertDialog` (no antd) that owns the whole logout protocol: on open it fetches a fresh CSRF token from `/_csrf`, renders a real `<form action="/logout" method="post">` with the token as a hidden input (the action URL comes from the `<meta name="logout-processing-url">` injected by Thymeleaf), and delegates submission to the browser. The confirm affordance is a native `<button type="submit" form=…>` styled with `buttonVariants` — Base UI's `<Button>` pins `type="button"`, which would never submit the form. Spring Security's 302 is followed natively, giving a clean full-page reload.

## Icons

Menu / UI icons come from [`lucide-react`](https://lucide.dev). Icons that Lucide does not ship live in [src/icons/](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/icons):

- `create-icon.tsx` — a factory that mirrors `lucide-react`'s internal `createLucideIcon`. Its output is a drop-in replacement for any `Lucide*` export: same default props (`size=24`, `strokeWidth=2`, `currentColor`), same SVG defaults, so downstream utility classes (`[&_svg]:size-4` etc.) resize / recolor these icons the same way they do Lucide's.
- `brand.tsx` — `Brand`, the Euler Project spiral-shell mark used in the sidebar header.
- `shield-lock.tsx` / `shield-oauth.tsx` — `ShieldLock` (shield + padlock) and `ShieldOAuth` (shield + inner "A"), used by the OAuth2 menu group.
- `index.ts` — barrel export.

Adding a new icon: drop a new file next to `shield-oauth.tsx`, call `createIcon(name, paths)`, and re-export it from `index.ts`.

## Internationalization

The admin console ships with two locales:

| Tag       | Language           |
|-----------|--------------------|
| `zh-Hans` | Simplified Chinese |
| `en`      | English            |

The active locale is exposed consistently across the application:

- i18next instance language (`i18n.language`)
- Resource files under [src/i18n/locales/](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/i18n/locales)
- `localStorage` key `admin.locale`
- `document.documentElement.lang`
- antd `ConfigProvider` locale (via `getAntdLocale`)

Default locale is `en`. On first load the runtime resolves the active locale through `@fluent/langneg`, which carries a CLDR likely-subtags subset and performs script-aware matching out of the box:

| Browser request              | Resolved   |
|------------------------------|------------|
| `zh-CN`, `zh-SG`, `zh-Hans-*`| `zh-Hans`  |
| `zh-Hans`                    | `zh-Hans`  |
| `en-US`, `en-GB`, `en-AU`, `en` | `en`    |
| `zh-TW`, `zh-HK`, `zh-Hant`  | `en` (matcher won't silently fall through to Simplified Chinese) |
| anything else                | `en`       |

The user can override the auto-detected locale via the globe button in the sidebar; the choice is persisted in `localStorage` and restored on the next visit.

### Adding a new locale

1. Create `src/i18n/locales/<tag>.json` with the same key tree as the existing files.
2. Register the tag in `SUPPORTED_LOCALES` inside [src/i18n/match.ts](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/i18n/match.ts).
3. Import the corresponding antd locale bundle and add a row to `ANTD_LOCALES` in [src/i18n/index.ts](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/i18n/index.ts).
4. Add a `language.<tag>` translation in every locale file so the LocaleSwitcher can display the native name.

## Adding a new page

1. Create the page component under `src/admin/` (feature pages typically use antd). Register it inside the `AntdAppRoute` node in `router.tsx` if it calls `App.useApp()` for message / modal holders.
2. Register the route in [src/router.tsx](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/router.tsx) with `handle: {title: '<i18n-key>'}` so the breadcrumb picks it up automatically.
3. If the page belongs to a submenu group, add an entry to `buildMenuItems` in [src/layout/ConsoleLayout.tsx](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/src/layout/ConsoleLayout.tsx) so the sidebar and the sibling-switcher dropdown pick it up.
4. Add the corresponding i18n keys under `nav.*` (menu label) and any page-specific keys under a dedicated namespace in the locale files.
5. If the page has list ↔ detail navigation and needs to preserve list-view query state on return, no extra work is required — the layout's `SEARCH_CACHE` handles it automatically for any route with `handle.title`.

## Adding a new shadcn component

The project is configured for the shadcn CLI (see [components.json](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/user-center/pages/components.json)). Base UI variant, TSX, neutral base color, Lucide icons:

```bash
npx shadcn@latest add <name>
```

Generated files land under `src/components/ui/`.
