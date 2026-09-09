/**
 * Reads a runtime value the Thymeleaf shell injects as a
 * `<meta name=… content=…>` tag. The static `content` attribute doubles
 * as the development-time fallback while the SPA is served by `vite`
 * without the backend.
 */
export function metaContent(name: string): string | null {
  return document.querySelector<HTMLMetaElement>(`meta[name="${name}"]`)?.content ?? null;
}
