import type { CSSProperties, ReactElement, ReactNode } from 'react';
import { useCallback, useState } from 'react';
import { Button, Input, Select, theme, Tooltip, Typography } from 'antd';
import {
  CheckOutlined,
  CloseOutlined,
  EditOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';

/**
 * Data-driven label/value descriptions grid — the "Basic Info" block
 * extracted from DetailLayout.
 *
 * Designed after antd's Table: callers declare FIELD DESCRIPTORS over
 * one `dataSource` record (`title` / `dataIndex` / `key` / `render`,
 * same vocabulary as Table columns) and never inject interaction JSX.
 * Every affordance is owned by this component and switched on purely
 * by data:
 *
 *   - `tip`      → question-mark icon on the label row (hover/focus
 *                  tooltip);
 *   - `editable` → edit icon on the label row; while editing it turns
 *                  into a green check (save) + red cross (cancel),
 *                  the value area becomes an editor — single-line
 *                  Input by default, multi-line TextArea when
 *                  `editor: 'textarea'`, fixed-choice Select when
 *                  `editor: 'select'` (options via `editorOptions`,
 *                  multi-pick with `editorMultiple`) — and saving is
 *                  handled through the component-level `onSave` (throw
 *                  from it to keep the editor open).
 *
 * Display values are clamped to `valueMaxRows` lines (default 2) via
 * antd's controlled ellipsis with an expand/collapse control that
 * only appears when the content actually overflows. CONTRACT: the
 * clamp applies to PLAIN TEXT values only — when `render` returns
 * anything but a string it is treated as an ATOMIC custom rendering
 * (status Tag, icon widget, fragment, …) and passed through verbatim,
 * never fed into the text-clamping wrapper.
 *
 * Layout: responsive grid of stacked label-over-value cells, max 3 per
 * row (2/1 on narrower screens); label bold (colorText), value normal
 * weight (colorTextSecondary) — all colors are theme tokens, so dark
 * mode and primary-color customization work — no width measurement
 * needed.
 */

/** Fixed height of the label row: label text AND every label-row icon
    button share it, so icons stay visually centered on the text
    (bare line-box centering makes icon boxes appear to float above
    the glyphs' optical center). Matches the natural line-height of
    the 14px label font. */
export const LABEL_ROW_HEIGHT = 22;

/** Shared size of every label-row icon button: a square matching the
    label text's own height, with a 12px glyph so the icon stays
    proportionate inside the small box. Borderless on purpose — a
    bordered box visually outweighs the plain label text. */
const LABEL_ICON_BUTTON_STYLE: CSSProperties = {
  width: LABEL_ROW_HEIGHT,
  height: LABEL_ROW_HEIGHT,
  minWidth: LABEL_ROW_HEIGHT
};

/** Basic-info grid geometry (px). */
/** Slot floor — below this width a row drops from 3 to 2 to 1 slots. */
const SLOT_MIN_WIDTH = 200;
const GAP_X = 32;
/** Default max display lines of a value; content beyond it is
    clamped with an antd-style expand/collapse control. */
const DEFAULT_VALUE_MAX_ROWS = 2;

/**
 * One field descriptor — the Table-column-like declaration of a single
 * label/value cell.
 */
export interface InfoFieldDescriptor<T extends object = Record<string, unknown>> {
  /** React key; falls back to `dataIndex`, then stringified `title`. */
  key?: string;
  /** Label text — bold, on its own line above the value. */
  title: ReactNode;
  /** Property name read from `dataSource` for this field's value. */
  dataIndex: string;
  /** Custom value renderer; defaults to plain text ('—' when empty).
      Returning a STRING participates in the valueMaxRows text clamp;
      returning anything else is treated as an atomic custom rendering
      (Tag, widget, …) and bypasses the clamp entirely. */
  render?: (value: unknown, record: T) => ReactNode;
  /** OPTIONAL hint rendered as a question-mark icon on the label row
      (hover/focus tooltip). Data-driven: pages compute it from the
      record, the icon/interaction stays inside this component. */
  tip?: ReactNode;
  /** Inline-editable: shows the edit icon, and while editing the
      green check / red cross pair plus the editor control (see
      `editor`). */
  editable?: boolean;
  /** Editor control for `editable` fields: single-line Input (the
      default), multi-line TextArea, or fixed-choice Select (options
      via `editorOptions`). */
  editor?: 'input' | 'textarea' | 'select';
  /** TextArea rows when `editor: 'textarea'` (default 3). */
  editorRows?: number;
  /** Choices for `editor: 'select'`. */
  editorOptions?: { value: string; label: string }[];
  /** Multi-pick for `editor: 'select'` — the draft and the `onSave`
      value become string[] instead of string. */
  editorMultiple?: boolean;
  /** Max display lines of the value (>= 1, default 2). Content beyond
      it is clamped with an antd-style expand/collapse control. */
  valueMaxRows?: number;
  /** Span the full row (1/2/3 slots depending on width). */
  fullRow?: boolean;
}

export interface InfoDescriptionsProps<T extends object> {
  /** The record the fields read from. */
  dataSource: T | null | undefined;
  /** Field descriptors, in display order. */
  fields: InfoFieldDescriptor<T>[];
  /**
   * Called with the raw editor value when saving an `editable`
   * field — string, or string[] for `editorMultiple` selects. THROW
   * (or return a rejected promise) to keep the editor open — the
   * component only exits edit mode on success. Pages own their own
   * success/error toasts.
   */
  onSave?: (
    dataIndex: string,
    value: string | string[],
    record: T,
  ) => Promise<void> | void;
}

export function InfoDescriptions<T extends object>({
  dataSource,
  fields,
  onSave,
}: InfoDescriptionsProps<T>): ReactElement {
  const { t } = useTranslation();
  // Design tokens (link color etc.) follow the active ConfigProvider
  // theme — dark mode and primary-color customization included.
  const { token } = theme.useToken();
  // Inline-edit state — one editable field at a time. `draftList`
  // backs `editorMultiple` selects; `draft` backs everything else.
  const [editingKey, setEditingKey] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const [draftList, setDraftList] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  const fieldKey = (f: InfoFieldDescriptor<T>): string =>
    f.key ?? f.dataIndex ?? String(f.title);

  const startEdit = useCallback(
    (f: InfoFieldDescriptor<T>, value: unknown): void => {
      if (f.editor === 'select' && f.editorMultiple) {
        setDraftList(Array.isArray(value) ? value.map((v) => String(v)) : []);
      } else {
        setDraft(
          typeof value === 'string' ? value : value == null ? '' : String(value),
        );
      }
      setEditingKey(fieldKey(f));
    },
    [],
  );

  const save = useCallback(
    async (f: InfoFieldDescriptor<T>): Promise<void> => {
      if (!dataSource || !onSave) return;
      const multi = f.editor === 'select' && f.editorMultiple;
      setSaving(true);
      try {
        await onSave(f.dataIndex, multi ? draftList : draft, dataSource);
        setEditingKey(null);
      } catch {
        // The page surfaces the error; keeping the editor open lets
        // the user retry without losing the draft.
      } finally {
        setSaving(false);
      }
    },
    [dataSource, draft, draftList, onSave],
  );

  const renderValue = (f: InfoFieldDescriptor<T>, value: unknown): ReactNode => {
    const content =
      dataSource && f.render
        ? f.render(value, dataSource)
        : value == null || value === ''
          ? '—'
          : String(value);
    // A non-string render result is an ATOMIC custom rendering (status
    // Tag, fragment, interactive widget, …) — pass it through verbatim.
    // Feeding such nodes into the ellipsis wrapper would let the text
    // clamp truncate them or append a meaningless more/less control.
    if (typeof content !== 'string') return content;
    // antd controlled ellipsis: clamp to the field's max rows and
    // offer expand/collapse only when the content actually overflows
    // (antd renders the symbol only in that case). Values fitting
    // inside the rows render unchanged apart from the wrapper.
    return (
      <Typography.Paragraph
        style={{ margin: 0 }}
        ellipsis={{
          rows: Math.max(1, f.valueMaxRows ?? DEFAULT_VALUE_MAX_ROWS),
          expandable: 'collapsible',
          // Theme-aware link color (token, not a hardcoded hex) — the
          // symbol must NOT fade with the value's 0.8-black text,
          // it's an interactive control.
          symbol: (expanded: boolean) => (
            <span style={{ color: token.colorLink }}>
              {t(expanded ? 'descriptions.collapse' : 'descriptions.expand')}
            </span>
          ),
        }}
      >
        {content}
      </Typography.Paragraph>
    );
  };

  return (
    <div
      style={{
        display: 'grid',
        // At MOST 3 slots per row; narrower screens auto-fill down to
        // 2 and then 1. Tracks stretch (1fr), leftover space flows
        // into the slots.
        gridTemplateColumns: `repeat(auto-fill, minmax(min(100%, max(${SLOT_MIN_WIDTH}px, calc((100% - ${
          GAP_X * 2
        }px) / 3))), 1fr))`,
        // Row gap is larger than the intra-cell label/value spacing
        // so the two-line cells of adjacent rows stay visually
        // distinct.
        gap: `20px ${GAP_X}px`,
        alignItems: 'start',
      }}
    >
      {fields.map((f) => {
        const key = fieldKey(f);
        const isEditing = editingKey === key;
        const rawValue = dataSource
          ? (dataSource as Record<string, unknown>)[f.dataIndex]
          : undefined;

        const cell = (
          <div style={{ minWidth: 0 }}>
            {/* Label row — text + component-owned trailing icons. */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                height: LABEL_ROW_HEIGHT,
              }}
            >
              <div
                title={typeof f.title === 'string' ? f.title : undefined}
                style={{
                  // minWidth: 0 lets the label shrink (ellipsis) while
                  // the icons keep their natural width. lineHeight
                  // fills the fixed row height so the text sits
                  // centered in the same box as the icon buttons.
                  minWidth: 0,
                  lineHeight: `${LABEL_ROW_HEIGHT}px`,
                  fontWeight: 500,
                  color: token.colorText,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}
              >
                {f.title}
              </div>
              {(f.tip != null || f.editable) && (
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    flexShrink: 0,
                  }}
                >
                  {/* Hint icon — present whenever `tip` is set. */}
                  {f.tip != null && (
                    <Tooltip title={f.tip}>
                      <Button
                        type="text"
                        shape="square"
                        size="small"
                        icon={<QuestionCircleOutlined />}
                        style={{
                          ...LABEL_ICON_BUTTON_STYLE,
                          color: token.colorTextTertiary,
                        }}
                      />
                    </Tooltip>
                  )}
                  {/* Edit interaction — borderless colored glyphs. */}
                  {f.editable &&
                    (isEditing ? (
                      <>
                        <Tooltip title={t('common.save')}>
                          <Button
                            type="text"
                            shape="square"
                            size="small"
                            icon={<CheckOutlined />}
                            style={{
                              ...LABEL_ICON_BUTTON_STYLE,
                              color: token.colorSuccess,
                            }}
                            loading={saving}
                            onClick={() => void save(f)}
                          />
                        </Tooltip>
                        <Tooltip title={t('common.cancel')}>
                          <Button
                            type="text"
                            shape="square"
                            size="small"
                            icon={<CloseOutlined />}
                            style={{
                              ...LABEL_ICON_BUTTON_STYLE,
                              color: token.colorError,
                            }}
                            disabled={saving}
                            onClick={() => setEditingKey(null)}
                          />
                        </Tooltip>
                      </>
                    ) : (
                      <Tooltip title={t('common.edit')}>
                        <Button
                          type="text"
                          shape="square"
                          size="small"
                          icon={<EditOutlined />}
                          style={LABEL_ICON_BUTTON_STYLE}
                          onClick={() => startEdit(f, rawValue)}
                        />
                      </Tooltip>
                    ))}
                </div>
              )}
            </div>

            {/* Value area — pure content. */}
            <div
              style={{
                // minWidth: 0 + overflowWrap let long unbreakable
                // strings (ids) break inside the slot instead of
                // bleeding into the next one. Secondary text token:
                // one step softer than the label's colorText, and
                // theme-aware (dark mode included).
                minWidth: 0,
                overflowWrap: 'anywhere',
                fontWeight: 400,
                color: token.colorTextSecondary,
              }}
            >
              {isEditing ? (
                f.editor === 'textarea' ? (
                  <Input.TextArea
                    value={draft}
                    onChange={(e) => setDraft(e.target.value)}
                    rows={f.editorRows ?? 3}
                    autoFocus
                  />
                ) : f.editor === 'select' ? (
                  // No defaultOpen: entering edit mode only swaps the
                  // value for the control; the dropdown expands when
                  // the user clicks it.
                  f.editorMultiple ? (
                    <Select
                      mode="multiple"
                      value={draftList}
                      onChange={(v: string[]) => setDraftList(v)}
                      options={f.editorOptions ?? []}
                      style={{ width: '100%' }}
                      autoFocus
                    />
                  ) : (
                    <Select
                      value={draft}
                      onChange={(v: string) => setDraft(v)}
                      options={f.editorOptions ?? []}
                      style={{ width: '100%' }}
                      autoFocus
                    />
                  )
                ) : (
                  <Input
                    value={draft}
                    onChange={(e) => setDraft(e.target.value)}
                    onPressEnter={() => void save(f)}
                    autoFocus
                  />
                )
              ) : (
                renderValue(f, rawValue)
              )}
            </div>
          </div>
        );

        return f.fullRow ? (
          <div key={key} style={{ gridColumn: '1 / -1' }}>
            {cell}
          </div>
        ) : (
          <div key={key}>{cell}</div>
        );
      })}
    </div>
  );
}
