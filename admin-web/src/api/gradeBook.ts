import { apiFetch } from './client';

export async function getGradeBook(classId: number | string, subjectId: number | string, semesterId: number | string) {
  return apiFetch(`/grade-books?classId=${classId}&subjectId=${subjectId}&semesterId=${semesterId}`);
}

export async function getGradeBookStudents(gradeBookId: number) {
  return apiFetch(`/grade-books/${gradeBookId}/students`);
}

export interface ScoreEntry {
  studentId: number;
  score?: number;
  isGraded: boolean;
  note?: string;
  isCommentBased: boolean;
  comment?: string;
}

export async function saveScores(gradeItemId: number, entries: ScoreEntry[]) {
  return apiFetch('/grade-books/scores', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ gradeItemId, entries })
  });
}

export async function finalizeGradeBook(gradeBookId: number) {
  return apiFetch(`/grade-books/${gradeBookId}/finalize`, {
    method: 'POST'
  });
}
