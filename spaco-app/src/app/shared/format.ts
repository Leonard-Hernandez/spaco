/** es-CO formatting in the spa's timezone. */

export const TZ = 'America/Bogota';
const LOCALE = 'es-CO';

const cop = new Intl.NumberFormat(LOCALE, { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const timeFmt = new Intl.DateTimeFormat(LOCALE, { hour: 'numeric', minute: '2-digit', hour12: true, timeZone: TZ });
const dayFmt = new Intl.DateTimeFormat(LOCALE, { weekday: 'long', day: 'numeric', month: 'long', timeZone: TZ });
const shortFmt = new Intl.DateTimeFormat(LOCALE, { day: 'numeric', month: 'short', year: 'numeric', timeZone: TZ });
const stampFmt = new Intl.DateTimeFormat(LOCALE, { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit', hour12: true, timeZone: TZ });
const monthFmt = new Intl.DateTimeFormat(LOCALE, { month: 'long', year: 'numeric', timeZone: 'UTC' });

/** "$ 120.000", or "Consultar" while the spa has not published it. */
export function price(value: number | null | undefined): string {
  return value == null ? 'Consultar' : cop.format(value);
}

export function duration(min: number): string {
  return `${min} min`;
}

/** "9:30 a. m." from an ISO instant. */
export function time(instant: string): string {
  return timeFmt.format(new Date(instant));
}

/** "9:30 a. m." from a local "09:30:00". */
export function clock(local: string): string {
  const [h, m] = local.split(':').map(Number);
  const suffix = h < 12 ? 'a. m.' : 'p. m.';
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${h12}:${String(m).padStart(2, '0')} ${suffix}`;
}

/** "miércoles, 21 de octubre" from an ISO instant. */
export function longDay(instant: string): string {
  return capitalize(dayFmt.format(new Date(instant)));
}

/** Same, from a "2026-10-21" date. */
export function longDate(isoDate: string): string {
  return longDay(`${isoDate}T12:00:00-05:00`);
}

/** "21 oct 2026" */
export function shortDate(instant: string): string {
  return shortFmt.format(new Date(instant));
}

/** "21 oct, 3:40 p. m." */
export function stamp(instant: string): string {
  return stampFmt.format(new Date(instant));
}

/** "Octubre de 2026" for the first day of a month (UTC "2026-10-01"). */
export function monthTitle(isoDate: string): string {
  return capitalize(monthFmt.format(new Date(`${isoDate}T00:00:00Z`)));
}

/** Today in the spa's timezone, "2026-10-21". */
export function todayIso(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: TZ }).format(new Date());
}

/** "2026-10-21" + "09:30:00" in Bogotá (UTC-5, no DST) as an ISO instant. */
export function bogotaInstant(isoDate: string, local: string): string {
  return `${isoDate}T${local.length === 5 ? `${local}:00` : local}-05:00`;
}

/** "+57 314 809 4166" */
export function phone(e164: string | null | undefined): string {
  if (!e164) return '';
  const d = e164.replace(/\D/g, '');
  if (d.startsWith('57') && d.length === 12) {
    return `+57 ${d.slice(2, 5)} ${d.slice(5, 8)} ${d.slice(8)}`;
  }
  return `+${d}`;
}

/** "01" */
export function formula(no: number | null | undefined): string {
  return no == null ? '—' : String(no).padStart(2, '0');
}

/** Lowercase without accents, for search. */
export function fold(value: string): string {
  return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
}

export function capitalize(s: string): string {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

export const STATUS_LABEL: Record<string, string> = {
  PENDING: 'Por confirmar',
  ACCEPTED: 'Confirmada',
  REJECTED: 'No aceptada',
  CANCELLED: 'Cancelada',
  COMPLETED: 'Atendida',
  NO_SHOW: 'No asistió',
  DRAFT: 'Borrador',
  PUBLISHED: 'Publicado',
  HIDDEN: 'Oculto'
};

export const CONSENT_LABEL: Record<string, string> = {
  DATA_PROCESSING: 'Tratamiento de datos personales',
  TREATMENT: 'Consentimiento informado del tratamiento',
  PHOTO_TESTIMONIAL: 'Publicar mis fotos y testimonio'
};

export const PHOTO_LABEL: Record<string, string> = { BEFORE: 'Antes', AFTER: 'Después', PROGRESS: 'Evolución' };
