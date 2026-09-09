/**
 * Shared API utilities for admin pages.
 * Handles CSRF token fetching and common HTTP methods.
 */

/** CSRF token shape returned by the `/_csrf` endpoint. */
export interface CsrfToken {
    headerName: string;
    parameterName: string;
    token: string;
}

/**
 * Fetch a fresh CSRF token from the server.
 */
export async function fetchCsrf(): Promise<CsrfToken> {
    const res = await fetch('/_csrf', {
        headers: { Accept: 'application/json' },
    });
    return res.json() as Promise<CsrfToken>;
}

/**
 * Perform a JSON POST request with CSRF protection.
 */
export async function apiPost<T = any>(url: string, body: unknown): Promise<T> {
    const csrf = await fetchCsrf();
    const res = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify(body),
    });
    return res.json() as Promise<T>;
}

/**
 * Perform a JSON PUT request with CSRF protection.
 */
export async function apiPut<T = any>(url: string, body: unknown): Promise<T | null> {
    const csrf = await fetchCsrf();
    const res = await fetch(url, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify(body),
    });
    if (res.status === 204) return null;
    const contentLength = res.headers.get('Content-Length');
    if (contentLength === '0') return null;
    return res.json() as Promise<T>;
}

/**
 * Perform a DELETE request with CSRF protection.
 */
export async function apiDelete<T = any>(url: string): Promise<T | null> {
    const csrf = await fetchCsrf();
    const res = await fetch(url, {
        method: 'DELETE',
        headers: {
            [csrf.headerName]: csrf.token,
        },
    });
    if (res.status === 204) return null;
    const contentLength = res.headers.get('Content-Length');
    if (contentLength === '0') return null;
    return res.json() as Promise<T>;
}

/**
 * Perform a JSON GET request.
 */
export async function apiGet<T = any>(url: string): Promise<T | null> {
    const res = await fetch(url, {
        headers: { Accept: 'application/json' },
    });
    if (!res.ok) return null;
    return res.json() as Promise<T>;
}

/**
 * Extract a human-readable message from an error surfaced by the fetch
 * helpers above. Consumed by the shared table scaffold to report inline
 * edit failures through antd's message API.
 */
export function extractApiError(err: unknown): string {
    if (err instanceof Error) return err.message;
    return String(err);
}
