/**
 * Shared table layout utilities for the admin console.
 *
 * - RowActions: text-link row actions that auto-collapse into a kebab
 *   menu when they overflow the (frozen) action column width.
 * - OverflowTags: tags that auto-collapse into a +N popover badge.
 * - TwoLineCell: two-line identity cell (e.g. ID / Name).
 * - EllipsisText / MiddleEllipsisText: the shared cell-text
 *   truncation building blocks (end vs middle).
 */
import { Fragment, useEffect, useMemo, useRef, useState } from 'react';
// Cell-scoped CSS travels with this module (affordance icon hide /
// row-hover reveal), keeping index.css neutral.
import './tableLayout.css';
import type { MouseEvent as ReactMouseEvent } from 'react';
import type { CSSProperties } from 'react';
import type { ReactElement, ReactNode } from 'react';
import type { InputProps } from 'antd/es/input';
import type { SelectProps } from 'antd/es/select';
import {
  App as AntdApp,
  Button,
  Divider,
  Dropdown,
  Input,
  Modal,
  Popover,
  Select,
  Space,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import {
  CheckOutlined,
  CloseOutlined,
  CopyOutlined,
  EditOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { Link } from 'react-router';
import { useTranslation } from 'react-i18next';
import { extractApiError } from './api';

// Width budget for a row-action column. Frozen on the right so this is
// authoritative when distributing actions between the visible row and
// the overflow menu.
export const ACTION_COLUMN_WIDTH = 200;

// antd Table cell horizontal padding (16 * 2).
export const CELL_PADDING = 32;
// <Space size="small"> between sibling action links.
const ACTION_SPACING = 8;
// 1px vertical divider between adjacent action links (antd's official
// action-column separator; its own margin is zeroed — the Space gap
// supplies the spacing on both sides), plus one extra gap it creates
// as an extra Space item: gap + line + gap instead of a single gap.
const DIVIDER_FOOTPRINT = 1 + ACTION_SPACING;
// Icon-only text button used as the overflow trigger.
const MORE_BUTTON = 24;
// antd Tag horizontal padding plus border.
const TAG_INNER_PADDING = 16;
// Gap between adjacent tags (also drives the +N badge spacing).
const TAG_GAP = 6;

const _measureCanvas =
  typeof document !== 'undefined' ? document.createElement('canvas') : null;
const _measureCtx = _measureCanvas?.getContext('2d');
const FONT_STACK =
  '-apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, Avenir, Helvetica, Arial, sans-serif';
const MEASURE_FONT = `14px ${FONT_STACK}`;

function measureText(text: string, font: string): number {
  if (!_measureCtx) return (text || '').length * 14;
  _measureCtx.font = font;
  return _measureCtx.measureText(text || '').width;
}

/**
 * Cheap, DOM-free text width measurement that shares a single canvas
 * across the application. Falls back to a character-count heuristic
 * when running outside a browser (SSR / tests).
 */
export function measureLabelWidth(text: string): number {
  return measureText(text, MEASURE_FONT);
}

const MONOSPACE_FONT =
  '14px "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace';

/**
 * Width of text rendered in the monospace font used by EditableCell —
 * for sizing columns that must fit full identifiers (e.g. a 36-char
 * UUID plus its copy affordance).
 */
export function monospaceTextWidth(text: string): number {
  return measureText(text, MONOSPACE_FONT);
}

interface ActionBlueprint {
  key: string;
  label: string;
}

export interface RowAction extends ActionBlueprint {
  onClick: () => void;
  danger?: boolean;
  disabled?: boolean;
}

export interface OverflowTagItem {
  key: string;
  label: string;
  color?: string;
}

/**
 * Compute the row-action column width that exactly fits what will be
 * rendered, capped at `maxWidth`. Delegates packing to splitActions so
 * the column hugs the visible cell whether the full set fits inline or
 * some actions collapse into the kebab.
 *
 * Pass the widest stable label per slot for toggle-style actions to
 * avoid horizontal jitter when row state changes.
 */
export function computeActionsColumnWidth(
  labels: string[],
  maxWidth = ACTION_COLUMN_WIDTH,
): number {
  if (!labels || labels.length === 0) return CELL_PADDING;
  const blueprint = labels.map((label, i) => ({ key: String(i), label }));
  const { visible, overflow } = splitActions(blueprint, maxWidth);
  const visibleWidth = visible.reduce(
    (sum, a, i) =>
      sum + measureLabelWidth(a.label) + (i > 0 ? ACTION_SPACING + DIVIDER_FOOTPRINT : 0),
    0,
  );
  const moreWidth = overflow.length > 0 ? ACTION_SPACING + MORE_BUTTON : 0;
  return Math.min(maxWidth, Math.ceil(visibleWidth + moreWidth + CELL_PADDING));
}

/**
 * Distribute actions between the visible row and the overflow menu
 * given a fixed column width. Always keeps at least one action visible
 * so a primary affordance is preserved even in narrow layouts.
 *
 * Actions are consumed in declared order, so the array itself encodes
 * priority (most important first).
 */
export function splitActions(
  actions: ActionBlueprint[],
  columnWidth = ACTION_COLUMN_WIDTH,
): { visible: ActionBlueprint[]; overflow: ActionBlueprint[] } {
  const available = Math.max(0, columnWidth - CELL_PADDING);
  const total = actions.reduce(
    (sum, a, i) =>
      sum + measureLabelWidth(a.label) + (i > 0 ? ACTION_SPACING + DIVIDER_FOOTPRINT : 0),
    0,
  );
  if (total <= available) {
    return { visible: actions, overflow: [] };
  }
  const reservedForMore = MORE_BUTTON + ACTION_SPACING;
  const visibleBudget = available - reservedForMore;
  const visible: ActionBlueprint[] = [];
  let used = 0;
  for (const action of actions) {
    const w =
      measureLabelWidth(action.label) +
      (visible.length > 0 ? ACTION_SPACING + DIVIDER_FOOTPRINT : 0);
    if (used + w <= visibleBudget) {
      visible.push(action);
      used += w;
    } else {
      break;
    }
  }
  if (visible.length === 0) visible.push(actions[0]!);
  return { visible, overflow: actions.slice(visible.length) };
}

/**
 * Decide how many tags fit before collapsing the rest into a +N badge.
 * Mirrors splitActions but accounts for the fixed Tag chrome.
 */
export function splitTagsByWidth(
  items: OverflowTagItem[],
  columnWidth: number,
): number {
  const available = Math.max(0, columnWidth - CELL_PADDING);
  let used = 0;
  let visibleCount = 0;
  for (let i = 0; i < items.length; i++) {
    const w =
      measureLabelWidth(items[i]!.label) +
      TAG_INNER_PADDING +
      (i > 0 ? TAG_GAP : 0);
    if (used + w <= available) {
      used += w;
      visibleCount++;
    } else {
      break;
    }
  }
  if (visibleCount === items.length) return visibleCount;
  const plusLabel = `+${items.length - visibleCount}`;
  const plusW =
    measureLabelWidth(plusLabel) +
    TAG_INNER_PADDING +
    (visibleCount > 0 ? TAG_GAP : 0);
  while (visibleCount > 0 && used + plusW > available) {
    visibleCount--;
    used -=
      measureLabelWidth(items[visibleCount]!.label) +
      TAG_INNER_PADDING +
      (visibleCount > 0 ? TAG_GAP : 0);
  }
  if (visibleCount === 0 && items.length > 0) visibleCount = 1;
  return visibleCount;
}

/**
 * Renders a sequence of Tags that auto-collapse into a +N badge with a
 * hover popover. Convention follows GitHub labels / Linear status: a
 * numeric overflow badge rather than the action-column kebab icon.
 */
export function OverflowTags({
  items,
  columnWidth,
}: {
  items: OverflowTagItem[];
  columnWidth: number;
}): ReactElement | null {
  if (!items || items.length === 0) return null;
  const visibleCount = splitTagsByWidth(items, columnWidth);
  const visible = items.slice(0, visibleCount);
  const overflow = items.slice(visibleCount);
  return (
    <Space size={TAG_GAP} wrap={false} style={{ display: 'inline-flex' }}>
      {visible.map((item) => (
        <Tag key={item.key} color={item.color} style={{ margin: 0 }}>
          {item.label}
        </Tag>
      ))}
      {overflow.length > 0 && (
        <Popover
          placement="top"
          content={
            <Space size={[TAG_GAP, TAG_GAP]} wrap style={{ maxWidth: 240 }}>
              {overflow.map((item) => (
                <Tag key={item.key} color={item.color} style={{ margin: 0 }}>
                  {item.label}
                </Tag>
              ))}
            </Space>
          }
        >
          <Tag style={{ margin: 0, cursor: 'pointer' }}>
            +{overflow.length}
          </Tag>
        </Popover>
      )}
    </Space>
  );
}

/**
 * Renders a row-action cell that adapts to the available width: actions
 * that fit are displayed inline as text links, the rest collapse behind
 * a kebab menu.
 *
 * @param actions       full action list including handlers, in priority
 *                      order (most important first)
 * @param columnWidth   width budget for layout calculation
 * @param moreLabel     accessible label for the kebab button
 * @param measureLabels optional map keyed by action.key, providing a
 *                      stable label string used only for layout
 *                      measurement; useful when an action's actual
 *                      label changes per row to avoid layout jitter
 */
export function RowActions({
  actions,
  columnWidth = ACTION_COLUMN_WIDTH,
  moreLabel,
  measureLabels,
}: {
  actions: RowAction[];
  columnWidth?: number;
  moreLabel?: string;
  measureLabels?: Record<string, string>;
}): ReactElement {
  const { t } = useTranslation();
  const layout = useMemo(() => {
    const blueprint = actions.map((a) => ({
      key: a.key,
      label:
        measureLabels && measureLabels[a.key] != null
          ? measureLabels[a.key]
          : a.label,
    }));
    return splitActions(blueprint, columnWidth);
  }, [actions, columnWidth, measureLabels]);

  const byKey = useMemo(
    () => Object.fromEntries(actions.map((a) => [a.key, a])),
    [actions],
  );

  return (
    <Space size="small" style={{ whiteSpace: 'nowrap' }}>
      {layout.visible.map((slot, i) => {
        const action = byKey[slot.key];
        if (!action) return null;
        return (
          // antd's official action-column separator: a vertical
          // Divider as its OWN Space item — inside the block-level
          // .ant-space-item wrapper its vertical-align: middle (and
          // the built-in -0.06em optical nudge) work against the
          // line box exactly as designed. Its own margin is zeroed
          // so the Space gap is the sole spacing
          // (DIVIDER_FOOTPRINT assumes it); labels containing
          // spaces stay unambiguous.
          <Fragment key={action.key}>
            {i > 0 && <Divider vertical style={{ marginInline: 0 }} />}
            <Typography.Link
              type={action.danger ? 'danger' : undefined}
              disabled={action.disabled}
              style={{ whiteSpace: 'nowrap' }}
              onClick={action.onClick}
            >
              {action.label}
            </Typography.Link>
          </Fragment>
        );
      })}
      {layout.overflow.length > 0 && (
        <Dropdown
          menu={{
            items: layout.overflow
              .map((slot) => byKey[slot.key])
              .filter(Boolean)
              .map((action) => ({
                key: action!.key,
                label: action!.label,
                danger: action!.danger,
                disabled: action!.disabled,
                onClick: action!.onClick,
              })),
          }}
          trigger={['click']}
          placement="bottomRight"
        >
          <Button
            type="text"
            size="small"
            icon={<MoreOutlined />}
            aria-label={moreLabel ?? t('common.more')}
            onClick={(e) => e.preventDefault()}
          />
        </Dropdown>
      )}
    </Space>
  );
}

/**
 * Two-line identity cell (e.g. ID over Name): a primary line plus a
 * muted secondary line (smaller font, 45% black), tight leading so
 * rows stay compact. TwoLineCell OWNS the primary/secondary hierarchy
 * styling; when a line needs copy / edit affordances, pass an
 * EditableCell as primary / secondary — it inherits the hierarchy's
 * font size and color and contributes only its affordances.
 */
export function TwoLineCell({
  primary,
  secondary,
}: {
  primary: ReactNode;
  secondary?: ReactNode;
}): ReactElement {
  return (
    <div style={{ lineHeight: 1.4 }}>
      <div>{primary}</div>
      {secondary !== undefined && secondary !== null && (
        <div style={{ fontSize: 12, color: 'rgba(0, 0, 0, 0.45)' }}>
          {secondary}
        </div>
      )}
    </div>
  );
}

/**
 * Single-line text with MIDDLE (center) ellipsis: keeps both ends of
 * a long identifier visible and cuts out the middle, e.g.
 * `com.xiaoyuxinchuang…premium.1m.auto_renew`. Pure CSS can only
 * truncate at one end, so the displayed string is computed by canvas
 * measurement against the available width (a ResizeObserver keeps it
 * correct when the column resizes). The full value stays in the title
 * tooltip.
 *
 * Width source: by default the element's OWN box — which is only safe
 * when that box is content-independent (e.g. a block filling a fixed
 * table column). When the surrounding layout shrink-to-fits content,
 * pass `maxWidth` measured by the parent from a content-independent
 * box; the component then never observes itself and the rendered
 * string cannot feed back into its own measurement (flicker loop).
 */
export function MiddleEllipsisText({
  text,
  maxWidth,
  mono = true,
}: {
  text: string;
  maxWidth?: number;
  /** Monospace (identifiers) vs the table's proportional text font. */
  mono?: boolean;
}): ReactElement {
  const ref = useRef<HTMLDivElement | null>(null);
  const [display, setDisplay] = useState(text);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const compute = (width: number): void => {
      if (width <= 0) return;
      const font = getComputedStyle(el).font;
      // +1px tolerance: clientWidth FLOORS the fractional content
      // width while measureText returns fractions — without the
      // tolerance an exact-fit text reads as "does not fit" and gets
      // needlessly truncated.
      if (measureText(text, font) <= width + 1) {
        setDisplay(text);
        return;
      }
      const ellipsis = '…';
      // Binary-search the largest kept-char budget whose
      // `head + … + tail` rendering still fits the column.
      let lo = 2;
      let hi = text.length;
      let best = 2;
      while (lo <= hi) {
        const mid = Math.floor((lo + hi) / 2);
        const head = Math.ceil(mid / 2);
        const tail = mid - head;
        const candidate =
          text.slice(0, head) +
          ellipsis +
          (tail > 0 ? text.slice(text.length - tail) : '');
        if (measureText(candidate, font) <= width) {
          best = mid;
          lo = mid + 1;
        } else {
          hi = mid - 1;
        }
      }
      const head = Math.ceil(best / 2);
      const tail = best - head;
      setDisplay(
        text.slice(0, head) +
          ellipsis +
          (tail > 0 ? text.slice(text.length - tail) : ''),
      );
    };
    if (maxWidth !== undefined) {
      // Parent dictates the budget (and re-renders us on resize).
      compute(maxWidth);
      return;
    }
    const self = (): void => compute(el.clientWidth);
    self();
    const ro = new ResizeObserver(self);
    ro.observe(el);
    return () => ro.disconnect();
  }, [text, maxWidth]);

  return (
    <div
      ref={ref}
      title={text}
      style={{
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        fontFamily: mono ? 'monospace' : undefined,
      }}
    >
      {display}
    </div>
  );
}

/**
 * Single-line text that END-truncates (CSS ellipsis) when it
 * overflows, optionally followed by non-shrinkable trailing content
 * (a badge, an icon, …): the text shrinks, the trailer never does.
 *
 * This is THE shared truncation building block of the cells below —
 * used by both the default text form and custom `display` shapes so
 * call sites never hand-write truncation CSS. Pure CSS, hence no
 * measurement feedback loops. MIDDLE truncation (identifiers that
 * must keep both ends) stays the dedicated MiddleEllipsisText.
 */
export function EllipsisText({
  text,
  mono = false,
  trailing,
}: {
  text: string;
  /** Monospace (identifiers) vs the table's proportional text font. */
  mono?: boolean;
  /** Content that never shrinks (8px gap supplied by the wrapper). */
  trailing?: ReactNode;
}): ReactElement {
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        maxWidth: '100%',
        minWidth: 0,
      }}
    >
      <span
        style={{
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          minWidth: 0,
          fontFamily: mono ? 'monospace' : undefined,
        }}
      >
        {text}
      </span>
      {trailing !== undefined && trailing !== null && (
        <span
          style={{
            flexShrink: 0,
            marginInlineStart: 8,
            display: 'inline-flex',
            alignItems: 'center',
          }}
        >
          {trailing}
        </span>
      )}
    </span>
  );
}

/**
 * Clipboard write with a hidden-textarea fallback for non-secure
 * contexts where navigator.clipboard is unavailable.
 */
async function copyText(value: string): Promise<void> {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(value);
    return;
  }
  const ta = document.createElement('textarea');
  ta.value = value;
  ta.style.position = 'fixed';
  ta.style.opacity = '0';
  document.body.appendChild(ta);
  ta.select();
  document.execCommand('copy');
  document.body.removeChild(ta);
}

/** 12px glyph + 4px gap — the slot each trailing affordance reserves. */
const AFFORDANCE_SLOT = 16;

export type CellEditorMode = 'inline' | 'popover' | 'popup';
export type CellEditor = 'input' | 'select' | 'textarea';

/**
 * Options of the input editor: the antd Input props AS-IS (placeholder
 * / maxLength / … — whatever Input accepts), minus the value/onChange
 * surface the cell owns, plus two editor-level items.
 */
export interface EditableCellInputEditorOptions
  extends Omit<
    InputProps,
    'value' | 'defaultValue' | 'onChange' | 'onPressEnter' | 'onKeyDown'
  > {
  /** Save-disabled predicate on the trimmed draft. */
  validate?: (v: string) => boolean;
  /** Helper line under the control (popover mode only). */
  hint?: ReactNode;
}

/**
 * Options of the select editor: the antd Select props AS-IS (options /
 * allowClear / loading / …); `value` is the bound id the draft seeds
 * from. The onChange / dropdown-open surface stays cell-owned.
 */
export type EditableCellSelectEditorOptions = Omit<
  SelectProps,
  'defaultValue' | 'onChange' | 'onDropdownVisibleChange'
>;

/**
 * Options of the popup (modal) editor: the antd TextArea props AS-IS
 * (rows / maxLength / placeholder / …), minus the value/onChange
 * surface the cell owns, plus the shared `validate` predicate.
 */
export interface EditableCellTextareaEditorOptions
  extends Omit<
    React.ComponentProps<typeof Input.TextArea>,
    'value' | 'defaultValue' | 'onChange'
  > {
  /** Save-disabled predicate on the trimmed draft (empty allowed by
      default — long-text fields are clearable). */
  validate?: (v: string) => boolean;
  /** Helper line under the control (popover / popup modes only). */
  hint?: ReactNode;
}

/**
 * Inline (in-cell) text editor: Input (or TextArea for the textarea
 * control) + green check / red cross — the same icon language as
 * InfoDescriptions' inline edit. Enter saves for input only (Enter is
 * a newline in a textarea); Esc exits without saving; a rejected
 * submit keeps the editor open for a retry. Textarea drafts may be
 * empty (long-text fields are clearable); input drafts may not.
 */
function InlineTextEditor({
  control,
  initial,
  options,
  onSubmit,
  onExit,
}: {
  control: 'input' | 'textarea';
  initial: string;
  options?: EditableCellTextareaEditorOptions;
  /** false = stay open (error already toasted by the caller). */
  onSubmit: (next: string) => Promise<boolean>;
  onExit: () => void;
}): ReactElement {
  // `hint` is popover/popup-only; keep it out of the control spread.
  const { validate, hint, ...controlProps } = options ?? {};
  void hint;
  const [draft, setDraft] = useState(initial);
  const [saving, setSaving] = useState(false);
  const valid = validate
    ? validate(draft.trim())
    : control === 'textarea'
      ? true
      : draft.trim().length > 0;
  const save = async (): Promise<void> => {
    if (!valid || saving) return;
    setSaving(true);
    const ok = await onSubmit(draft.trim());
    setSaving(false);
    if (ok) onExit();
  };
  const shared = {
    ...controlProps,
    value: draft,
    onChange: (e: { target: { value: string } }) => setDraft(e.target.value),
    onKeyDown: (e: { key: string }) => {
      if (e.key === 'Escape') onExit();
    },
  };
  return (
    // Plain flex, NOT Space.Compact: compact mode squares the
    // control's right corners and sizes children to content — the
    // editor must stay fully rounded and fill the cell.
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 4,
        width: '100%',
      }}
    >
      {control === 'textarea' ? (
        <Input.TextArea
          rows={2}
          autoFocus
          {...shared}
          style={{ flex: 1, minWidth: 0, ...controlProps.style }}
        />
      ) : (
        <Input
          size="small"
          autoFocus
          {...(shared as unknown as InputProps)}
          onPressEnter={() => void save()}
          style={{ flex: 1, minWidth: 0, ...controlProps.style }}
        />
      )}
      <Button
        size="small"
        type="text"
        icon={<CheckOutlined />}
        loading={saving}
        disabled={!valid}
        style={{ color: '#52c41a' }}
        onClick={() => void save()}
      />
      <Button
        size="small"
        type="text"
        icon={<CloseOutlined />}
        disabled={saving}
        style={{ color: '#ff4d4f' }}
        onClick={onExit}
      />
    </div>
  );
}

/**
 * Inline (in-cell) select editor, same contract as the input one:
 * the pencil swaps the read-only text for Select + green check /
 * red cross; picking an option only updates the draft, the check
 * commits (Enter-equivalent) and the cross exits without saving.
 * The dropdown opens at once; a rejected submit keeps the editor
 * open for a retry.
 */
function InlineSelectEditor({
  initial,
  options,
  onSubmit,
  onExit,
}: {
  initial: string | undefined;
  options?: EditableCellSelectEditorOptions;
  onSubmit: (next: string | null) => Promise<boolean>;
  onExit: () => void;
}): ReactElement {
  const { loading, ...selectProps } = options ?? {};
  const [draft, setDraft] = useState<string | undefined>(initial);
  const [saving, setSaving] = useState(false);
  const unchanged = (draft ?? null) === (initial ?? null);
  const save = async (): Promise<void> => {
    if (unchanged || saving) return;
    setSaving(true);
    const ok = await onSubmit(draft ?? null);
    setSaving(false);
    if (ok) onExit();
  };
  return (
    // Plain flex, NOT Space.Compact: compact mode squares the
    // control's right corners and sizes children to content — the
    // editor must stay fully rounded and fill the cell.
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 4,
        width: '100%',
      }}
    >
      <Select
        size="small"
        autoFocus
        defaultOpen
        {...selectProps}
        style={{ flex: 1, minWidth: 0, ...selectProps.style }}
        loading={saving || !!loading}
        value={draft}
        onChange={(v) => setDraft(v)}
      />
      <Button
        size="small"
        type="text"
        icon={<CheckOutlined />}
        loading={saving}
        disabled={unchanged}
        style={{ color: '#52c41a' }}
        onClick={() => void save()}
      />
      <Button
        size="small"
        type="text"
        icon={<CloseOutlined />}
        disabled={saving}
        style={{ color: '#ff4d4f' }}
        onClick={onExit}
      />
    </div>
  );
}

/**
 * Popover ("tip box") text editor behind a pencil trigger: Input (or
 * TextArea for the textarea control) + optional hint + Cancel / Save.
 * Enter saves for input only; Esc cancels; Save stays disabled while
 * the trimmed draft is invalid (textarea drafts may be empty); a
 * rejected submit keeps the box open for a retry.
 */
function PopoverTextEditor({
  control,
  initial,
  options,
  onSubmit,
  onOpenChange,
  children,
}: {
  control: 'input' | 'textarea';
  initial: string;
  options?: EditableCellTextareaEditorOptions;
  onSubmit: (next: string) => Promise<boolean>;
  onOpenChange?: (open: boolean) => void;
  children: ReactNode;
}): ReactElement {
  const { t } = useTranslation();
  const { validate, hint, ...controlProps } = options ?? {};
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState(initial);
  const [saving, setSaving] = useState(false);
  const valid = validate
    ? validate(draft.trim())
    : control === 'textarea'
      ? true
      : draft.trim().length > 0;
  // Every INTERNAL close path (Cancel / Save success / Esc) must also
  // notify the parent — outside-click closes already flow through the
  // Popover's onOpenChange, but these would otherwise leave the
  // parent's editorOpen stuck at true (affordances never re-hide).
  const close = (): void => {
    onOpenChange?.(false);
    setOpen(false);
  };
  const save = async (): Promise<void> => {
    if (!valid || saving) return;
    setSaving(true);
    const ok = await onSubmit(draft.trim());
    setSaving(false);
    if (ok) close();
  };
  const shared = {
    ...controlProps,
    value: draft,
    onChange: (e: { target: { value: string } }) => setDraft(e.target.value),
    onKeyDown: (e: { key: string }) => {
      if (e.key === 'Escape') close();
    },
  };
  return (
    <Popover
      trigger="click"
      open={open}
      onOpenChange={(v) => {
        onOpenChange?.(v);
        // Fully controlled `open`: both directions must be honored,
        // otherwise the trigger click never opens the box.
        if (v) setOpen(true);
        else if (!saving) setOpen(false);
      }}
      placement="bottomLeft"
      content={
        <div style={{ width: 280 }}>
          {control === 'textarea' ? (
            <Input.TextArea rows={3} autoFocus {...shared} />
          ) : (
            <Input
              autoFocus
              {...(shared as unknown as InputProps)}
              onPressEnter={() => void save()}
            />
          )}
          {hint && (
            <div
              style={{ marginTop: 4, fontSize: 12, color: 'rgba(0, 0, 0, 0.45)' }}
            >
              {hint}
            </div>
          )}
          <div
            style={{
              marginTop: 8,
              display: 'flex',
              justifyContent: 'flex-end',
              gap: 8,
            }}
          >
            <Button size="small" onClick={close}>
              {t('common.cancel')}
            </Button>
            <Button
              size="small"
              type="primary"
              loading={saving}
              disabled={!valid}
              onClick={() => void save()}
            >
              {t('common.save')}
            </Button>
          </div>
        </div>
      }
    >
      {/* Re-seed the draft on every trigger click. */}
      <span style={{ display: 'inline-flex' }} onClick={() => setDraft(initial)}>
        {children}
      </span>
    </Popover>
  );
}

/**
 * Popover select editor behind a pencil trigger: Select + Cancel /
 * Save — picking an option only updates the draft, Save commits.
 * Save stays disabled while the draft is unchanged; a rejected
 * submit keeps the box open for a retry.
 */
function PopoverSelectEditor({
  initial,
  options,
  onSubmit,
  onOpenChange,
  children,
}: {
  initial: string | null;
  options?: EditableCellSelectEditorOptions;
  onSubmit: (next: string | null) => Promise<boolean>;
  onOpenChange?: (open: boolean) => void;
  children: ReactNode;
}): ReactElement {
  const { t } = useTranslation();
  const { loading, ...selectProps } = options ?? {};
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<string | undefined>(initial ?? undefined);
  const [saving, setSaving] = useState(false);
  const unchanged = (draft ?? null) === (initial ?? null);
  const close = (): void => {
    onOpenChange?.(false);
    setOpen(false);
  };
  const save = async (): Promise<void> => {
    if (unchanged || saving) return;
    setSaving(true);
    const ok = await onSubmit(draft ?? null);
    setSaving(false);
    if (ok) close();
  };
  return (
    <Popover
      trigger="click"
      open={open}
      onOpenChange={(v) => {
        onOpenChange?.(v);
        if (v) setOpen(true);
        else if (!saving) setOpen(false);
      }}
      placement="bottomLeft"
      content={
        <div style={{ width: 220 }}>
          <Select
            {...selectProps}
            loading={saving || !!loading}
            value={draft}
            onChange={(v) => setDraft(v)}
          />
          <div
            style={{
              marginTop: 8,
              display: 'flex',
              justifyContent: 'flex-end',
              gap: 8,
            }}
          >
            <Button size="small" onClick={close}>
              {t('common.cancel')}
            </Button>
            <Button
              size="small"
              type="primary"
              loading={saving}
              disabled={unchanged}
              onClick={() => void save()}
            >
              {t('common.save')}
            </Button>
          </div>
        </div>
      }
    >
      {/* Re-seed the draft on every trigger click. */}
      <span
        style={{ display: 'inline-flex' }}
        onClick={() => setDraft(initial ?? undefined)}
      >
        {children}
      </span>
    </Popover>
  );
}

/**
 * Popup (modal) editor behind a pencil trigger: a Modal hosting the
 * configured control (Input / Select / TextArea) + Save / Cancel; the
 * modal title is the owning column's title (`title`), falling back
 * to the generic "Edit" label. Same draft + confirm/cancel contract
 * as the other editors: input drafts must be non-empty, textarea
 * drafts may be empty (clearable), select commits only when changed.
 */
function PopupEditor({
  control,
  title,
  initial,
  options,
  onSubmit,
  onOpenChange,
  children,
}: {
  control: CellEditor;
  title?: ReactNode;
  initial: string;
  options?:
    | EditableCellInputEditorOptions
    | EditableCellSelectEditorOptions
    | EditableCellTextareaEditorOptions;
  onSubmit: (next: string | null) => Promise<boolean>;
  onOpenChange?: (open: boolean) => void;
  children: ReactNode;
}): ReactElement {
  const { t } = useTranslation();
  const isSelect = control === 'select';
  const selectOpts = isSelect
    ? (options as EditableCellSelectEditorOptions | undefined)
    : undefined;
  const textOpts = isSelect
    ? undefined
    : (options as EditableCellTextareaEditorOptions | undefined);
  const { validate, hint, ...textProps } = textOpts ?? {};
  // Select drafts seed from the bound id (options.value); text drafts
  // from the cell value.
  const seed = isSelect
    ? ((selectOpts?.value as string | undefined) ?? null)
    : initial;
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<string | null>(seed);
  const [saving, setSaving] = useState(false);
  const valid = isSelect
    ? (draft ?? null) !== (seed ?? null)
    : validate
      ? validate((draft ?? '').trim())
      : control === 'textarea'
        ? true
        : (draft ?? '').trim().length > 0;
  const close = (): void => {
    onOpenChange?.(false);
    setOpen(false);
  };
  const save = async (): Promise<void> => {
    if (!valid || saving) return;
    setSaving(true);
    const ok = await onSubmit(isSelect ? draft : (draft ?? '').trim());
    setSaving(false);
    if (ok) close();
  };
  return (
    <>
      <span
        style={{ display: 'inline-flex' }}
        onClick={() => {
          setDraft(seed);
          onOpenChange?.(true);
          setOpen(true);
        }}
      >
        {children}
      </span>
      <Modal
        open={open}
        title={title ?? t('common.edit')}
        okText={t('common.save')}
        onCancel={() => {
          if (!saving) close();
        }}
        onOk={() => void save()}
        confirmLoading={saving}
        okButtonProps={{ disabled: !valid }}
      >
        {isSelect ? (
          // antd Select sizes to its content by default (Input /
          // TextArea are width:100%) — force full modal width.
          <Select
            {...selectOpts}
            style={{ width: '100%', ...selectOpts?.style }}
            loading={saving || !!selectOpts?.loading}
            value={draft ?? undefined}
            onChange={(v) => setDraft(v ?? null)}
          />
        ) : control === 'textarea' ? (
          <Input.TextArea
            rows={4}
            autoFocus
            {...textProps}
            value={draft ?? ''}
            onChange={(e) => setDraft(e.target.value)}
          />
        ) : (
          <Input
            autoFocus
            {...(textProps as unknown as InputProps)}
            value={draft ?? ''}
            onChange={(e) => setDraft(e.target.value)}
            onPressEnter={() => void save()}
          />
        )}
        {hint && (
          <div
            style={{ marginTop: 4, fontSize: 12, color: 'rgba(0, 0, 0, 0.45)' }}
          >
            {hint}
          </div>
        )}
      </Modal>
    </>
  );
}

/**
 * CellShell (internal) — the composable two-state table cell behind
 * the two public presets, CopyableCell and EditableCell. Feature
 * axes, each opt-in at the shell level:
 *
 *   value     canonical data string — copy payload, editor seed and
 *             render-fn argument
 *   display   the read-only shape: a ReactNode rendered as-is, or
 *             (value) => ReactNode; OMITTED = the default text form
 *             (value + mono / ellipsis; link wraps it in a router
 *             Link). mono / ellipsis apply to the default text form
 *             only — custom shapes own their styling and compose the
 *             shared truncation blocks: EllipsisText (end truncation
 *             + non-shrinkable trailer) or MiddleEllipsisText (both
 *             ends kept)
 *   copyable  hover-revealed copy icon (copies the FULL value)
 *   affordanceMode
 *             'hover' (default) | 'always' | 'row-hover' — visibility
 *             of ALL trailing affordances (copy + pencil), applied
 *             uniformly; an open editor box keeps them visible.
 *             'hover' reveals them while THIS cell is hovered;
 *             'row-hover' reveals them while ANY part of the table
 *             row is hovered (a global CSS rule in index.css —
 *             cross-cell coordination cannot live in one cell's
 *             state).
 *
 *   editable  pencil affordance + an editor, shaped by:
 *               editor      'input' | 'select' | 'textarea' — the
 *                             control used (textarea = multi-line)
 *               editorMode  'inline' — editor lives in the row
 *                             (input: pencil swaps text for
 *                             Input+check/cross; select: pencil
 *                             swaps text for Select+check/cross,
 *                             dropdown opens at once; textarea: same
 *                             with a small TextArea)
 *                           'popover' — pencil opens a tip box with
 *                             the control + Save/Cancel
 *                           'popup' — pencil opens a modal hosting
 *                             the control + Save/Cancel; the modal
 *                             title is `editorTitle` as composed by
 *                             the caller (e.g. "Edit {column}"),
 *                             defaulting to "Edit"
 *               editorOptions  the underlying antd control's props
 *                             AS-IS (Input: placeholder/maxLength/…;
 *                             Select: options/allowClear/loading/…,
 *                             value = the bound id) plus validate /
 *                             hint for the input editor
 *
 * All affordances (copy, pencil) sit in the SAME content-hugging
 * inline-flex row as the text, so they always sit right after it.
 *
 * Ellipsis measurement is deliberately split from layout: the width
 * budget is observed on the OUTER block box (content-independent —
 * the parent must be a fixed column or a flex-grow wrapper, or the
 * rendered string feeds back into its own measurement and the cell
 * flickers), minus one AFFORDANCE_SLOT per trailing icon; the inner
 * row shrink-to-fits the pre-truncated text.
 *
 * onSubmit contract: resolve = success (the caller toasts / reloads
 * inside it and the editor closes); reject = EditableCell toasts the
 * error and keeps the editor open for a retry.
 */
export function CellShell({
  value,
  display,
  link,
  mono = false,
  ellipsis = false,
  copyable = false,
  editable = false,
  affordanceMode = 'hover',
  editor = 'input',
  editorMode = 'popover',
  editorTitle,
  editorOptions,
  onSubmit,
}: {
  /** Full value — what copy writes and input editors seed from. */
  value: string;
  /** Read-only shape: node as-is or (value) => node; omitted = the
      default text form. */
  display?: ReactNode | ((value: string) => ReactNode);
  link?: string;
  /** Monospace (identifiers) vs the table's proportional text font. */
  mono?: boolean;
  /** Middle-truncate when the value overflows the column. */
  ellipsis?: boolean;
  copyable?: boolean;
  editable?: boolean;
  /** Uniform visibility of the copy / pencil affordances. */
  affordanceMode?: 'hover' | 'always' | 'row-hover';
  /** Which control the editor uses. */
  editor?: CellEditor;
  editorMode?: CellEditorMode;
  /** Full title of the popup modal, composed by the caller (e.g.
      "Edit {column}" via common.edit_with_title); omitted = the
      generic "Edit" label. */
  editorTitle?: ReactNode;
  /** Control props as-is — see EditableCell*EditorOptions. */
  editorOptions?:
    | EditableCellInputEditorOptions
    | EditableCellSelectEditorOptions
    | EditableCellTextareaEditorOptions;
  onSubmit?: (next: string | null) => Promise<void>;
}): ReactElement {
  const { t } = useTranslation();
  const { message } = AntdApp.useApp();
  const [hovered, setHovered] = useState(false);
  const [copied, setCopied] = useState(false);
  const [editing, setEditing] = useState(false);
  const [editorOpen, setEditorOpen] = useState(false);
  // One visibility rule for every trailing affordance; an open
  // editor box (or a just-copied flash) keeps them visible.
  const affordanceVisible =
    affordanceMode === 'always' || hovered || copied || editorOpen;
  // 'row-hover' leaves the base visibility to CSS (.table-cell-
  // affordance is hidden by default and revealed by the
  // .ant-table-row:hover rule in index.css) — an inline opacity here
  // would beat that stylesheet rule and break it. Inline styles are
  // emitted ONLY for the state-level overrides (which must win over
  // the CSS hide) and for the non-row modes.
  const affordanceStyle: CSSProperties =
    affordanceMode === 'row-hover' && !copied && !editorOpen
      ? {}
      : { opacity: affordanceVisible ? 1 : 0 };
  // Ellipsis budget observed on the outer (content-independent) box.
  const budgetRef = useRef<HTMLDivElement | null>(null);
  const [textBudget, setTextBudget] = useState<number | null>(null);

  const affordances = (copyable ? 1 : 0) + (editable ? 1 : 0);

  useEffect(() => {
    if (!ellipsis || editing) return;
    const el = budgetRef.current;
    if (!el) return;
    const compute = (): void =>
      setTextBudget(Math.max(0, el.clientWidth - affordances * AFFORDANCE_SLOT));
    compute();
    const ro = new ResizeObserver(compute);
    ro.observe(el);
    return () => ro.disconnect();
  }, [ellipsis, editing, affordances]);

  const onCopy = async (e: ReactMouseEvent): Promise<void> => {
    e.stopPropagation();
    e.preventDefault();
    try {
      await copyText(value);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // Clipboard blocked (permissions / iframe); nothing sensible to
      // surface from a shared cell component.
    }
  };

  /** Runs the caller's onSubmit; toasts rejections, reports success. */
  const submit = async (next: string | null): Promise<boolean> => {
    if (!onSubmit) return true;
    try {
      await onSubmit(next);
      return true;
    } catch (err) {
      message.error(extractApiError(err));
      return false;
    }
  };

  const copyIcon = (
    <Tooltip title={copied ? t('common.copied') : t('common.copy')}>
      <span
        role="button"
        aria-label={t('common.copy')}
        className="table-cell-affordance"
        onClick={(e) => void onCopy(e)}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          cursor: 'pointer',
          ...affordanceStyle,
          color: copied ? '#52c41a' : 'rgba(0, 0, 0, 0.45)',
          flexShrink: 0,
        }}
      >
        {copied ? (
          <CheckOutlined style={{ fontSize: 12, display: 'block' }} />
        ) : (
          <CopyOutlined style={{ fontSize: 12, display: 'block' }} />
        )}
      </span>
    </Tooltip>
  );

  const pencil = (
    <span
      role="button"
      aria-label={t('common.edit')}
      className="table-cell-affordance"
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        cursor: 'pointer',
        ...affordanceStyle,
        color: 'rgba(0, 0, 0, 0.45)',
        flexShrink: 0,
      }}
    >
      <EditOutlined style={{ fontSize: 12, display: 'block' }} />
    </span>
  );

  // inline editing: the editor replaces the whole cluster while
  // active (read-only text + pencil otherwise).
  if (editable && editorMode === 'inline' && editing) {
    return editor === 'select' ? (
      <InlineSelectEditor
        initial={
          (editorOptions as EditableCellSelectEditorOptions | undefined)?.value
        }
        options={editorOptions as EditableCellSelectEditorOptions}
        onSubmit={(v) => submit(v)}
        onExit={() => setEditing(false)}
      />
    ) : (
      <InlineTextEditor
        control={editor as 'input' | 'textarea'}
        initial={value}
        options={editorOptions as EditableCellTextareaEditorOptions}
        onSubmit={(next) => submit(next)}
        onExit={() => setEditing(false)}
      />
    );
  }

  // Read-only state: a custom `display` (node or render fn) wins;
  // omitted = the default text form (value + mono / ellipsis). Both
  // go through the shared truncation building blocks — the plain
  // EllipsisText (end truncation; inert until overflow) or, when
  // `ellipsis` is set, the budget-driven MiddleEllipsisText.
  const readOnly =
    display === undefined ? (
      ellipsis && textBudget !== null ? (
        <MiddleEllipsisText text={value} maxWidth={textBudget} mono={mono} />
      ) : (
        <EllipsisText text={value} mono={mono} />
      )
    ) : typeof display === 'function' ? (
      display(value)
    ) : (
      display
    );

  return (
    <div ref={budgetRef} style={{ minWidth: 0, maxWidth: '100%' }}>
      <div
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 4,
          maxWidth: '100%',
        }}
        onMouseEnter={
          // Pointer tracking feeds ONLY the cell-'hover' mode; in
          // 'always' / 'row-hover' the hovered state is never read,
          // so tracking here would cause two pointless re-renders on
          // every enter / leave of the cluster.
          affordanceMode === 'hover' ? () => setHovered(true) : undefined
        }
        onMouseLeave={
          affordanceMode === 'hover' ? () => setHovered(false) : undefined
        }
      >
        {link ? <Link to={link}>{readOnly}</Link> : readOnly}
        {editable &&
          (editorMode === 'popup' ? (
            <PopupEditor
              control={editor}
              title={editorTitle}
              initial={value}
              options={editorOptions}
              onSubmit={(v) => submit(v)}
              onOpenChange={setEditorOpen}
            >
              {pencil}
            </PopupEditor>
          ) : editorMode === 'popover' ? (
            editor === 'select' ? (
              <PopoverSelectEditor
                initial={
                  (editorOptions as EditableCellSelectEditorOptions | undefined)
                    ?.value ?? null
                }
                options={editorOptions as EditableCellSelectEditorOptions}
                onSubmit={(v) => submit(v)}
                onOpenChange={setEditorOpen}
              >
                {pencil}
              </PopoverSelectEditor>
            ) : (
              <PopoverTextEditor
                control={editor as 'input' | 'textarea'}
                initial={value}
                options={editorOptions as EditableCellTextareaEditorOptions}
                onSubmit={(v) => submit(v)}
                onOpenChange={setEditorOpen}
              >
                {pencil}
              </PopoverTextEditor>
            )
          ) : (
            <span
              style={{ display: 'inline-flex' }}
              onClick={() => setEditing(true)}
            >
              {pencil}
            </span>
          ))}
        {/* Copy trails the pencil: editing is the cell's essence,
            copying the incidental affordance. */}
        {copyable && copyIcon}
      </div>
    </div>
  );
}

/** Read-only-shape props shared by both public cell presets. */
interface CellDisplayProps {
  /** Canonical data string — copy payload, editor seed, render-fn
      argument. */
  value: string;
  /** Read-only shape: node as-is or (value) => node; omitted = the
      default text form. */
  display?: ReactNode | ((value: string) => ReactNode);
  link?: string;
  mono?: boolean;
  ellipsis?: boolean;
  affordanceMode?: 'hover' | 'always' | 'row-hover';
}

/**
 * CopyableCell — a read-only cell whose ESSENCE is copying: the copy
 * affordance is always on and cannot be switched off; no editing.
 */
export function CopyableCell(props: CellDisplayProps): ReactElement {
  return <CellShell {...props} copyable />;
}

/**
 * EditableCell — a cell whose ESSENCE is editing: the pencil + editor
 * are always on; the inherited copy affordance is incidental and
 * opt-in via `copyable`.
 */
export function EditableCell(
  props: CellDisplayProps & {
    copyable?: boolean;
    editor?: CellEditor;
    editorMode?: CellEditorMode;
    editorTitle?: ReactNode;
    editorOptions?:
      | EditableCellInputEditorOptions
      | EditableCellSelectEditorOptions
      | EditableCellTextareaEditorOptions;
    onSubmit?: (next: string | null) => Promise<void>;
  },
): ReactElement {
  return <CellShell {...props} editable />;
}

