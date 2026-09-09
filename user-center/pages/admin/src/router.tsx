/**
 * Route table for the admin console.
 *
 * Route metadata:
 *
 * `handle.title` on each titled route is an i18n key. The admin shell
 * picks these up via `useMatches()` and auto-generates the breadcrumb —
 * feature pages don't render or mutate breadcrumb state. New pages just
 * declare their title here.
 *
 * The `users` wrapper has no `element` of its own: the index child
 * still renders <User/> at `/users`, and `:userId` fully replaces it
 * at `/users/:userId`. The shared parent lets `/users/:userId`
 * contribute both "Users" (link back to the list) and "User Detail"
 * (current page) to the breadcrumb without any per-page glue.
 *
 * `createBrowserRouter` (over `<BrowserRouter>`) is used so
 * `useMatches()` returns the route table's `handle` fields.
 */
import { createBrowserRouter, Navigate } from 'react-router';
import { AntdAppRoute } from './components/AntdAppRoute';
import ConsoleLayout from './layout/ConsoleLayout';
import User from './admin/User';
import UserDetail from './admin/UserDetail';
import OAuth2Client from './admin/OAuth2Client';

export const router = createBrowserRouter(
  [
    {
      path: '/',
      element: <ConsoleLayout />,
      children: [
        { index: true, element: <Navigate to="users" replace /> },
        {
          // antd `<App>` context boundary. Every descendant route may
          // call `App.useApp()` (message/modal holders) without
          // wrapping itself — this node is the single declaration of
          // which pages live inside the antd context. The console
          // chrome (sidebar / inset header / logout dialog) stays
          // OUTSIDE on purpose: it is pure shadcn, and antd `<App>`
          // injects unlayered global rules (e.g. an `a` color) that
          // would pollute shadcn-only surfaces. New antd pages go
          // inside this node; a page migrating away from antd simply
          // moves out.
          element: <AntdAppRoute />,
          children: [
            {
              path: 'users',
              handle: { title: 'nav.user' },
              children: [
                { index: true, element: <User /> },
                {
                  path: ':userId',
                  element: <UserDetail />,
                  handle: { title: 'user.detailPage.breadcrumb' },
                },
              ],
            },
            {
              path: 'oauth2/clients',
              element: <OAuth2Client />,
              handle: { title: 'nav.oauth2_client' },
            },
          ],
        },
      ],
    },
  ],
  { basename: '/admin/console' },
);
