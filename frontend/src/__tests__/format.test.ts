import { describe, it, expect } from 'vitest';
import {
  formatNumber,
  formatPercent,
  humanizeCode,
  formatBytes,
  truncate,
  filenameFromDisposition,
} from '@/lib/format';

describe('format utilities', () => {
  it('formats numbers with Indian locale grouping', () => {
    expect(formatNumber(1000)).toBe('1,000');
    expect(formatNumber(100000)).toBe('1,00,000');
    expect(formatNumber(null)).toBe('—');
  });

  it('calculates percentages correctly', () => {
    expect(formatPercent(50, 100)).toBe('50%');
    expect(formatPercent(1, 3)).toBe('33%');
    expect(formatPercent(0, 0)).toBe('—');
  });

  it('humanizes enum codes', () => {
    expect(humanizeCode('INSTITUTION_ADMIN')).toBe('Institution admin');
    expect(humanizeCode('AICTE_OFFICER')).toBe('Aicte officer');
    expect(humanizeCode(null)).toBe('Unknown');
  });

  it('formats byte sizes', () => {
    expect(formatBytes(500)).toBe('500 B');
    expect(formatBytes(1024)).toBe('1.0 KB');
    expect(formatBytes(1048576 * 5)).toBe('5.0 MB');
  });

  it('truncates text safely', () => {
    expect(truncate('Hello world', 5)).toBe('Hell…');
    expect(truncate('Hello', 10)).toBe('Hello');
  });

  it('extracts filename from content disposition header', () => {
    expect(
      filenameFromDisposition(
        'attachment; filename="enrolments_2026.csv"',
        'fallback.csv'
      )
    ).toBe('enrolments_2026.csv');
    expect(filenameFromDisposition(undefined, 'fallback.csv')).toBe('fallback.csv');
  });
});
