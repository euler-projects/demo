import { negotiateLanguages } from '@fluent/langneg';

/**
 * Locales the entrance app ships translations for.
 * Tags follow BCP 47 and are used as-is in resources file names,
 * i18next instance, localStorage and document.documentElement.lang.
 *
 * The array order drives the language-switcher listing; additional
 * locales are added here (plus a matching `locales/<tag>.json`) when
 * translated.
 */
export const SUPPORTED_LOCALES: readonly string[] = ['zh-Hans', 'en'];

/** Fallback locale when nothing in the requested list matches. */
export const DEFAULT_LOCALE = 'en';

/**
 * Pick the best matching supported locale for the given list of
 * requested locales (e.g. navigator.languages).
 *
 * Matching is delegated to @fluent/langneg, which carries a CLDR
 * likely-subtags subset and therefore handles cases such as
 * `zh-CN` / `zh-SG` -> `zh-Hans`, `en-GB` -> `en`, automatically.
 */
export function resolveLocale(
  requested: readonly string[] | string | null | undefined,
): string {
  const list = Array.isArray(requested) && requested.length > 0
    ? requested
    : [DEFAULT_LOCALE];
  const [best] = negotiateLanguages(list, SUPPORTED_LOCALES, {
    defaultLocale: DEFAULT_LOCALE,
    strategy: 'matching',
  });
  return best ?? DEFAULT_LOCALE;
}

/** Check whether a tag is one of the supported locales. */
export function isSupported(tag: string | null | undefined): tag is string {
  return typeof tag === 'string' && SUPPORTED_LOCALES.includes(tag);
}
