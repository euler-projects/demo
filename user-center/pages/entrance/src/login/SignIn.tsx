import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ErrorAlert } from '@/components/ErrorAlert';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { GoogleIcon } from '@/icons/google';
import { metaContent } from '@/lib/meta';
import { fetchLoginMethods, PASSWORD_FALLBACK, submitDispatch, type LoginMethod } from './api';
import { formErrorOf, type FormError } from './errors';
import { OtpForm } from './OtpForm';
import { PasswordForm } from './PasswordForm';

type AuthState = 'signed-in' | 'anonymous' | 'probing';

/**
 * Decide synchronously wherever the shell can. A `current-username` meta
 * means a template rendered this page and the controller already routed
 * signed-in visitors elsewhere, so an empty marker authoritatively means
 * anonymous and no probe is needed. A missing marker means a statically
 * deployed shell with no server behind the render - there the session
 * probe is the only source of truth.
 */
function initialAuthState(): AuthState {
  const marker = metaContent('current-username');
  if (marker === null) return 'probing';
  return marker ? 'signed-in' : 'anonymous';
}

/** Relative paths only: anything else would be an open redirect. */
function redirectTarget(raw: string | null): string {
  return raw && raw.startsWith('/') && !raw.startsWith('//') ? raw : '/';
}

/**
 * The sign-in page, rendered from what the discovery endpoint reports:
 * the active method expands into its form, every other method renders as
 * a button below the divider (switching forms client-side, or starting
 * the OAuth2 full-page navigation). Submissions go through the dispatch
 * entry point - see ./api.
 */
export default function SignIn() {
  const { t } = useTranslation();
  const [authState, setAuthState] = useState<AuthState>(initialAuthState);
  const [methods, setMethods] = useState<LoginMethod[]>([]);
  const [activeName, setActiveName] = useState<string | null>(null);
  // True while the OTP flow is in its code-entry stage: the method
  // switcher hides then, so nothing competes with the digit boxes.
  const [otpCodeStage, setOtpCodeStage] = useState(false);
  // The one error slot of the page: every form reports failures upward
  // and the alert block below renders them out of flow, so an appearing
  // message never grows the centred column (no upward shift).
  const [formError, setFormError] = useState<FormError | null>(null);

  // Static-shell fallback: ask the session whether it exists. `redirect:
  // manual` is essential - on the default web chain an anonymous request
  // answers 302 back to this very page, and a following fetch would misread
  // that bounce's 200 HTML as "signed in".
  useEffect(() => {
    if (authState !== 'probing') return;
    let cancelled = false;
    fetch('/api/user', {
      headers: { Accept: 'application/json' },
      credentials: 'same-origin',
      redirect: 'manual',
    })
      .then((res) => {
        if (!cancelled) setAuthState(res.ok ? 'signed-in' : 'anonymous');
      })
      .catch(() => {
        if (!cancelled) setAuthState('anonymous');
      });
    return () => {
      cancelled = true;
    };
  }, [authState]);

  // Already signed in: leave without keeping this page in history.
  useEffect(() => {
    if (authState !== 'signed-in') return;
    window.location.replace(
      redirectTarget(new URLSearchParams(window.location.search).get('redirect_url')),
    );
  }, [authState]);

  // Discovery: what this deployment offers, in presentation order.
  useEffect(() => {
    if (authState !== 'anonymous') return;
    let cancelled = false;
    fetchLoginMethods().then((list) => {
      if (!cancelled) setMethods(list);
    });
    return () => {
      cancelled = true;
    };
  }, [authState]);

  if (authState !== 'anonymous') {
    return null;
  }

  const offered = methods.length > 0 ? methods : [PASSWORD_FALLBACK];
  const active =
    offered.find((method) => method.name === activeName) ??
    offered.find((method) => method.primary) ??
    offered[0];
  const others = offered.filter((method) => method.name !== active.name);
  const siteName = metaContent('site-name') ?? t('app.title');

  return (
    <>
      <h1 className="mb-6 text-center text-2xl font-semibold tracking-tight">
        {siteName}
      </h1>

      <div className="relative flex w-80 max-w-full flex-col gap-4">
        {active.type === 'password' && <PasswordForm method={active} onError={setFormError} />}
        {active.type === 'otp' && (
          <OtpForm method={active} onStageChange={setOtpCodeStage} onError={setFormError} />
        )}
        {active.type === 'oauth2' && <OAuth2Button method={active} onError={setFormError} />}

        {!otpCodeStage && others.length > 0 && (
          <div className="mt-2 flex flex-col gap-2">
            {/* The rules stop where the pill corner curves visually begin:
                17px in per side on the 320px column, a 286px line. */}
            <div className="mx-[17px] flex items-center gap-3">
              <Separator className="flex-1" />
              <span className="text-xs uppercase tracking-wide text-muted-foreground">
                {t('signIn.or')}
              </span>
              <Separator className="flex-1" />
            </div>
            {others.map((method) =>
              method.type === 'oauth2' ? (
                <OAuth2Button key={method.name} method={method} onError={setFormError} />
              ) : (
                <Button
                  key={method.name}
                  type="button"
                  variant="outline"
                  className="w-full"
                  onClick={() => {
                    setActiveName(method.name);
                    setFormError(null);
                  }}
                >
                  {methodLabel(t, method)}
                </Button>
              ),
            )}
          </div>
        )}

        {formError && (
          // Out of flow: an appearing error never grows the centred
          // column, so nothing shifts upward.
          <ErrorAlert error={formError} className="absolute inset-x-0 top-full mt-4" />
        )}
      </div>
    </>
  );
}

function methodLabel(t: (key: string, options?: Record<string, unknown>) => string, method: LoginMethod): string {
  if (method.type === 'otp') {
    return method.attributes.channel === 'sms' ? t('signIn.usePhoneCode') : t('signIn.useEmailCode');
  }
  return t('signIn.usePassword');
}

/**
 * OAuth2 methods need no collected input, so their dispatch submission is
 * empty: the server resolves the registration and answers with the
 * authorization-initiation URL, which this button then navigates to as a
 * top-level navigation (the authorization round-trip itself can never be
 * an XHR). Dispatching instead of hard-coding /oauth2/authorization/…
 * keeps the registrationId a server-side concern.
 */
function OAuth2Button({
  method,
  onError,
}: {
  method: LoginMethod;
  onError: (error: FormError | null) => void;
}) {
  const { t } = useTranslation();
  const [starting, setStarting] = useState(false);
  const provider = method.attributes.provider ?? method.name;

  async function start() {
    if (starting) return;
    setStarting(true);
    onError(null);

    const result = await submitDispatch(method.name, {});
    if (result.ok) {
      window.location.assign(result.redirectUrl);
      return;
    }
    onError(formErrorOf(t, result));
    setStarting(false);
  }

  return (
    <Button
      type="button"
      variant="outline"
      className="w-full"
      disabled={starting}
      onClick={() => void start()}
    >
      {provider === 'google' && <GoogleIcon data-icon="inline-start" />}
      {t('signIn.signInWith', { provider })}
    </Button>
  );
}
