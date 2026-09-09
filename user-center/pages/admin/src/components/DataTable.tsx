import type { ReactElement, ReactNode } from 'react';
import { useMemo, useState } from 'react';
import {
  Button,
  Checkbox,
  Divider,
  Popover,
  Space,
  Table,
  Typography,
} from 'antd';
import type { TableProps } from 'antd';
import type { ColumnType } from 'antd/es/table';
import {
  DownOutlined,
  ReloadOutlined,
  SettingOutlined,
  UpOutlined,
} from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { FilterPanel } from './FilterPanel';

/** Floor width of the single width-less (flexible) column (px). */
const DEFAULT_FLEX_FLOOR = 200;

/**
 * Stable identity of a column for the visibility settings: the
 * explicit `key` first, then `dataIndex` (every DataTable column must
 * provide one of the two).
 */
function columnKey(col: ColumnType<never> | { key?: unknown; dataIndex?: unknown }): string {
  const c = col as { key?: unknown; dataIndex?: unknown };
  return c.key != null ? String(c.key) : String(c.dataIndex ?? '');
}

export interface DataTableProps<T extends object>
  extends Omit<TableProps<T>, 'scroll'> {
  /**
   * Toolbar actions (e.g. "New", "Sync") rendered on the left of the
   * toolbar row, before the Filters toggle.
   */
  leading?: ReactNode;
  /**
   * FilterField elements. Their mere presence enables the Filters
   * toggle on the toolbar and the collapsible panel below it.
   */
  filters?: ReactNode;
  /**
   * Applied-filter change: Search submits the form values, Reset
   * submits `{}`. The applied filter is an attribute of THIS table
   * (DataTable state); FilterPanel only injects values here.
   */
  onFilterApply?: (values: Record<string, unknown>) => void;
  /**
   * Reload hook of the refresh button. Receives the currently applied
   * filter — the button is fully decoupled from FilterPanel; it only
   * triggers "reload the table under its current filter attribute".
   * Omitted (along with the button) when not provided.
   */
  onRefresh?: (values: Record<string, unknown>) => void;
  /**
   * Floor width of the one width-less column (the column that absorbs
   * leftover space under `tableLayout="fixed"`); added to `scroll.x`
   * so narrow screens scroll instead of squeezing it.
   */
  flexFloor?: number;
  /** Explicit `scroll.x` override (rare; the default is derived). */
  scrollX?: number | string;
  /**
   * Keys of the columns visible by default (all columns when absent).
   * The column-settings popover restores to this set.
   */
  defaultVisibleColumns?: string[];
  /**
   * Human labels for columns whose `title` is not a plain string
   * (e.g. a header with an embedded control), keyed by column key.
   */
  columnLabels?: Record<string, string>;
}

/**
 * Control bar — fixed-width cluster pinned to the right corner of the
 * toolbar row: refresh + column settings, joined as one capsule via
 * antd Space.Compact (first button rounded-left, last rounded-right,
 * inner corners square; compact styling is context-based, so the
 * Popover-wrapped settings button participates too). Refresh reads
 * the table's applied-filter attribute only; it knows nothing about
 * FilterPanel.
 */
function ControlBar({
  onRefresh,
  appliedFilter,
  columnOptions,
  visibleKeys,
  onVisibleChange,
  onRestore,
}: {
  onRefresh?: (values: Record<string, unknown>) => void;
  appliedFilter: Record<string, unknown>;
  columnOptions: { label: string; value: string }[];
  visibleKeys: string[];
  onVisibleChange: (keys: string[]) => void;
  onRestore: () => void;
}): ReactElement {
  const { t } = useTranslation();
  return (
    <Space.Compact>
      {onRefresh && (
        <Button
          icon={<ReloadOutlined />}
          aria-label={t('common.refresh')}
          onClick={() => onRefresh(appliedFilter)}
        />
      )}
      <Popover
        trigger="click"
        placement="bottomRight"
        content={
          <div style={{ minWidth: 160 }}>
            <Checkbox.Group
              value={visibleKeys}
              onChange={(vals) => onVisibleChange(vals as string[])}
              options={columnOptions}
              style={{ display: 'flex', flexDirection: 'column', gap: 8 }}
            />
            <Divider style={{ margin: '8px 0' }} />
            <Typography.Link onClick={onRestore}>
              {t('common.restore_default')}
            </Typography.Link>
          </div>
        }
      >
        <Button
          icon={<SettingOutlined />}
          aria-label={t('common.column_settings')}
        />
      </Popover>
    </Space.Compact>
  );
}

/**
 * The one standard table of the console. Every business list renders
 * through it so toolbar / filter / table geometry stays uniform.
 *
 * Three independent layers, composed by this component alone:
 *
 *   Toolbar (this row is owned ONLY by DataTable):
 *     [leading + Filters toggle] ......... [ControlBar, fixed width,
 *                                           pinned to the right corner]
 *   FilterPanel (pure panel, mounted below the toolbar when expanded):
 *     draft form values in, applied filter out via onFilterApply.
 *   Table: tableLayout="fixed", size="small", derived scroll.x,
 *     pagination pageSize 20, optional frozen action column.
 *
 * The APPLIED filter is an attribute of this table (`appliedFilter`
 * state): FilterPanel injects it, the ControlBar's refresh reads it —
 * the two never talk to each other.
 */
export function DataTable<T extends object>({
  leading,
  filters,
  onFilterApply,
  onRefresh,
  columns,
  flexFloor,
  scrollX,
  defaultVisibleColumns,
  columnLabels,
  tableLayout = 'fixed',
  size = 'small',
  pagination,
  ...tableProps
}: DataTableProps<T>): ReactElement {
  const { t } = useTranslation();

  // ---- Table attributes: expanded panel + applied filter -----------------

  const [expanded, setExpanded] = useState(false);
  const [appliedFilter, setAppliedFilter] = useState<Record<string, unknown>>(
    {},
  );
  const applyFilter = (values: Record<string, unknown>): void => {
    setAppliedFilter(values);
    onFilterApply?.(values);
  };

  // ---- Column visibility ---------------------------------------------------

  const allColumns = (columns ?? []) as ColumnType<never>[];
  const defaultKeys = useMemo(
    () => defaultVisibleColumns ?? allColumns.map((c) => columnKey(c)),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [defaultVisibleColumns, allColumns.length],
  );
  // null = "the default set" (so restore is a single setState(null)).
  const [visibleOverride, setVisibleOverride] = useState<string[] | null>(
    null,
  );
  const visibleKeys = visibleOverride ?? defaultKeys;
  const visibleColumns = useMemo(
    () => allColumns.filter((c) => visibleKeys.includes(columnKey(c))),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [columns, visibleKeys],
  );

  const columnOptions = useMemo(
    () =>
      allColumns.map((c) => {
        const key = columnKey(c);
        const label =
          typeof c.title === 'string'
            ? c.title
            : (columnLabels?.[key] ?? key);
        return { label, value: key };
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [columns, columnLabels],
  );

  // ---- Derived geometry ------------------------------------------------------

  const x =
    scrollX ??
    (() => {
      let sum = 0;
      let hasFlex = false;
      for (const col of visibleColumns) {
        const w = col.width;
        if (typeof w === 'number') sum += w;
        else hasFlex = true;
      }
      return sum + (hasFlex ? (flexFloor ?? DEFAULT_FLEX_FLOOR) : 0);
    })();

  // ---- Toolbar row (sole owner: this component) ------------------------------

  // antd icons so the arrow inherits the button's native icon spacing.
  const Arrow = expanded ? UpOutlined : DownOutlined;

  return (
    <>
      <div
        style={{
          marginBottom: 12,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
        }}
      >
        {leading}
        {filters !== undefined && (
          <Button onClick={() => setExpanded((v) => !v)}>
            {t('common.filters')}
            <Arrow />
          </Button>
        )}
        <div style={{ marginLeft: 'auto', flex: 'none' }}>
          <ControlBar
            onRefresh={onRefresh}
            appliedFilter={appliedFilter}
            columnOptions={columnOptions}
            visibleKeys={visibleKeys}
            onVisibleChange={setVisibleOverride}
            onRestore={() => setVisibleOverride(null)}
          />
        </div>
      </div>
      {filters !== undefined && expanded && (
        <FilterPanel
          onSearch={(values) => applyFilter(values)}
          onReset={() => applyFilter({})}
        >
          {filters}
        </FilterPanel>
      )}
      <Table<T>
        columns={visibleColumns as unknown as ColumnType<T>[]}
        tableLayout={tableLayout}
        size={size}
        pagination={pagination === undefined ? { pageSize: 20 } : pagination}
        scroll={{ x }}
        {...tableProps}
      />
    </>
  );
}
