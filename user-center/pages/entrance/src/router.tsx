import { createBrowserRouter } from 'react-router';
import EntranceLayout from './layout/EntranceLayout';
import SignIn from './login/SignIn';
import SignOut from './logout/SignOut';
import App from './App';

/**
 * Route table for the entrance app.
 *
 * The framework serves this SPA shell at the exact security-page paths
 * (`/signin`, `/create-account`, `/signout`), which are sibling paths
 * rather than children of a shared prefix — so there is no single router
 * `basename` to pin here. Every page renders inside EntranceLayout, which
 * owns the centred column and the shared footer. Only `/signin` and
 * `/signout` are implemented in this pass; the catch-all keeps the root
 * (`/`) usable in `vite dev`.
 */
export const router = createBrowserRouter([
  {
    element: <EntranceLayout />,
    children: [
      { path: '/signin', element: <SignIn /> },
      { path: '/signout', element: <SignOut /> },
      { path: '/create-account', element: <App /> },
      { path: '*', element: <SignIn /> },
    ],
  },
]);
