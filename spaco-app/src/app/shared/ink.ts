import { Ink } from '../core/api/models';

/** Label colors of each category ink (miel-spa-design INK). */
const INKS: Record<Ink, { ink: string; band: string; deep: string }> = {
  honey: { ink: 'var(--honey)', band: 'var(--navy)', deep: 'var(--deep-honey)' },
  lavender: { ink: 'var(--lavender)', band: 'var(--navy)', deep: 'var(--deep-lavender)' },
  sage: { ink: 'var(--sage)', band: 'var(--navy)', deep: 'var(--deep-sage)' },
  navy: { ink: 'var(--navy)', band: 'var(--gold)', deep: 'var(--navy)' },
  clay: { ink: 'var(--clay)', band: 'var(--wax)', deep: 'var(--clay)' }
};

export function inkOf(ink: string | null | undefined) {
  return INKS[(ink as Ink) ?? 'honey'] ?? INKS.honey;
}

/** Inline style that colors a label: --ink, --band-fg, --deep. */
export function inkStyle(ink: string | null | undefined): string {
  const k = inkOf(ink);
  return `--ink:${k.ink};--band-fg:${k.band};--deep:${k.deep}`;
}
