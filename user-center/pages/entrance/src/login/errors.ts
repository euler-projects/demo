/**
 * View model of a failed round-trip, plus the mapping from the wire
 * envelope to what the page's error callout shows.
 */
export interface FormError {
  /** Localized headline rendered as the alert title. */
  title: string;
  /**
   * Verbatim server message, rendered as the alert description only
   * when the envelope carried an explicit `message`.
   */
  description?: string;
}

/**
 * Map a failed dispatch/issue round-trip to a FormError.
 *
 * Credential failures arrive as the deliberately blurred
 * `authentication_failed` code with no dedicated variant; each form
 * knows how to word that failure for its own context, so the caller
 * passes that wording in as `blurredTitle`. Every other code is looked
 * up in the shared error table, and an explicit server message - absent
 * from the contract's {error, timestamp} envelope - becomes the
 * description.
 */
export function formErrorOf(
  t: (key: string, options?: Record<string, unknown>) => string,
  result: { error: string; message?: string },
  blurredTitle?: string,
): FormError {
  return {
    title: result.error === 'authentication_failed' && blurredTitle
      ? blurredTitle
      : t(`error.${result.error}`, { defaultValue: t('signIn.errorGeneric') }),
    description: result.message,
  };
}
