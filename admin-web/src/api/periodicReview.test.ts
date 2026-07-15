import { describe, expect, it } from 'vitest';
import { buildPeriodicReportQuery } from './periodicReview';

describe('periodic report academic year scope', () => {
  it('always sends selected academic year and semester', () => {
    const query = new URLSearchParams(buildPeriodicReportQuery({
      academicYearId: 2027,
      semesterId: 18,
      classId: 7,
      status: 'PUBLISHED',
    }));
    expect(query.get('academicYearId')).toBe('2027');
    expect(query.get('semesterId')).toBe('18');
    expect(query.get('classId')).toBe('7');
    expect(query.get('status')).toBe('PUBLISHED');
  });
});
