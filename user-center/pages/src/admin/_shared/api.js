/**
 * Shared API utilities for admin pages.
 * Handles CSRF token fetching and common HTTP methods.
 */

/**
 * Fetch a fresh CSRF token from the server.
 * @returns {Promise<{headerName: string, token: string}>}
 */
export async function fetchCsrf() {
    const res = await fetch('/_csrf', {
        headers: { Accept: 'application/json' },
    });
    return res.json();
}

/**
 * Perform a JSON POST request with CSRF protection.
 */
export async function apiPost(url, body) {
    const csrf = await fetchCsrf();
    const res = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify(body),
    });
    return res.json();
}

/**
 * Perform a JSON PUT request with CSRF protection.
 */
export async function apiPut(url, body) {
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
    return res.json();
}

/**
 * Perform a DELETE request with CSRF protection.
 */
export async function apiDelete(url) {
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
    return res.json();
}

/**
 * Perform a JSON GET request.
 */
export async function apiGet(url) {
    const res = await fetch(url, {
        headers: { Accept: 'application/json' },
    });
    if (!res.ok) return null;
    return res.json();
}
