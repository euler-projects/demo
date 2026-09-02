import i18n from './index';
// Measurement primitives live in the shared table utilities; reusing
// them keeps one canvas per app and one definition of cell padding.
import { CELL_PADDING, measureLabelWidth } from '../admin/_shared/tableLayout';

/**
 * Locale-aware date/time formatting for the whole console.
 *
 * Every display follows the active i18n locale's conventions via
 * `Intl.DateTimeFormat` (en-US today; when more locales ship, formats
 * switch automatically with the language — no per-page work).
 *
 * Three distinct semantics — pick the right one:
 *   - fmtDateTime:     audit instants (createdAt / syncedAt /
 *                      receivedAt …), rendered in the viewer's local
 *                      timezone.
 *   - fmtDate:         date-only view of an instant; still local
 *                      timezone so the calendar day matches what the
 *                      viewer experienced.
 *   - fmtCalendarDate: Apple's date-only boundaries (price window
 *                      start/end, price entry dates). These are
 *                      calendar days stored as UTC midnight, so they
 *                      are formatted in UTC to stay exact in any
 *                      viewer timezone.
 */

function locale(): string {
  return i18n.language || 'en';
}

export function fmtDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Intl.DateTimeFormat(locale(), {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(iso));
}

export function fmtDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Intl.DateTimeFormat(locale(), {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(iso));
}

export function fmtCalendarDate(iso: string | null | undefined): string {
  if (!iso) return '—';
  return new Intl.DateTimeFormat(locale(), {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(iso));
}

/**
 * Table column width helpers for datetime columns under
 * `tableLayout="fixed"` (antd has no native fit-content columns).
 * The formatted output is locale-deterministic, so we measure the
 * widest rendering across all twelve months + edge times for the
 * ACTIVE locale and add cell padding — the column hugs its content
 * exactly and re-sizes itself whenever the locale changes.
 */
const SAMPLE_ISOS = Array.from(
  { length: 12 },
  (_, m) => `2026-${String(m + 1).padStart(2, '0')}-28T23:59:59.000Z`,
);

function widestText(fmt: (iso: string) => string): number {
  return Math.max(...SAMPLE_ISOS.map((iso) => measureLabelWidth(fmt(iso))));
}

/** Width that exactly fits the widest fmtDateTime output. */
export function dateTimeColumnWidth(): number {
  return Math.ceil(widestText((iso) => fmtDateTime(iso))) + CELL_PADDING;
}

/** Width that exactly fits the widest fmtDate output. */
export function dateColumnWidth(): number {
  return Math.ceil(widestText((iso) => fmtDate(iso))) + CELL_PADDING;
}
