import { useTranslation } from 'react-i18next';

/**
 * Placeholder shell for the entrance paths that are not implemented yet
 * (`/create-account`, `/signout`). Renders the app title so a navigation
 * to those routes is visibly served rather than blank.
 */
export default function App() {
  const { t } = useTranslation();

  return (
    <main className="flex min-h-svh items-center justify-center bg-background text-foreground">
      <h1 className="text-2xl font-semibold">{t('app.title')}</h1>
    </main>
  );
}
