import { clock } from './format';

/** Merged opening blocks of one weekday, as the API sends them (ISO weekday 1 = Monday ... 7 = Sunday, "HH:mm"). */
export interface OpeningDay {
  weekday: number;
  blocks: { start: string; end: string }[];
}

const DAY = ['', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

/** "Lun", "Lun y mar" or "Lun a vie" for a run of consecutive days. */
function dayRange(first: number, last: number): string {
  const lower = (d: number) => DAY[d].toLowerCase();
  if (first === last) return DAY[first];
  if (last === first + 1) return `${DAY[first]} y ${lower(last)}`;
  return `${DAY[first]} a ${lower(last)}`;
}

/**
 * Compact weekly hours, e.g. ["Lun y mar: 1:00 p. m. – 6:00 p. m.", "Mié a dom: 8:00 a. m. – 2:00 p. m."].
 * Consecutive days with identical blocks are grouped. A closed stretch is listed only when it sits
 * between open days ("Jue: cerrado"); a closed week end is already implied by the days listed.
 */
export function openingHoursLines(days: OpeningDay[]): string[] {
  const byDay = new Map<number, string>();
  for (const d of days) {
    if (d.weekday < 1 || d.weekday > 7 || !d.blocks.length) continue;
    byDay.set(d.weekday, d.blocks.map(b => `${clock(b.start)} – ${clock(b.end)}`).join(', '));
  }
  if (!byDay.size) return [];

  const groups: { first: number; last: number; text: string | null }[] = [];
  for (let d = 1; d <= 7; d++) {
    const text = byDay.get(d) ?? null;
    const last = groups.at(-1);
    if (last && last.text === text) last.last = d;
    else groups.push({ first: d, last: d, text });
  }
  const firstOpen = groups.findIndex(g => g.text);
  const lastOpen = groups.length - 1 - [...groups].reverse().findIndex(g => g.text);
  return groups
    .slice(firstOpen, lastOpen + 1)
    .map(g => `${dayRange(g.first, g.last)}: ${g.text ?? 'cerrado'}`);
}
