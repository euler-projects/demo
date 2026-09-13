import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { InputOTP, InputOTPGroup, InputOTPSlot } from '@/components/ui/input-otp';
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { issueOtpTicket, pendingRedirectUrl, submitDispatch, type LoginMethod } from './api';
import { formErrorOf, type FormError } from './errors';

/** Digit boxes the code entry renders; the code length this deployment issues. */
const CODE_LENGTH = 6;

/**
 * Dialing codes offered beside the SMS national-number field, in
 * presentation order. `region` is an i18n key under `dialingCode.regions`;
 * the trigger shows only the compact `code` while the popup pairs it with
 * the region name. Only +86 is offered for now; add entries here (plus the
 * matching `dialingCode.regions` labels) to widen the list.
 */
const DIALING_CODES = [
  { code: '+86', region: 'CN' },
] as const;

/** Dialing code preselected on the SMS recipient field. */
const DEFAULT_DIALING_CODE = '+86';

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
  const isSms = channel === 'sms';
  // SMS collects a national number beside a dialing-code selector; email
  // collects the whole address in a single field.
  const recipientLabel = isSms ? t('signIn.phone') : t('signIn.email');

  const [stage, setStage] = useState<'recipient' | 'code'>('recipient');
  const [recipient, setRecipient] = useState('');
  const [dialingCode, setDialingCode] = useState(DEFAULT_DIALING_CODE);
  const [ticket, setTicket] = useState<string | null>(null);
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  // Seconds left before the issuer accepts another ticket request; the
  // resend affordance counts down and stays disabled until it reaches
  // zero. Seeded from the issue response's retry_after.
  const [retryLeft, setRetryLeft] = useState(0);

  // The channel-addressable target the issuer receives and the "code sent
  // to" line echoes: for SMS the selected dialing code prefixed to the
  // national number (digits only) forms the E.164 string the backend
  // hashes into the phone identity's subject; for email it is the address.
  const target = isSms ? `${dialingCode}${recipient.replace(/\D/g, '')}` : recipient;

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

    const result = await issueOtpTicket(channel, target);
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
        {isSms ? (
          <div className="flex gap-2">
            {/* Dialing-code prefix: the trigger stays compact (just the
                code) while the popup pairs each code with its region. */}
            <Select
              items={DIALING_CODES.map(({ code }) => ({ value: code, label: code }))}
              value={dialingCode}
              onValueChange={(value) => setDialingCode(value as string)}
            >
              <SelectTrigger className="shrink-0" aria-label={t('dialingCode.label')}>
                <SelectValue />
              </SelectTrigger>
              <SelectContent align="start" alignItemWithTrigger={false} className="min-w-44">
                <SelectGroup>
                  {DIALING_CODES.map(({ code, region }) => (
                    <SelectItem key={code} value={code}>
                      <span>{code}</span>
                      <span className="text-muted-foreground">
                        {t(`dialingCode.regions.${region}`)}
                      </span>
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
            <Input
              id="otp-recipient"
              className="flex-1"
              type="tel"
              inputMode="numeric"
              autoComplete="tel-national"
              placeholder={recipientLabel}
              aria-label={recipientLabel}
              value={recipient}
              onChange={(e) => setRecipient(e.target.value)}
              disabled={busy}
            />
          </div>
        ) : (
          <Input
            id="otp-recipient"
            type="text"
            autoComplete="email"
            placeholder={recipientLabel}
            aria-label={recipientLabel}
            value={recipient}
            onChange={(e) => setRecipient(e.target.value)}
            disabled={busy}
          />
        )}

        <Button type="submit" className="w-full" disabled={busy}>
          {busy ? t('signIn.sending') : t('signIn.continue')}
        </Button>
      </form>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <p className="text-center text-xs text-muted-foreground">
        {t('signIn.codeSentTo', { recipient: target })}
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
