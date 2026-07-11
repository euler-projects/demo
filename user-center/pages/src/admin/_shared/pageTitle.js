import {createContext, useContext, useEffect} from 'react';

/**
 * Runtime page-title override channel used by the admin breadcrumb.
 *
 * The default breadcrumb label for the current page comes from the
 * route's `handle.title` i18n key — no per-page code required. When a
 * page needs a dynamic label (e.g. displaying an entity name instead
 * of a static "User Detail"), it calls `usePageTitle(label)` and the
 * breadcrumb's trailing segment uses that value.
 *
 * The context exposes only `setValue`; the current override lives in
 * AdminLayout state, so children never re-render just because someone
 * else changed the value. Passing `null` / `undefined` clears the
 * override so the breadcrumb falls back to `handle.title`; cleanup on
 * unmount / dependency change happens automatically.
 */
export const PageTitleContext = createContext(null);

/**
 * Publish a dynamic breadcrumb label for the current page. Pass a
 * falsy value (or omit the effect entirely) to keep the static
 * `handle.title` from the route table.
 */
export const usePageTitle = (title) => {
    const ctx = useContext(PageTitleContext);
    useEffect(() => {
        if (!ctx) return undefined;
        ctx.setValue(title ?? null);
        return () => ctx.setValue(null);
    }, [ctx, title]);
};
