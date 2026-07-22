import { apiFetch } from './client';
export const getGradeBook = (classId:number,subjectId:number,semesterId:number) => apiFetch(`/grade-books?classId=${classId}&subjectId=${subjectId}&semesterId=${semesterId}`);
export const getGradeBookStudents = (bookId:number) => apiFetch(`/grade-books/${bookId}/students`);
export function getGradeComponentOverview(academicYearId: number, semesterId: number, classId?: number, subjectId?: number) {
  const query = new URLSearchParams({ academicYearId: String(academicYearId), semesterId: String(semesterId) });
  if (classId) query.set('classId', String(classId));
  if (subjectId) query.set('subjectId', String(subjectId));
  return apiFetch(`/grade-books/component-overview?${query}`);
}
export const updateScores = (gradeItemId:number, entries:any[], reason:string) => apiFetch('/grade-books/scores',{method:'PUT',body:JSON.stringify({gradeItemId,entries,reason})});
export const lockGradeBook = (bookId:number) => apiFetch(`/grade-books/${bookId}/status/LOCKED`,{method:'POST'});
export const calculateSubjectAverages = (bookId:number) => apiFetch(`/grade-books/${bookId}/calculate`,{method:'POST'});
export const calculateSemesterResults = (classId:number,semesterId:number) => apiFetch('/semester-results/calculate',{method:'POST',body:JSON.stringify({classId,semesterId})});
export const calculateSchoolSemesterResults = (academicYearId:number,semesterId:number) => apiFetch('/semester-results/calculate-school',{method:'POST',body:JSON.stringify({academicYearId,semesterId})});
