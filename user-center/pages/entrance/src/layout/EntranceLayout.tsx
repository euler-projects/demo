import { useTranslation } from 'react-i18next';
import { Outlet } from 'react-router';
import { metaContent } from '@/lib/meta';

/**
 * Shared chrome for every entrance page: a viewport-centred content
 * column (bottom-padded to clear the footer band) plus the site footer
 * pinned to the bottom of the viewport, mirroring the framework's
 * reference pages. Each page renders only its own content — heading,
 * form, messages — through the <Outlet />.
 */
export default function EntranceLayout() {
  const { t } = useTranslation();
  const siteName = metaContent('site-name') ?? t('app.title');
  const copyrightHolder = metaContent('copyright-holder') ?? 'Euler Project';
  const copyrightWebsite =
    metaContent('copyright-holder-website') ?? 'https://www.eulerproject.io';

  return (
    <div className="relative min-h-svh bg-background text-foreground">
      <div className="flex min-h-svh flex-col items-center justify-center px-4 pt-8 pb-24">
        <Outlet />
      </div>

      {/* Footer pinned to the viewport bottom: site name linking home +
          copyright line. Links stay plain and underline on hover. */}
      <footer className="fixed inset-x-0 bottom-0 z-10 px-4 py-3 text-center text-xs text-muted-foreground">
        <span>
          <a href="/" className="no-underline hover:underline hover:text-foreground">
            {siteName}
          </a>
        </span>
        <span className="mx-2" aria-hidden="true">
          ·
        </span>
        <span>
          © {new Date().getFullYear()}{' '}
          <a
            href={copyrightWebsite}
            target="_blank"
            rel="noreferrer"
            className="no-underline hover:underline hover:text-foreground"
          >
            {copyrightHolder}
          </a>
        </span>
      </footer>
    </div>
  );
}
