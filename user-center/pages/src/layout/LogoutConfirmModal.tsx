import {useEffect, useState} from 'react';
import type {ReactElement} from 'react';
import {Loader2} from 'lucide-react';
import {useTranslation} from 'react-i18next';

import {
    AlertDialog,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {buttonVariants} from '@/components/ui/button';

/**
 * Logout endpoint path. Sourced from the backend Thymeleaf shell via
 * <meta name="logout-processing-url" th:content="${logoutProcessingUrl}"/>,
 * which mirrors the same {@code logoutProcessingUrl} model attribute used
 * by the framework's logout.html. The literal fallback only applies
 * during pure-frontend dev (npm run dev) when Thymeleaf is not in play.
 */
const FALLBACK_LOGOUT_ACTION = '/logout';

const getLogoutAction = (): string => {
    if (typeof document === 'undefined') return FALLBACK_LOGOUT_ACTION;
    const meta = document.querySelector('meta[name="logout-processing-url"]');
    const value = meta?.getAttribute('content')?.trim();
    return value && value.length > 0 ? value : FALLBACK_LOGOUT_ACTION;
};

const LOGOUT_ACTION = getLogoutAction();

/**
 * Internal HTML id wiring the dialog's submit button to the actual
 * logout form via the standard HTML5 `button[form="..."]` attribute.
 * This is what keeps the interaction declarative end-to-end: clicking
 * the button is a browser-native form submit, the 302 returned by
 * Spring Security is followed by the browser, and the SPA performs a
 * natural full-page reload after logout.
 */
const LOGOUT_FORM_ID = 'euler-logout-form';

/** CSRF token shape returned by the `/_csrf` endpoint. */
interface CsrfToken {
    parameterName: string;
    token: string;
}

interface LogoutConfirmModalProps {
    open: boolean;
    onCancel: () => void;
}

/**
 * Confirmation dialog owning the entire logout protocol of this console:
 * - Fetches a fresh CSRF token from `/_csrf` on open (tokens may rotate
 *   during a long session, so eager pre-fetch is avoided).
 * - Hosts a real <form action="/logout" method="post"> with the CSRF
 *   token as a hidden input, mirroring the contract of the backend
 *   logout.html template.
 * - Delegates submission to the browser; no fetch / no manual
 *   window.location redirects are performed in JS.
 *
 * Built on the shadcn AlertDialog so the console chrome carries no antd
 * component (matching ConsoleLayout's pure-shadcn boundary). The confirm
 * affordance is a native <button type="submit" form=…> styled with
 * `buttonVariants` rather than the shadcn <Button>: Base UI's Button
 * pins `type="button"` (its internal props are merged last), which would
 * never submit the form, so the native element is required here.
 */
const LogoutConfirmModal = ({open, onCancel}: LogoutConfirmModalProps): ReactElement => {
    const {t} = useTranslation();
    const [csrf, setCsrf] = useState<CsrfToken | null>(null);
    const [loadError, setLoadError] = useState(false);
    // Delay the spinner so a fast /_csrf round-trip does not briefly
    // inject a Loader2 icon and reflow the button width (visible as a
    // wide-to-narrow flicker on open). Slow networks still get the
    // loading affordance.
    const [spinnerArmed, setSpinnerArmed] = useState(false);

    useEffect(() => {
        if (!open) return undefined;
        let cancelled = false;
        setCsrf(null);
        setLoadError(false);
        setSpinnerArmed(false);
        const spinnerTimer = window.setTimeout(() => {
            if (!cancelled) setSpinnerArmed(true);
        }, 200);
        fetch('/_csrf', {headers: {Accept: 'application/json'}})
            .then((res) => {
                if (!res.ok) {
                    throw new Error(`CSRF fetch failed: ${res.status}`);
                }
                return res.json() as Promise<CsrfToken>;
            })
            .then((token) => {
                if (!cancelled) setCsrf(token);
            })
            .catch(() => {
                if (!cancelled) setLoadError(true);
            });
        return () => {
            cancelled = true;
            window.clearTimeout(spinnerTimer);
        };
    }, [open]);

    const ready = csrf != null && !loadError;
    const showSpinner = !ready && !loadError && spinnerArmed;

    return (
        <AlertDialog
            open={open}
            onOpenChange={(next) => {
                if (!next) onCancel();
            }}
        >
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>{t('header.logout')}</AlertDialogTitle>
                    <AlertDialogDescription>
                        {t('header.logout_confirm')}
                    </AlertDialogDescription>
                </AlertDialogHeader>
                {loadError && (
                    <p className="text-sm text-destructive">{t('header.logout_error')}</p>
                )}
                {/*
                  Real HTML form. The browser handles POST submission and
                  follows the 302 redirect produced by Spring Security's
                  LogoutSuccessHandler, which clears the SPA state via a
                  full page navigation.
                */}
                <form id={LOGOUT_FORM_ID} action={LOGOUT_ACTION} method="post">
                    {csrf && (
                        <input
                            type="hidden"
                            name={csrf.parameterName}
                            value={csrf.token}
                        />
                    )}
                </form>
                <AlertDialogFooter>
                    <AlertDialogCancel>{t('common.cancel')}</AlertDialogCancel>
                    <button
                        type="submit"
                        form={LOGOUT_FORM_ID}
                        disabled={!ready}
                        className={buttonVariants({variant: 'destructive'})}
                    >
                        {showSpinner && <Loader2 className="animate-spin" data-icon="inline-start"/>}
                        {t('header.logout')}
                    </button>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

export default LogoutConfirmModal;
