import { describe, expect, it } from 'vitest';
import { gradeEntryPayload, numericAssessmentItems } from './gradeAssessment';

describe('grade assessment mapping', () => {
  it('keeps only numeric score data for SCORE', () => {
    expect(gradeEntryPayload('SCORE', 7, 8.5, 'ignored')).toEqual({
      studentId: 7,
      score: 8.5,
      comment: null,
      isGraded: true,
    });
  });

  it('maps PASS_FAIL and COMMENT through the comment field', () => {
    expect(gradeEntryPayload('PASS_FAIL', 7, 9, ' PASS ')).toEqual({
      studentId: 7,
      score: null,
      comment: 'PASS',
      isGraded: true,
    });
    expect(gradeEntryPayload('COMMENT', 7, null, '   ')).toEqual({
      studentId: 7,
      score: null,
      comment: null,
      isGraded: false,
    });
  });

  it('excludes non-numeric assessments from the average formula', () => {
    expect(numericAssessmentItems([
      { id: 1, assessmentType: 'SCORE' as const },
      { id: 2, assessmentType: 'PASS_FAIL' as const },
      { id: 3, assessmentType: 'COMMENT' as const },
    ])).toEqual([{ id: 1, assessmentType: 'SCORE' }]);
  });
});
