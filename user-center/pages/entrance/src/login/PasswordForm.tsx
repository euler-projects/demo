import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { pendingRedirectUrl, submitDispatch, type LoginMethod } from './api';
import { formErrorOf, type FormError } from './errors';

/**
 * Username + password form. Submits through the dispatch entry point;
 * the server replays the complete submission to the formLogin
 * processing endpoint and the JSON envelope arrives back here.
 *
 * Failures are reported upward through `onError`: the page renders every
 * form's message in one alert block below the form area, so an appearing
 * message never changes this form's height (and never shifts the
 * vertically centred column).
 */
export function PasswordForm({
  method,
  onError,
}: {
  method: LoginMethod;
  onError: (error: FormError | null) => void;
}) {
  const { t } = useTranslation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    onError(null);

    const fields: Record<string, string> = { username, password };
    const redirect = pendingRedirectUrl();
    if (redirect) {
      fields.redirect_url = redirect;
    }

    const result = await submitDispatch(method.name, fields);
    if (result.ok) {
      window.location.assign(result.redirectUrl);
      return;
    }
    // The server blurs credential failures into the generic 401; this form
    // words them for its own context.
    onError(formErrorOf(t, result, t('signIn.badCredentials')));
    setSubmitting(false);
  }

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-4">
      <Input
        id="username"
        name="username"
        type="text"
        autoComplete="username"
        placeholder={t('signIn.username')}
        aria-label={t('signIn.username')}
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        disabled={submitting}
      />
      <Input
        id="password"
        name="password"
        type="password"
        autoComplete="current-password"
        placeholder={t('signIn.password')}
        aria-label={t('signIn.password')}
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        disabled={submitting}
      />

      <Button type="submit" className="w-full" disabled={submitting}>
        {submitting ? t('signIn.signingIn') : t('signIn.submit')}
      </Button>
    </form>
  );
}
