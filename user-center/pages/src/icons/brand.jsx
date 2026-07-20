import * as React from 'react';

import {cn} from '@/lib/utils';

/**
 * Brand — Euler Project brand logo as a Lucide-compatible icon.
 *
 * A solid disc with a snail-shell spiral punched out of it. The disc
 * fills the entire 24×24 viewBox (r = 12, edge-to-edge — same coord
 * system as every Lucide icon, so `size-4`/`size-5` classnames behave
 * identically here) and paints with `currentColor` so the mark
 * inherits the surrounding text colour — swap the CSS `color` on any
 * ancestor and the whole brand follows (light/dark theming, `:hover`
 * escalation, sidebar accent tints, etc.).
 *
 * The negative-space curve is a proper logarithmic-spiral snail shell.
 * Four arcs of radii 8φ → 8 → 5 → 3, every step shrinking by exactly
 * 1/φ (a genuine geometric progression, not an approximation) — a
 * faithful discrete sampling of `r = a·e^(θ/2π)`, Euler's namesake
 * growth law encoded in the shell geometry. Threading
 *
 *     (11.91,24) → (20,12) → (4,12) → (14,12) → (8,12)
 *
 * Segments 1–3 are complete half-circles; segment 0 is the outward
 * extension whose r = 8φ ≈ 12.94 circle overshoots the r=12 disc, so
 * it's truncated at the exact circle-circle intersection with the
 * disc boundary. Solving that intersection analytically gives
 * x = 20 − 5φ ≈ 11.9098 at y ≈ 24; segment 0 is only ~68° of arc
 * rather than a half-turn, precisely because the disc geometry cuts
 * the log-spiral short — the spiral doesn't stop by convention, it
 * stops where the mathematics of the two circles say it must.
 *
 * The spiral also deliberately terminates on the inner side at r=3
 * rather than curling all the way to (12,12): the tail rests ~4 px
 * shy of centre, leaving an airy core so the innermost turns don't
 * smear together at UI display sizes.
 *
 * All four arc joints are tangent-continuous — segment 0's centre
 * (20 − 8φ, 12) sits on the y=12 axis just like the other three, so
 * the tangent at every seam is vertical and the mark reads as one
 * smooth curve. The stroke's round line-cap at the (11.91, 24) start
 * extends beyond the disc boundary and is naturally clipped by the
 * mask, so the tail dissolves into the disc rim precisely where the
 * spiral intersects it — no visible "stop" mark.
 *
 * The mask uses SVG's "white keeps, black erases" convention:
 *
 *   1. A white disc (r = 12) preserves the entire painted area.
 *   2. A black stroked spiral drawn over it carves the curve out.
 *
 * `React.useId()` mints a component-instance-unique DOM id so several
 * <Brand/> instances on the same page do not collide on `url(#…)`
 * references — the first-declared `<mask id="…">` would otherwise win
 * and every duplicate icon would silently reference it.
 *
 * Path coordinates, viewBox, stroke width and line caps are inherited
 * verbatim from the reference implementation on the Euler Project
 * homepage (see `euler-projects.github.io/index.html`, `#brand` <svg>).
 * Do not tweak them here without updating the homepage brand in the
 * same commit or the two marks will drift apart.
 */
const Brand = React.forwardRef(function Brand(
    {className, size = 24, ...rest},
    ref,
) {
    const maskId = React.useId();
    return (
        <svg
            ref={ref}
            xmlns="http://www.w3.org/2000/svg"
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="currentColor"
            className={cn('lucide', 'lucide-euler-brand', className)}
            {...rest}
        >
            <defs>
                <mask id={maskId}>
                    <circle cx="12" cy="12" r="12" fill="white"/>
                    <path
                        d="M 11.91 24 A 12.94 12.94 0 0 0 20 12 A 8 8 0 1 0 4 12 A 5 5 0 1 0 14 12 A 3 3 0 1 0 8 12"
                        fill="none"
                        stroke="black"
                        strokeWidth={2}
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    />
                </mask>
            </defs>
            <circle cx="12" cy="12" r="12" fill="currentColor" mask={`url(#${maskId})`}/>
        </svg>
    );
});

Brand.displayName = 'Brand';

export {Brand};
