import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { InputOTP, InputOTPGroup, InputOTPSlot } from '@/components/ui/input-otp';
import { issueOtpTicket, pendingRedirectUrl, submitDispatch, type LoginMethod } from './api';
import { formErrorOf, type FormError } from './errors';

/** Digit boxes the code entry renders; the code length this deployment issues. */
const CODE_LENGTH = 6;

/**
 * Two-step OTP form. Step one asks the issue endpoint to deliver a code
 * (anonymous, CSRF-exempt, not a login submission); step two collects the
 * code in per-digit boxes and submits ticket + code through the dispatch
 * entry point automatically the moment the last box is filled - no
 * submit button, matching the prevailing consumer interaction.
 *
 * `onStageChange` lets the page react to the stage: while the visitor is
 * mid-code the page hides the method switcher so nothing competes with
 * the boxes. Failures travel upward through `onError` like every other
 * form on the page, so the shared alert block below the form area shows
 * them without shifting the centred column.
 */
export function OtpForm({
  method,
  onStageChange,
  onError,
}: {
  method: LoginMethod;
  onStageChange?: (inCodeStage: boolean) => void;
  onError: (error: FormError | null) => void;
}) {
  const { t } = useTranslation();
  const channel = method.attributes.channel ?? 'email';
  // One derivation of the recipient field's channel-dependent traits.
  const recipientField = channel === 'email'
    ? { label: t('signIn.email'), autoComplete: 'email' }
    : { label: t('signIn.phone'), autoComplete: 'tel' };

  const [stage, setStage] = useState<'recipient' | 'code'>('recipient');
  const [recipient, setRecipient] = useState('');
  const [ticket, setTicket] = useState<string | null>(null);
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  // Seconds left before the issuer accepts another ticket request; the
  // resend affordance counts down and stays disabled until it reaches
  // zero. Seeded from the issue response's retry_after.
  const [retryLeft, setRetryLeft] = useState(0);

  useEffect(() => {
    if (retryLeft <= 0) return;
    const timer = window.setTimeout(() => setRetryLeft((seconds) => seconds - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [retryLeft]);

  function enterStage(next: 'recipient' | 'code') {
    setStage(next);
    onStageChange?.(next === 'code');
  }

  async function issue() {
    if (busy) return;
    setBusy(true);
    onError(null);

    const result = await issueOtpTicket(channel, recipient);
    if (result.ok) {
      setTicket(result.otpTicket);
      setCode('');
      setRetryLeft(result.retryAfter);
      enterStage('code');
    } else {
      onError(formErrorOf(t, result));
    }
    setBusy(false);
  }

  /** Auto-submitted by the boxes when the code is complete. */
  async function verify(submitted: string) {
    if (busy || !ticket) return;
    setBusy(true);
    onError(null);

    const fields: Record<string, string> = { otp_ticket: ticket, otp: submitted };
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
    // words them for its own context. A rejected code empties the boxes;
    // the library keeps its hidden input focused, so re-entry starts at
    // the first box with no click.
    onError(formErrorOf(t, result, t('signIn.badOtp')));
    setCode('');
    setBusy(false);
  }

  if (stage === 'recipient') {
    return (
      <form
        onSubmit={(event) => {
          event.preventDefault();
          void issue();
        }}
        className="flex flex-col gap-4"
      >
        <Input
          id="otp-recipient"
          type="text"
          autoComplete={recipientField.autoComplete}
          placeholder={recipientField.label}
          aria-label={recipientField.label}
          value={recipient}
          onChange={(e) => setRecipient(e.target.value)}
          disabled={busy}
        />

        <Button type="submit" className="w-full" disabled={busy}>
          {busy ? t('signIn.sending') : t('signIn.continue')}
        </Button>
      </form>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <p className="text-center text-xs text-muted-foreground">
        {t('signIn.codeSentTo', { recipient })}
      </p>

      <InputOTP
        maxLength={CODE_LENGTH}
        value={code}
        onChange={(next) => {
          setCode(next);
          if (next.length === CODE_LENGTH) {
            void verify(next);
          }
        }}
        disabled={busy}
        containerClassName="w-full justify-center"
      >
        <InputOTPGroup>
          {Array.from({ length: CODE_LENGTH }, (_, index) => (
            <InputOTPSlot key={index} index={index} />
          ))}
        </InputOTPGroup>
      </InputOTP>

      <div className="flex justify-between">
        <Button
          type="button"
          variant="link"
          size="sm"
          disabled={busy}
          onClick={() => {
            enterStage('recipient');
            onError(null);
          }}
        >
          {t('signIn.back')}
        </Button>
        <Button
          type="button"
          variant="link"
          size="sm"
          disabled={busy || retryLeft > 0}
          onClick={() => void issue()}
        >
          {retryLeft > 0 ? t('signIn.resendIn', { seconds: retryLeft }) : t('signIn.resend')}
        </Button>
      </div>
    </div>
  );
}
