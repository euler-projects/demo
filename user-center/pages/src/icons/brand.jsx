import * as React from 'react';

import {cn} from '@/lib/utils';

/**
 * Brand — Euler Project brand logo as a Lucide-compatible icon.
 *
 * The geometry is a stylised π (pi) mark: a thick arc + thin chord
 * form the semicircle, and a filled Q1 sector completes the symbol.
 *
 * Path coordinates, viewBox and stroke widths are inherited verbatim
 * from the reference implementation on the Euler Project homepage
 * (see `euler-projects.github.io/index.html`, `#brand` <svg>). Both
 * files use radius 13 in a 30×30 viewBox so the outer stroke reaches
 * ~99% of the canvas (“hug-edges” fill), matching — and in fact
 * exceeding — the visual density of Lucide/shadcn icons. Stroke
 * widths (thick 3.64, thin 1.3) preserve the 2.8 : 1 contrast of the
 * original design at the new radius; do not tweak them here without
 * updating the homepage brand in the same commit or the two marks
 * will drift apart.
 *
 * The icon uses `currentColor` for both fill and stroke — it inherits
 * the parent's text colour, which in the sidebar header is
 * `text-sidebar-primary-foreground` on a `bg-sidebar-primary` tile.
 *
 * Line caps and joins deliberately stay at SVG defaults (`butt` +
 * `miter`) rather than Lucide's `round`/`round`. The original
 * navbar-brand mark on the Euler homepage uses flat endpoints on
 * the thick outer arc — rounding them here would soften the
 * silhouette and drift away from the reference brand.
 */
const Brand = React.forwardRef(function Brand(
    {className, size = 24, ...rest},
    ref,
) {
    return (
        <svg
            ref={ref}
            xmlns="http://www.w3.org/2000/svg"
            width={size}
            height={size}
            viewBox="0 0 30 30"
            fill="none"
            stroke="currentColor"
            className={cn('lucide', 'lucide-euler-brand', className)}
            {...rest}
        >
            {/* Thick outer arc: from 0° (on crossbar) CCW 330° → gap at Q4 */}
            <path
                d="M 28 15 A 13 13 0 1 0 26.26 21.5"
                fill="none"
                stroke="currentColor"
                strokeWidth={3.75}
            />
            {/* Thin diameter chord: 30° (Q1) → 210° (Q3), forms π semi-circle with upper arc */}
            <path
                d="M 26.26 8.5 L 3.74 21.5"
                fill="none"
                stroke="currentColor"
                strokeWidth={2}
            />
            {/* Q1 filled sector: 30°, symmetric to Q4 gap */}
            <path
                d="M 15 15 L 28 15 A 13 13 0 0 0 26.26 8.5 Z"
                fill="currentColor"
                stroke="none"
            />
        </svg>
    );
});

Brand.displayName = 'Brand';

export {Brand};
