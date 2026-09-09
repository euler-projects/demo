import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import { SUPPORTED_LOCALES, DEFAULT_LOCALE, resolveLocale, isSupported } from './match';
import enResources from './locales/en.json';
import zhHansResources from './locales/zh-Hans.json';

const STORAGE_KEY = 'entrance.locale';

function readStoredLocale(): string | null {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    return isSupported(stored) ? stored : null;
  } catch {
    return null;
  }
}

function pickInitial(): string {
  const stored = readStoredLocale();
  if (stored) return stored;
  const requested = typeof navigator !== 'undefined'
    ? (navigator.languages ?? [navigator.language].filter(Boolean))
    : [];
  return resolveLocale(requested);
}

const initialLocale = pickInitial();

i18n
  .use(initReactI18next)
  .init({
    resources: {
      'en': { translation: enResources },
      'zh-Hans': { translation: zhHansResources },
    },
    lng: initialLocale,
    fallbackLng: DEFAULT_LOCALE,
    supportedLngs: [...SUPPORTED_LOCALES],
    load: 'currentOnly',
    interpolation: { escapeValue: false },
    returnNull: false,
  });

if (typeof document !== 'undefined') {
  document.documentElement.lang = initialLocale;
}

/**
 * Switch the active locale, persist the choice and sync the
 * html lang attribute. Unsupported tags are ignored.
 */
export function setLocale(tag: string): void {
  if (!isSupported(tag)) return;
  void i18n.changeLanguage(tag);
  try {
    window.localStorage.setItem(STORAGE_KEY, tag);
  } catch {
    /* ignore quota / privacy errors */
  }
  if (typeof document !== 'undefined') {
    document.documentElement.lang = tag;
  }
}

export { SUPPORTED_LOCALES, DEFAULT_LOCALE };
export default i18n;
