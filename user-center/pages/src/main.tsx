import './index.css';
import './i18n';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router';
import { router } from './router';

/**
 * No `antd/dist/reset.css` here — it globally recolors `<a>` to the
 * primary blue and (being unlayered CSS) outranks Tailwind's layered
 * preflight, painting sidebar menu buttons / breadcrumb links blue.
 * The reference console relies on Tailwind preflight for base styles
 * and antd cssinjs for component styles; we follow suit.
 *
 * No global antd `<App>` wrapper either: it renders an `.ant-app` hash
 * div around the whole tree, and antd's unlayered runtime rule
 * `:where(.hash) a { color: colorLink }` would then recolor every
 * anchor in the shell (sidebar / breadcrumb) blue, beating Tailwind's
 * layered utilities. Instead `<App>` wraps only the console's
 * business route tree (see AntdAppRoute) so `useApp()` keeps working
 * on business pages while the console chrome stays untouched; the
 * console chrome (sidebar / inset header / logout dialog) is pure
 * shadcn and needs no `<App>` at all.
 */
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>,
);
