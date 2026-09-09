import type { ReactElement, ReactNode } from 'react';
import { Space, Typography } from 'antd';

/**
 * Shared detail-page shell — header ONLY:
 *
 *   1. Header row — REQUIRED page title (fixed or dynamic) on the
 *      left, OPTIONAL page-level actions (one or more buttons) on
 *      the right.
 *   2. `children` — the ENTIRE page body as one free content slot.
 *      Callers compose their own sections (e.g. a Basic Info card
 *      built from `InfoDescriptions`, Raw Info, schedules, …) with
 *      no layout constraints.
 *
 * The Basic Info block is NOT part of this shell — it lives in the
 * standalone `InfoDescriptions` component, added by callers that
 * need it.
 */

export interface DetailLayoutProps {
  /** REQUIRED page title — fixed ("Product Detail") or dynamic. */
  title: ReactNode;
  /** OPTIONAL page-level actions rendered on the right of the header
      row (one or more buttons, tooltips, …). */
  actions?: ReactNode;
  /** Free page body below the header. */
  children?: ReactNode;
}

export function DetailLayout({
  title,
  actions,
  children,
}: DetailLayoutProps): ReactElement {
  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      {/* Header row — page title on the left, optional actions on the
          right. */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <Typography.Title level={4} style={{ margin: 0 }}>
          {title}
        </Typography.Title>
        {actions}
      </div>

      {/* Free content slot. */}
      {children}
    </Space>
  );
}
