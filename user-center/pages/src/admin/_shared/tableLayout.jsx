/* eslint-disable react-refresh/only-export-components */
import React, {useMemo} from 'react';
import {Space, Tag, Popover, Dropdown, Button, Typography} from 'antd';
import {MoreOutlined} from '@ant-design/icons';

// Width budget for a row-action column. Frozen on the right so this is
// authoritative when distributing actions between the visible row and
// the overflow menu.
export const ACTION_COLUMN_WIDTH = 200;

// antd Table cell horizontal padding (16 * 2).
const CELL_PADDING = 32;
// <Space size="small"> between sibling action links.
const ACTION_SPACING = 8;
// Icon-only text button used as the overflow trigger.
const MORE_BUTTON = 24;
// antd Tag horizontal padding plus border.
const TAG_INNER_PADDING = 16;
// Gap between adjacent tags (also drives the +N badge spacing).
const TAG_GAP = 6;

const _measureCanvas = typeof document !== 'undefined' ? document.createElement('canvas') : null;
const _measureCtx = _measureCanvas?.getContext('2d');
const MEASURE_FONT = '14px -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, Avenir, Helvetica, Arial, sans-serif';

/**
 * Cheap, DOM-free text width measurement that shares a single canvas
 * across the application. Falls back to a character-count heuristic
 * when running outside a browser (SSR / tests).
 */
export function measureLabelWidth(text) {
    if (!_measureCtx) return (text || '').length * 14;
    _measureCtx.font = MEASURE_FONT;
    return _measureCtx.measureText(text || '').width;
}

/**
 * Compute the row-action column width that exactly fits what will be
 * rendered, capped at `maxWidth`. Delegates packing to {@link splitActions}
 * so the column hugs the visible cell whether the full set fits inline
 * or some actions collapse into the kebab.
 *
 * Pass the widest stable label per slot for toggle-style actions to
 * avoid horizontal jitter when row state changes.
 */
export function computeActionsColumnWidth(labels, maxWidth = ACTION_COLUMN_WIDTH) {
    if (!labels || labels.length === 0) return CELL_PADDING;
    const blueprint = labels.map((label, i) => ({key: i, label}));
    const {visible, overflow} = splitActions(blueprint, maxWidth);
    const visibleWidth = visible.reduce(
        (sum, a, i) => sum + measureLabelWidth(a.label) + (i > 0 ? ACTION_SPACING : 0),
        0
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
export function splitActions(actions, columnWidth = ACTION_COLUMN_WIDTH) {
    const available = Math.max(0, columnWidth - CELL_PADDING);
    const total = actions.reduce(
        (sum, a, i) => sum + measureLabelWidth(a.label) + (i > 0 ? ACTION_SPACING : 0),
        0
    );
    if (total <= available) {
        return {visible: actions, overflow: []};
    }
    const reservedForMore = MORE_BUTTON + ACTION_SPACING;
    const visibleBudget = available - reservedForMore;
    const visible = [];
    let used = 0;
    for (const action of actions) {
        const w = measureLabelWidth(action.label) + (visible.length > 0 ? ACTION_SPACING : 0);
        if (used + w <= visibleBudget) {
            visible.push(action);
            used += w;
        } else {
            break;
        }
    }
    if (visible.length === 0) visible.push(actions[0]);
    return {visible, overflow: actions.slice(visible.length)};
}

/**
 * Decide how many tags fit before collapsing the rest into a +N badge.
 * Mirrors splitActions but accounts for the fixed Tag chrome.
 */
export function splitTagsByWidth(items, columnWidth) {
    const available = Math.max(0, columnWidth - CELL_PADDING);
    let used = 0;
    let visibleCount = 0;
    for (let i = 0; i < items.length; i++) {
        const w = measureLabelWidth(items[i].label) + TAG_INNER_PADDING + (i > 0 ? TAG_GAP : 0);
        if (used + w <= available) {
            used += w;
            visibleCount++;
        } else {
            break;
        }
    }
    if (visibleCount === items.length) return visibleCount;
    const plusLabel = `+${items.length - visibleCount}`;
    const plusW = measureLabelWidth(plusLabel) + TAG_INNER_PADDING + (visibleCount > 0 ? TAG_GAP : 0);
    while (visibleCount > 0 && used + plusW > available) {
        visibleCount--;
        used -= measureLabelWidth(items[visibleCount].label) + TAG_INNER_PADDING + (visibleCount > 0 ? TAG_GAP : 0);
    }
    if (visibleCount === 0 && items.length > 0) visibleCount = 1;
    return visibleCount;
}

/**
 * Renders a sequence of Tags that auto-collapse into a +N badge with a
 * hover popover. Convention follows GitHub labels / Linear status: a
 * numeric overflow badge rather than the action-column kebab icon.
 */
export function OverflowTags({items, columnWidth}) {
    if (!items || items.length === 0) return null;
    const visibleCount = splitTagsByWidth(items, columnWidth);
    const visible = items.slice(0, visibleCount);
    const overflow = items.slice(visibleCount);
    return (
        <Space size={TAG_GAP} wrap={false} style={{display: 'inline-flex'}}>
            {visible.map((item) => (
                <Tag key={item.key} color={item.color} style={{margin: 0}}>{item.label}</Tag>
            ))}
            {overflow.length > 0 && (
                <Popover
                    placement="top"
                    content={
                        <Space size={[TAG_GAP, TAG_GAP]} wrap style={{maxWidth: 240}}>
                            {overflow.map((item) => (
                                <Tag key={item.key} color={item.color} style={{margin: 0}}>{item.label}</Tag>
                            ))}
                        </Space>
                    }
                >
                    <Tag style={{margin: 0, cursor: 'pointer'}}>+{overflow.length}</Tag>
                </Popover>
            )}
        </Space>
    );
}

/**
 * Renders a row-action cell that adapts to the current locale: actions
 * that fit are displayed inline, the rest collapse behind a kebab menu.
 *
 * @param actions       full action list including handlers, in priority
 *                      order (most important first)
 * @param columnWidth   width budget for layout calculation
 * @param moreLabel     accessible label for the kebab button
 * @param measureLabels optional map keyed by action.key, providing a
 *                      stable label string used only for layout
 *                      measurement; useful when an action's actual
 *                      label changes per row (e.g. enable/disable
 *                      toggle) to avoid layout jitter
 */
export function RowActions({actions, columnWidth = ACTION_COLUMN_WIDTH, moreLabel = 'More', measureLabels}) {
    const layout = useMemo(() => {
        const blueprint = actions.map((a) => ({
            key: a.key,
            label: measureLabels && measureLabels[a.key] != null ? measureLabels[a.key] : a.label,
        }));
        return splitActions(blueprint, columnWidth);
    }, [actions, columnWidth, measureLabels]);

    const byKey = useMemo(
        () => Object.fromEntries(actions.map((a) => [a.key, a])),
        [actions]
    );

    return (
        <Space size="small" style={{whiteSpace: 'nowrap'}}>
            {layout.visible.map((slot) => {
                const action = byKey[slot.key];
                if (!action) return null;
                return (
                    <Typography.Link
                        key={action.key}
                        type={action.danger ? 'danger' : undefined}
                        disabled={action.disabled}
                        style={{whiteSpace: 'nowrap'}}
                        onClick={action.onClick}
                    >
                        {action.label}
                    </Typography.Link>
                );
            })}
            {layout.overflow.length > 0 && (
                <Dropdown
                    menu={{
                        items: layout.overflow
                            .map((slot) => byKey[slot.key])
                            .filter(Boolean)
                            .map((action) => ({
                                key: action.key,
                                label: action.label,
                                danger: action.danger,
                                disabled: action.disabled,
                                onClick: action.onClick,
                            })),
                    }}
                    trigger={['click']}
                    placement="bottomRight"
                >
                    <Button
                        type="text"
                        size="small"
                        icon={<MoreOutlined/>}
                        aria-label={moreLabel}
                        onClick={(e) => e.preventDefault()}
                    />
                </Dropdown>
            )}
        </Space>
    );
}
