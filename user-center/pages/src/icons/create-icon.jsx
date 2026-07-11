import * as React from 'react';

import {cn} from '@/lib/utils';

/**
 * Factory for Lucide-compatible SVG icons.
 *
 * Mirrors the surface of `lucide-react`'s internal `createLucideIcon`
 * so anything produced here is a drop-in replacement for `Lucide*`
 * exports:
 *
 * - Same default props: `size=24`, `color='currentColor'`, `strokeWidth=2`
 * - Same defaults on the SVG element: `viewBox="0 0 24 24"`, `fill="none"`,
 *   `strokeLinecap="round"`, `strokeLinejoin="round"`. This is what lets
 *   downstream utility classes (Sidebar's `[&_svg]:size-4`, Button's
 *   destructive slot, DropdownMenuItem's `[&_svg]:size-4`, etc.) resize
 *   and recolor our icons the same way they do Lucide's.
 * - `absoluteStrokeWidth` support so stroke width can stay pixel-locked
 *   when the icon is scaled (matches Lucide semantics).
 * - `ref` is forwarded to the underlying `<svg>` for consumers that
 *   need it (Base UI `useRender` chains, focus management, etc.).
 * - `className` is merged through `cn()`; a stable `lucide lucide-<name>`
 *   class pair is emitted so third-party CSS targeting Lucide icons
 *   applies to our custom ones as well.
 *
 * `paths` is an array of `[tag, attributes]` tuples, exactly the shape
 * `lucide-react` uses internally (see the auto-generated `Icon.js`
 * exports). Keeping the same data structure lets us paste raw path
 * data from a design tool without wrapping every element in JSX.
 *
 * Usage:
 *   export const ShieldOAuth = createIcon('shield-oauth', [
 *     ['path', {d: '...'}],
 *     ['path', {d: '...'}],
 *   ]);
 */
export const createIcon = (iconName, paths) => {
    const Component = React.forwardRef(function LucideStyleIcon(
        {
            color = 'currentColor',
            size = 24,
            strokeWidth = 2,
            absoluteStrokeWidth,
            className,
            children,
            ...rest
        },
        ref,
    ) {
        return (
            <svg
                ref={ref}
                xmlns="http://www.w3.org/2000/svg"
                width={size}
                height={size}
                viewBox="0 0 24 24"
                fill="none"
                stroke={color}
                strokeWidth={
                    absoluteStrokeWidth
                        ? (Number(strokeWidth) * 24) / Number(size)
                        : strokeWidth
                }
                strokeLinecap="round"
                strokeLinejoin="round"
                className={cn('lucide', `lucide-${iconName}`, className)}
                {...rest}
            >
                {paths.map(([tag, attrs], i) =>
                    React.createElement(tag, {...attrs, key: i}),
                )}
                {children}
            </svg>
        );
    });

    // PascalCase display name from a kebab-case identifier so React
    // devtools show `ShieldOAuth`, matching Lucide's export naming.
    Component.displayName = iconName
        .split('-')
        .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
        .join('');

    return Component;
};
