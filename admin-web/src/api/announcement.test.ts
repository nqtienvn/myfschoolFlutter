import { describe, expect, it } from 'vitest';
import { buildRecipientQuery, recipientStats } from './announcement';

describe('announcement recipient tracking', () => {
  it('always scopes filters by the selected academic year', () => {
    const query = new URLSearchParams(buildRecipientQuery({
      academicYearId: '2026',
      classId: '12',
      role: 'PARENT',
      status: 'PENDING',
      keyword: '  Nguyễn An  ',
      page: 2,
      size: 25,
    }));

    expect(query.get('academicYearId')).toBe('2026');
    expect(query.get('classId')).toBe('12');
    expect(query.get('role')).toBe('PARENT');
    expect(query.get('status')).toBe('PENDING');
    expect(query.get('keyword')).toBe('Nguyễn An');
    expect(query.get('page')).toBe('2');
    expect(query.get('size')).toBe('25');
  });

  it('maps backend recipient statistics without deriving static values', () => {
    expect(recipientStats({ totalRecipients: 42, readCount: 31, acknowledgedCount: 20, repliedCount: 8 }))
      .toEqual({ total: 42, read: 31, acknowledged: 20, replied: 8 });
  });
});
