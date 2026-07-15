import { describe, expect, it } from 'vitest';
import { buildMonitoringQuery } from './homeroomMonitoring';

describe('homeroom monitoring academic-year scope', () => {
  it('always carries the selected year and semester through report filters', () => {
    const query = new URLSearchParams(buildMonitoringQuery({
      academicYearId: 2027,
      semesterId: 18,
      classId: 7,
      gradeLevel: 12,
      status: 'OPEN',
    }));
    expect(query.get('academicYearId')).toBe('2027');
    expect(query.get('semesterId')).toBe('18');
    expect(query.get('classId')).toBe('7');
    expect(query.get('gradeLevel')).toBe('12');
    expect(query.get('status')).toBe('OPEN');
  });

  it('does not retain optional filters after the selected context is reset', () => {
    const query = new URLSearchParams(buildMonitoringQuery({ academicYearId: 30, semesterId: 4 }));
    expect(query.get('classId')).toBeNull();
    expect(query.get('gradeLevel')).toBeNull();
    expect(query.get('status')).toBeNull();
  });
});
