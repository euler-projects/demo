import { negotiateLanguages } from '@fluent/langneg';

/**
 * Locales the admin console ships translations for.
 * Tags follow BCP 47 and are used as-is in resources file names,
 * i18next instance, localStorage and document.documentElement.lang.
 */
export const SUPPORTED_LOCALES = ['zh-Hans', 'en'];

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
export function resolveLocale(requested) {
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
export function isSupported(tag) {
  return SUPPORTED_LOCALES.includes(tag);
}
