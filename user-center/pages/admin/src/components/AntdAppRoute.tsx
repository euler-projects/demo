import type { ReactElement } from 'react';
import { App as AntdApp } from 'antd';
import { Outlet } from 'react-router';

/**
 * Pathless layout route supplying antd's `<App>` context to every
 * descendant route. Pages and their shared sub-components call
 * `App.useApp()` (message/modal/notification holders) without wrapping
 * themselves — the route table decides the antd boundary by which
 * routes sit inside this node.
 *
 * Keep pure shadcn routes OUTSIDE: antd `<App>` injects unlayered
 * global rules (e.g. an `a` color) that would pollute shadcn styling.
 */
export function AntdAppRoute(): ReactElement {
  return (
    <AntdApp>
      <Outlet />
    </AntdApp>
  );
}
