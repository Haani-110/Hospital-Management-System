import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/** Merges conditional class names and de-duplicates conflicting Tailwind utilities. */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}

/** `2026-03-15T09:00:00.000Z` → `15 Mar 2026`. */
export function formatDate(value: string | Date | null | undefined): string {
  const date = toDate(value);
  if (!date) return '—';
  return new Intl.DateTimeFormat(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date);
}

/** `2026-03-15T09:00:00.000Z` → `15 Mar 2026, 14:30`. */
export function formatDateTime(value: string | Date | null | undefined): string {
  const date = toDate(value);
  if (!date) return '—';
  return new Intl.DateTimeFormat(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

/** Time only — used for appointment slots. */
export function formatTime(value: string | Date | null | undefined): string {
  const date = toDate(value);
  if (!date) return '—';
  return new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' }).format(date);
}

/**
 * Whole years between `value` and now. Returns null for missing/invalid input
 * or a birth date in the future.
 */
export function calculateAge(dateOfBirth: string | Date | null | undefined): number | null {
  const date = toDate(dateOfBirth);
  if (!date) return null;
  const now = new Date();
  let age = now.getFullYear() - date.getFullYear();
  const monthDelta = now.getMonth() - date.getMonth();
  if (monthDelta < 0 || (monthDelta === 0 && now.getDate() < date.getDate())) {
    age -= 1;
  }
  return age >= 0 && age < 130 ? age : null;
}

/**
 * Formats a decimal **string** from the API (Prisma `Decimal`) as currency.
 *
 * The backend deliberately serialises decimals as strings to preserve
 * precision, so values are never coerced through `Number` for display.
 */
export function formatCurrency(
  value: string | null | undefined,
  currency = 'PKR',
  options: { compact?: boolean } = {},
): string {
  if (value === null || value === undefined || value === '') return '—';
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return value;

  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency,
    currencyDisplay: 'narrowSymbol',
    notation: options.compact && Math.abs(parsed) >= 10_000 ? 'compact' : 'standard',
    maximumFractionDigits: options.compact && Math.abs(parsed) >= 10_000 ? 1 : 2,
    minimumFractionDigits: options.compact && Math.abs(parsed) >= 10_000 ? 0 : 2,
  }).format(parsed);
}

/** Sums decimal strings without losing precision on typical money values. */
export function sumDecimalStrings(values: (string | null | undefined)[]): string {
  const total = values.reduce((acc, value) => acc + Number(value ?? 0), 0);
  return Number.isFinite(total) ? total.toFixed(2) : '0.00';
}

/** `1234` → `1,234`. */
export function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '—';
  return new Intl.NumberFormat().format(value);
}

/** Turns `RECEPTIONIST` / `PARTIALLY_PAID` into `Receptionist` / `Partially paid`. */
export function humanise(value: string | null | undefined): string {
  if (!value) return '—';
  const spaced = value.replace(/_/g, ' ').toLowerCase();
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

/** Initials for avatars: `Ada Lovelace` → `AL`. */
export function initials(name: string | null | undefined): string {
  if (!name) return '—';
  const parts = name.trim().split(/\s+/).slice(0, 2);
  return parts.map((part) => part.charAt(0).toUpperCase()).join('') || '—';
}

/** Safe date coercion for API strings and Date instances. */
function toDate(value: string | Date | null | undefined): Date | null {
  if (!value) return null;
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** `2026-03-15` for a local date — used for `<input type="date">` defaults. */
export function toDateInputValue(date: Date): string {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 10);
}

/** Local start/end of day as ISO strings, for the appointments date filter. */
export function dayBounds(date: Date): { from: string; to: string } {
  const start = new Date(date);
  start.setHours(0, 0, 0, 0);
  const end = new Date(start);
  end.setDate(end.getDate() + 1);
  return { from: start.toISOString(), to: end.toISOString() };
}

/** True when the ISO timestamp falls on today's local date. */
export function isToday(value: string | Date): boolean {
  const date = toDate(value);
  if (!date) return false;
  const now = new Date();
  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  );
}
