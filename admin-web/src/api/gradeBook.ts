import { apiFetch } from './client';
export const getGradeBook = (classId:number,subjectId:number,semesterId:number) => apiFetch(`/grade-books?classId=${classId}&subjectId=${subjectId}&semesterId=${semesterId}`);
export const getGradeBookStudents = (bookId:number) => apiFetch(`/grade-books/${bookId}/students`);
export const updateScores = (gradeItemId:number, entries:any[], reason:string) => apiFetch('/grade-books/scores',{method:'PUT',body:JSON.stringify({gradeItemId,entries,reason})});
export const changeGradeBookStatus = (bookId:number,status:'PUBLISHED'|'LOCKED') => apiFetch(`/grade-books/${bookId}/status/${status}`,{method:'POST'});
