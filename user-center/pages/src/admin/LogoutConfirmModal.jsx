/* eslint-disable compat/compat */
import React, {useEffect, useState} from 'react';
import {Modal, Typography} from 'antd';
import {useTranslation} from 'react-i18next';

/**
 * Logout endpoint path. Sourced from the backend Thymeleaf shell via
 * <meta name="logout-processing-url" th:content="${logoutProcessingUrl}"/>,
 * which mirrors the same {@code logoutProcessingUrl} model attribute used
 * by the framework's logout.html. The literal fallback only applies
 * during pure-frontend dev (npm run dev) when Thymeleaf is not in play.
 */
const FALLBACK_LOGOUT_ACTION = '/logout';

const getLogoutAction = () => {
    if (typeof document === 'undefined') return FALLBACK_LOGOUT_ACTION;
    const meta = document.querySelector('meta[name="logout-processing-url"]');
    const value = meta?.getAttribute('content')?.trim();
    return value && value.length > 0 ? value : FALLBACK_LOGOUT_ACTION;
};

const LOGOUT_ACTION = getLogoutAction();

/**
 * Internal HTML id wiring the Modal's OK button (rendered outside the
 * <form> by antd) to the actual logout form via the standard HTML5
 * `button[form="..."]` attribute. This is what keeps the interaction
 * declarative end-to-end: clicking OK is browser-native form submit,
 * the 302 returned by Spring Security is followed by the browser, and
 * the SPA performs a natural full-page reload after logout.
 */
const LOGOUT_FORM_ID = 'euler-logout-form';

/**
 * A reusable confirmation modal that owns the entire logout protocol:
 * - Fetches a fresh CSRF token from {@code /_csrf} on open (tokens may
 *   rotate during a long session, so eager pre-fetch is avoided).
 * - Hosts a real <form action="/logout" method="post"> with the CSRF
 *   token as a hidden input, mirroring the contract of the backend
 *   logout.html template.
 * - Delegates submission to the browser; no fetch / no manual
 *   window.location redirects are performed in JS.
 *
 * The component is intentionally decoupled from any business container
 * (e.g. AdminLayout) so it can be reused across consoles.
 */
const LogoutConfirmModal = ({open, onCancel}) => {
    const {t} = useTranslation();
    const [csrf, setCsrf] = useState(null);
    const [loadError, setLoadError] = useState(false);

    useEffect(() => {
        if (!open) {
            return;
        }
        let cancelled = false;
        setCsrf(null);
        setLoadError(false);
        fetch('/_csrf', {headers: {Accept: 'application/json'}})
            .then((res) => {
                if (!res.ok) {
                    throw new Error(`CSRF fetch failed: ${res.status}`);
                }
                return res.json();
            })
            .then((token) => {
                if (!cancelled) setCsrf(token);
            })
            .catch(() => {
                if (!cancelled) setLoadError(true);
            });
        return () => {
            cancelled = true;
        };
    }, [open]);

    const ready = !!csrf && !loadError;

    return (
        <Modal
            open={open}
            title={t('header.logout')}
            okText={t('header.logout')}
            cancelText={t('common.cancel')}
            onCancel={onCancel}
            destroyOnHidden
            okButtonProps={{
                danger: true,
                htmlType: 'submit',
                form: LOGOUT_FORM_ID,
                // Delay the spinner so a fast /_csrf round-trip does not
                // briefly inject a LoadingOutlined icon and reflow the
                // button width (visible as a wide-to-narrow flicker on
                // open). Slow networks still get the loading affordance.
                loading: !ready && !loadError ? {delay: 200} : false,
                disabled: !ready,
            }}
        >
            <Typography.Paragraph>
                {t('header.logout_confirm')}
            </Typography.Paragraph>
            {loadError && (
                <Typography.Paragraph type="danger" style={{marginBottom: 0}}>
                    {t('header.logout_error')}
                </Typography.Paragraph>
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
        </Modal>
    );
};

export default LogoutConfirmModal;
