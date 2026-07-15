import type { AssessmentType } from '../api/gradeConfiguration';

export function gradeEntryPayload(
  assessmentType: AssessmentType,
  studentId: number,
  score: number | null,
  comment: string | null,
) {
  if (assessmentType === 'SCORE') {
    return {
      studentId,
      score,
      comment: null,
      isGraded: score !== null,
    };
  }
  const normalizedComment = comment?.trim() || null;
  return {
    studentId,
    score: null,
    comment: normalizedComment,
    isGraded: normalizedComment !== null,
  };
}

export function numericAssessmentItems<T extends { assessmentType: AssessmentType }>(items: T[]) {
  return items.filter(item => item.assessmentType === 'SCORE');
}
