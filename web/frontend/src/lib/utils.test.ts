import { describe, expect, it } from 'vitest';
import {
  calculateAge,
  formatCurrency,
  formatDate,
  formatDateTime,
  humanise,
  initials,
  isToday,
  sumDecimalStrings,
} from './utils';

describe('formatCurrency', () => {
  it('formats decimal strings from the API without coercing them away', () => {
    expect(formatCurrency('1500.50')).toContain('1,500.50');
  });

  it('uses a placeholder for missing or invalid values', () => {
    expect(formatCurrency(null)).toBe('—');
    expect(formatCurrency('')).toBe('—');
    expect(formatCurrency('not-a-number')).toBe('not-a-number');
  });

  it('compacts large values when asked', () => {
    expect(formatCurrency('1250000.00', 'PKR', { compact: true })).toMatch(/1\.3M|1,250,000/);
  });
});

describe('sumDecimalStrings', () => {
  it('adds monetary strings to two decimals', () => {
    expect(sumDecimalStrings(['10.10', '20.20', null, '0.70'])).toBe('31.00');
  });

  it('treats an empty list as zero', () => {
    expect(sumDecimalStrings([])).toBe('0.00');
  });
});

describe('calculateAge', () => {
  it('returns whole years', () => {
    const today = new Date();
    const birth = new Date(today.getFullYear() - 30, today.getMonth(), today.getDate());
    expect(calculateAge(birth.toISOString().slice(0, 10))).toBe(30);
  });

  it('rejects implausible and missing dates', () => {
    expect(calculateAge(null)).toBeNull();
    expect(calculateAge('2999-01-01')).toBeNull();
    expect(calculateAge('nonsense')).toBeNull();
  });
});

describe('humanise and initials', () => {
  it('turns enum values into readable labels', () => {
    expect(humanise('PARTIALLY_PAID')).toBe('Partially paid');
    expect(humanise(null)).toBe('—');
  });

  it('builds at most two initials', () => {
    expect(initials('Ada Lovelace King')).toBe('AL');
    expect(initials(null)).toBe('—');
  });
});

describe('date helpers', () => {
  it('formats ISO timestamps and tolerates junk', () => {
    expect(formatDate('2026-03-15T09:00:00.000Z')).toMatch(/2026/);
    expect(formatDateTime('nonsense')).toBe('—');
  });

  it('detects today', () => {
    expect(isToday(new Date())).toBe(true);
    expect(isToday('2000-01-01T00:00:00.000Z')).toBe(false);
  });
});
