import {createIcon} from './create-icon';

/**
 * ShieldLock — Lucide-style shield outline with a padlock carved
 * inside. Pairs the OAuth 2.0 shield silhouette with a closed padlock
 * to signal "secured / authorized access", staying at 24×24 with 2px
 * stroke so it sits alongside other `Shield*` variants without any
 * visual jitter.
 *
 * Geometry:
 * - Path 1: the Lucide `Shield` outline verbatim (24×24 viewBox,
 *   round joins, 2px stroke). Reusing it guarantees pixel-perfect
 *   parity with `Shield`, `ShieldCheck`, `ShieldOAuth`, etc. When
 *   Lucide revises the shield silhouette, update this path AND the
 *   identical one in `shield-oauth.jsx` together.
 * - Path 2 + 3: the Lucide `lock` glyph (body `rect x=3 y=11 w=18
 *   h=11 rx=2`, shackle `M7 11V7a5 5 0 0 1 10 0v4`) scaled uniformly
 *   by 4/9 (~0.444) about its own centre and re-centred on the
 *   shield's inner space (apex x=12). The uniform factor preserves
 *   the padlock's native proportions — body 8×5, shackle chord 4.4,
 *   leg inset 1.8 — so it reads as a genuine Lucide lock rather than a
 *   redrawn approximation.
 */
export const ShieldLock = createIcon('shield-lock', [
    [
        'path',
        {
            d: 'M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z',
        },
    ],
    ['rect', {x: 8, y: 11.5, width: 8, height: 5, rx: 1}],
    ['path', {d: 'M9.8 11.5V9.8a2.2 2.2 0 0 1 4.4 0V11.5'}],
]);
