/**
 * Clients for the browser-session login endpoints, wired in
 * discovery + dispatch mode:
 *
 * - the page renders whatever `GET /login-methods` reports instead of
 *   hard-coding a method list;
 * - every method submission goes to the unified dispatch entry point
 *   (`POST /login?_m=<name>`) rather than addressing per-type processing
 *   URLs, so adding a method server-side needs no frontend change;
 * - the only direct calls left are the CSRF token fetch and the OTP
 *   ticket issue, neither of which is a login submission.
 *
 * Paths mirror this deployment's server configuration; see
 * doc/apis/zh-cn/APIs-#-Login.md for the contract.
 */

const DISPATCH_URL = '/login';
const METHOD_PARAMETER = '_m';
const CSRF_FETCHING_URL = '/_csrf';
const OTP_ISSUE_URL = '/otp/tickets';
const LOGIN_METHODS_FETCHING_URL = '/login-methods';

export interface LoginMethod {
  name: string;
  type: string;
  primary: boolean;
  attributes: Record<string, string>;
}

/** Rendered when discovery is unavailable or reports nothing. */
export const PASSWORD_FALLBACK: LoginMethod = {
  name: 'password',
  type: 'password',
  primary: true,
  attributes: {},
};

interface CsrfToken {
  headerName: string;
  parameterName: string;
  token: string;
}

export type { CsrfToken };

export type SubmitResult =
  | { ok: true; redirectUrl: string }
  | { ok: false; error: string; message?: string };

export type IssueResult =
  | { ok: true; otpTicket: string; retryAfter: number }
  | { ok: false; error: string; message?: string };

/** Error code plus the envelope's explicit message, if any. */
interface ErrorEnvelope {
  error: string;
  message?: string;
}

function envelopeOf(payload: Record<string, unknown> | null, fallback: string): ErrorEnvelope {
  return {
    error: payload && typeof payload.error === 'string' ? payload.error : fallback,
    message: payload && typeof payload.message === 'string' ? payload.message : undefined,
  };
}

/** Deep-link target the login page was entered with, if any. */
export function pendingRedirectUrl(): string | null {
  return new URLSearchParams(window.location.search).get('redirect_url');
}

export async function fetchLoginMethods(): Promise<LoginMethod[]> {
  const res = await fetch(LOGIN_METHODS_FETCHING_URL, {
    headers: { Accept: 'application/json' },
    credentials: 'same-origin',
  });
  if (!res.ok) {
    return [];
  }
  const body: unknown = await res.json().catch(() => null);
  return Array.isArray(body) ? (body as LoginMethod[]) : [];
}

/**
 * The CSRF token of the current session, or null when the endpoint is
 * off or unreachable. Shared by the dispatch submissions and by pages
 * that post native forms carrying the token as a hidden field.
 */
export async function fetchCsrfToken(): Promise<CsrfToken | null> {
  const res = await fetch(CSRF_FETCHING_URL, {
    headers: { Accept: 'application/json' },
    credentials: 'same-origin',
  });
  if (!res.ok) {
    return null;
  }
  return (await res.json().catch(() => null)) as CsrfToken | null;
}

async function postDispatch(
  csrf: CsrfToken,
  methodName: string,
  fields: Record<string, string>,
): Promise<Response> {
  const url = `${DISPATCH_URL}?${METHOD_PARAMETER}=${encodeURIComponent(methodName)}`;
  return fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      // Required: without an explicit JSON Accept the endpoint answers
      // with redirects a fetch client cannot use.
      Accept: 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: new URLSearchParams(fields),
    credentials: 'same-origin',
  });
}

async function readError(res: Response): Promise<ErrorEnvelope> {
  return envelopeOf(await res.json().catch(() => null), 'authentication_failed');
}

/**
 * Submit one method's collected fields through the dispatch entry point.
 *
 * A `307` replay (fields complete) is followed transparently by fetch,
 * which resubmits the body to the method's processing endpoint; what
 * lands here is therefore either the processing endpoint's envelope or
 * the dispatch's own `200 {redirect_url}` for an incomplete submission.
 * A stale CSRF token is refreshed and retried once.
 */
export async function submitDispatch(
  methodName: string,
  fields: Record<string, string>,
): Promise<SubmitResult> {
  const csrf = await fetchCsrfToken();
  if (!csrf) {
    return { ok: false, error: 'invalid_csrf_token' };
  }

  let res = await postDispatch(csrf, methodName, fields);
  if (!res.ok && (await peekError(res)) === 'invalid_csrf_token') {
    const fresh = await fetchCsrfToken();
    if (fresh) {
      res = await postDispatch(fresh, methodName, fields);
    }
  }

  if (res.ok) {
    const payload = (await res.json().catch(() => null)) as Record<string, unknown> | null;
    const redirectUrl = payload && typeof payload.redirect_url === 'string' ? payload.redirect_url : '/';
    return { ok: true, redirectUrl };
  }
  return { ok: false, ...await readError(res) };
}

/** Reads the error code without consuming the response body twice. */
async function peekError(res: Response): Promise<string> {
  return res.clone().json().catch(() => null).then((payload: Record<string, unknown> | null) =>
    payload && typeof payload.error === 'string' ? payload.error : '');
}

/**
 * Ask the OTP issue endpoint to deliver a code. Anonymous and
 * CSRF-exempt by contract; not a login submission, hence not routed
 * through dispatch.
 */
export async function issueOtpTicket(channel: string, recipient: string): Promise<IssueResult> {
  const res = await fetch(OTP_ISSUE_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      Accept: 'application/json',
    },
    body: new URLSearchParams({ channel, recipient }),
    credentials: 'same-origin',
  });
  const payload = (await res.json().catch(() => null)) as Record<string, unknown> | null;
  if (res.ok && payload && typeof payload.otp_ticket === 'string') {
    // Seconds the issuer asks clients to wait before requesting another
    // code; drives the resend countdown. Absent means no waiting.
    const retryAfter = typeof payload.retry_after === 'number' ? payload.retry_after : 0;
    return { ok: true, otpTicket: payload.otp_ticket, retryAfter };
  }
  return { ok: false, ...envelopeOf(payload, 'server_error') };
}
