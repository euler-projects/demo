import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import {createBrowserRouter, Navigate, RouterProvider} from 'react-router'
import './index.css'
import './i18n'
import AdminLayout from './admin/AdminLayout.jsx'
import User from './admin/User.jsx'
import UserDetail from './admin/UserDetail.jsx'
import OAuth2Client from './admin/OAuth2Client.jsx'

// Route metadata:
//
// `handle.title` on each titled route is an i18n key. The admin
// shell picks these up via `useMatches()` and auto-generates the
// breadcrumb — feature pages don't render or mutate breadcrumb
// state. New pages just declare their title here.
//
// The `users` wrapper has no `element` of its own: the index child
// still renders <User/> at `/users`, and `:userId` fully replaces
// it at `/users/:userId`. The shared parent lets `/users/:userId`
// contribute both "Users" (link back to list) and "User Detail"
// (current page) to the breadcrumb without any per-page glue.
//
// `createBrowserRouter` (over `<BrowserRouter>`) is used so
// `useMatches()` returns the route table's `handle` fields.
const router = createBrowserRouter(
    [
        {
            path: '/',
            element: <AdminLayout/>,
            children: [
                {index: true, element: <Navigate to="users" replace/>},
                {
                    path: 'users',
                    handle: {title: 'nav.user'},
                    children: [
                        {index: true, element: <User/>},
                        {
                            path: ':userId',
                            element: <UserDetail/>,
                            handle: {title: 'user.detailPage.breadcrumb'},
                        },
                    ],
                },
                {
                    path: 'oauth2/clients',
                    element: <OAuth2Client/>,
                    handle: {title: 'nav.oauth2_client'},
                },
            ],
        },
    ],
    {basename: '/admin/console'},
)

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <RouterProvider router={router}/>
    </StrictMode>,
)
