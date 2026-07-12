import { apiFetch } from './client';

export type GradeEntryRole = 'SUBJECT_TEACHER' | 'ADMIN' | 'SUBJECT_TEACHER_AND_ADMIN';
export type AssessmentType = 'SCORE' | 'PASS_FAIL' | 'COMMENT';
export interface GradeConfigItem { id?: number; code: string; displayName: string; weight: number; quantity: number; entryRole: GradeEntryRole; assessmentType: AssessmentType; requiredEntry: boolean; displayOrder: number; }
export interface GradeConfig { id: number; name: string; version: number; academicYearId?: number; status: string; items: GradeConfigItem[]; }
export const getGradeTemplates = () => apiFetch('/grade-configurations/templates') as Promise<GradeConfig[]>;
export const getYearGradeConfig = (yearId: string | number) => apiFetch(`/grade-configurations/academic-years/${yearId}`) as Promise<GradeConfig>;
export const createGradeTemplate = (name: string, items: GradeConfigItem[]) => apiFetch('/grade-configurations/templates', { method: 'POST', body: JSON.stringify({ name, items }) }) as Promise<GradeConfig>;
