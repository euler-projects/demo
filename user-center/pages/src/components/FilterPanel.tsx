import type { CSSProperties } from 'react';
import type { ReactElement, ReactNode } from 'react';
import {
  Children,
  createContext,
  cloneElement,
  isValidElement,
  useContext,
  useMemo,
} from 'react';
import { Button, Form } from 'antd';
import { useTranslation } from 'react-i18next';

/** Fixed geometry shared by every filter field (px). */
const LABEL_MAX_WIDTH = 100;
const LABEL_PADDING = 8;
/** Control minimum width; controls grow to fill each flex row. */
const CONTROL_MIN_WIDTH = 200;
const GAP_X = 24;
const ACTION_BUTTON_WIDTH = 88;

/**
 * Measure the widest label's rendered pixel width using the console's
 * base font (antd default 14px system stack), capped at `maxWidth`.
 * Consumers give every field on the page this shared width so labels
 * align across rows while short labels don't waste space; longer
 * labels truncate (ellipsis + title tooltip).
 */
let measureCanvas: HTMLCanvasElement | null = null;

export function measureLabelWidth(
  labels: string[],
  maxWidth: number,
  fontWeight = '500',
): number {
  if (labels.length === 0) return maxWidth;
  if (typeof document === 'undefined') return maxWidth;
  measureCanvas = measureCanvas ?? document.createElement('canvas');
  const ctx = measureCanvas.getContext('2d');
  if (!ctx) return maxWidth;
  // Weight matches the rendered label so measurement is exact.
  ctx.font =
    `${fontWeight} 14px -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, Avenir, Helvetica, Arial, sans-serif`;
  const widest = Math.max(...labels.map((l) => ctx.measureText(l).width));
  // +LABEL_PADDING: the label box is border-box, so the inner text area
  // is the box width minus its right padding.
  return Math.min(maxWidth, Math.ceil(widest) + LABEL_PADDING);
}

function adaptLabelWidth(labels: string[]): number {
  return measureLabelWidth(labels, LABEL_MAX_WIDTH);
}

/** Label width decided by FilterPanel, consumed by FilterField. */
const FilterGeometryContext = createContext<{ labelWidth: number }>({
  labelWidth: LABEL_MAX_WIDTH,
});

export interface FilterPanelProps {
  /** Receives form values on Search (form submit). */
  onSearch: (values: Record<string, unknown>) => void;
  /** Invoked after fields are cleared on Reset. */
  onReset: () => void;
  /** FilterField children. */
  children: ReactNode;
}

/**
 * Pure filter panel BODY — the collapsible field grid with its
 * Search / Reset buttons. It owns the antd Form DRAFT values only;
 * the APPLIED filter is a table attribute (DataTable state) and the
 * panel injects new values through `onSearch` / `onReset`.
 *
 * Deliberately knows nothing about the toolbar row, the Filters
 * toggle (DataTable renders it), refresh, or the table itself.
 * DataTable mounts this panel below its toolbar when expanded.
 */
export function FilterPanel({
  onSearch,
  onReset,
  children,
}: FilterPanelProps): ReactElement {
  const { t } = useTranslation();
  const [form] = Form.useForm();

  // One shared label width for the whole page: widest label measured,
  // capped at LABEL_MAX_WIDTH.
  const labelKey = useMemo(() => {
    const labels: string[] = [];
    Children.forEach(children, (child) => {
      if (
        isValidElement<{ label?: unknown }>(child) &&
        typeof child.props.label === 'string'
      ) {
        labels.push(child.props.label);
      }
    });
    return labels.join('\u0000');
  }, [children]);
  const geometry = useMemo(
    () => ({ labelWidth: adaptLabelWidth(labelKey.split('\u0000').filter(Boolean)) }),
    [labelKey],
  );

  return (
    <FilterGeometryContext.Provider value={geometry}>
      <div
        style={{
          marginBottom: 12,
          padding: 16,
          background: '#fafafa',
          border: '1px solid #f0f0f0',
          borderRadius: 8,
        }}
      >
        <Form form={form} onFinish={(values) => onSearch(values)}>
          <div
            style={{
              display: 'grid',
              // Shared column tracks across all rows: auto-fill packs as
              // many cells as fit, 1fr stretches tracks to the full
              // panel width, and a short last row keeps the same track
              // widths as the rows above (no per-row re-distribution).
              gridTemplateColumns: `repeat(auto-fill, minmax(${geometry.labelWidth + CONTROL_MIN_WIDTH}px, 1fr))`,
              gap: `12px ${GAP_X}px`,
              alignItems: 'center',
            }}
          >
            {children}
            {/*
              Search/Reset share the field grid: pinned to the LAST
              column track (`-2 / -1`). Two 88px buttons + gap only
              need ~184px, less than one track's minimum width
              (label + 200px), so they fit whenever the last field
              row has ANY free track; otherwise auto-placement drops
              them onto a new row — right-aligned either way.
            */}
            <div
              style={{
                gridColumn: '-2 / -1',
                display: 'flex',
                justifyContent: 'flex-end',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <Button
                type="primary"
                htmlType="submit"
                style={{ width: ACTION_BUTTON_WIDTH }}
              >
                {t('common.search')}
              </Button>
              <Button
                style={{ width: ACTION_BUTTON_WIDTH }}
                onClick={() => {
                  form.resetFields();
                  onReset();
                }}
              >
                {t('common.reset')}
              </Button>
            </div>
          </div>
        </Form>
      </div>
    </FilterGeometryContext.Provider>
  );
}

export interface FilterFieldProps {
  /** Form field name. */
  name: string;
  /** Label text; right-aligned, truncated with a tooltip when long. */
  label: string;
  /**
   * Cells occupied. `2` spans exactly two grid cells (including the
   * gap between them); used for wide controls like date ranges.
   */
  span?: 1 | 2;
  /** Single control (Input / Select / DatePicker…); fills its slot. */
  children: ReactElement<{ style?: CSSProperties }>;
}

/**
 * One filter field: shared-width right-aligned truncated label +
 * adaptive control. The label width is decided by FilterPanel (widest
 * label on the page, capped at 100px) via context. Fields live on the
 * panel's shared grid tracks (one cell each, span-2 takes two), so
 * every row — including a short last row — keeps identical widths.
 */
export function FilterField({
  name,
  label,
  span = 1,
  children,
}: FilterFieldProps): ReactElement {
  const { labelWidth } = useContext(FilterGeometryContext);
  const cellWidth = labelWidth + CONTROL_MIN_WIDTH;
  // Span-2 fields swallow the second cell's label + the column gap, so
  // their minimum footprint is exactly two cells wide.
  const controlMin =
    span === 2 ? cellWidth * 2 + GAP_X - labelWidth : CONTROL_MIN_WIDTH;
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gridColumn: span === 2 ? 'span 2' : undefined,
      }}
    >
      <span
        title={label}
        style={{
          width: labelWidth,
          flex: `0 0 ${labelWidth}px`,
          textAlign: 'right',
          paddingRight: LABEL_PADDING,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          fontWeight: 500,
          color: 'rgba(0, 0, 0, 0.88)',
        }}
      >
        {label}
      </span>
      {/* Control area: flex slot that absorbs the row's leftover space;
          minWidth keeps the 200px guarantee while wrapping. */}
      <div style={{ flex: 1, minWidth: controlMin }}>
        <Form.Item name={name} noStyle>
          {cloneElement(children, {
            style: { width: '100%', ...children.props.style },
          })}
        </Form.Item>
      </div>
    </div>
  );
}
