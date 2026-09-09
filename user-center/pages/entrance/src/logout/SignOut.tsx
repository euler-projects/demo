import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ErrorAlert } from '@/components/ErrorAlert';
import { Button } from '@/components/ui/button';
import { metaContent } from '@/lib/meta';
import { fetchCsrfToken, type CsrfToken } from '@/login/api';

/** Development fallback when the shell injects no logout URL. */
const FALLBACK_LOGOUT_PROCESSING_URL = '/doLogout';

/**
 * Sign-out confirmation page, mirroring the framework's reference
 * logout page: a heading that asks the question and a single danger
 * action that POSTs to the logout processing URL.
 *
 * The submission is a native browser form POST, not a fetch: Spring
 * Security answers with a 302 and the browser performs a full-page
 * navigation, which is what drops the SPA state along with the session.
 * The CSRF token is fetched on mount and travels as a hidden field, so
 * the action stays disabled until the token is in hand.
 */
export default function SignOut() {
  const { t } = useTranslation();
  const [csrf, setCsrf] = useState<CsrfToken | null>(null);
  const [loadError, setLoadError] = useState(false);

  const logoutProcessingUrl =
    metaContent('logout-processing-url') ?? FALLBACK_LOGOUT_PROCESSING_URL;

  useEffect(() => {
    let cancelled = false;
    fetchCsrfToken()
      .then((token) => {
        if (cancelled) return;
        if (token) setCsrf(token);
        else setLoadError(true);
      })
      .catch(() => {
        if (!cancelled) setLoadError(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
      <h1 className="mb-6 text-center text-2xl font-semibold tracking-tight">
        {t('signOut.confirm')}
      </h1>

      <form
        action={logoutProcessingUrl}
        method="post"
        className="flex w-80 max-w-full flex-col gap-4"
      >
        {csrf && <input type="hidden" name={csrf.parameterName} value={csrf.token} />}
        <Button
          type="submit"
          variant="destructive"
          className="w-full"
          disabled={!csrf || loadError}
        >
          {t('signOut.submit')}
        </Button>
      </form>

      {loadError && (
        <ErrorAlert error={{ title: t('signOut.error') }} className="mt-4 w-80 max-w-full" />
      )}
    </>
  );
}
