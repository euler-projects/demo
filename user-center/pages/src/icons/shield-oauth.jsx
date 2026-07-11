import {createIcon} from './create-icon';

/**
 * ShieldOAuth — Lucide-style shield outline with the letter "A"
 * carved inside. Aligns with the OAuth 2.0 official mark (shield +
 * inner letter shape) while staying at 24×24 with 2px stroke so it
 * sits alongside other `Shield*` variants without any visual jitter.
 *
 * Geometry:
 * - Path 1: the Lucide `Shield` outline verbatim (24×24 viewBox,
 *   round joins, 2px stroke). Reusing it guarantees pixel-perfect
 *   parity with `Shield`, `ShieldCheck`, `ShieldEllipsis`, etc.
 * - Path 2: the "A" legs, drawn as one continuous polyline from the
 *   bottom-left foot up to the apex and back down to the bottom-right
 *   foot. Round joins from the SVG defaults give it a soft peak.
 * - Path 3: the horizontal crossbar of the "A".
 *
 * Letter coordinates were picked so the A sits centered horizontally
 * (apex x=12) and vertically within the shield's inner space, with a
 * one-unit margin from the shield edge on each side — same visual
 * weight as the check mark in `ShieldCheck`.
 */
export const ShieldOAuth = createIcon('shield-oauth', [
    [
        'path',
        {
            d: 'M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z',
        },
    ],
    ['path', {d: 'M9 16.5 12 7.5 15 16.5'}],
    ['path', {d: 'M10 14h4'}],
]);
