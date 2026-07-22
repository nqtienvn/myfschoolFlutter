import { apiDownload, apiFetch } from './client';

export type AssessmentType = 'SCORE' | 'PASS_FAIL' | 'COMMENT';

export interface AdminGradeImportContext {
  academicYearId: number;
  academicYearName: string;
  semesterId: number;
  semesterName: string;
}

export interface AdminGradeImportItem {
  configItemId: number;
  occurrence: number;
  itemCode: string;
  displayName: string;
  assessmentType: AssessmentType;
}

export interface AdminGradeImportBatch {
  id: number;
  itemCode: string;
  itemName: string;
  fileName: string;
  totalRows: number;
  updatedScores: number;
  importedAt: string;
}

export interface AdminGradeImportSubject { id: number; code: string; name: string }
export interface AdminGradeImportCell { subjectId: number; score: number | null; comment: string | null; isGraded: boolean }
export interface AdminGradeImportRow {
  studentId: number;
  studentCode: string;
  studentName: string;
  classId: number;
  className: string;
  sourceOrder: number;
  cells: AdminGradeImportCell[];
}

export interface AdminGradeImportTable {
  batchId: number;
  itemCode: string;
  itemName: string;
  assessmentType: AssessmentType;
  subjects: AdminGradeImportSubject[];
  rows: AdminGradeImportRow[];
}

export interface AdminGradeImportResult {
  batchId: number;
  itemCode: string;
  itemName: string;
  totalRows: number;
  updatedScores: number;
}

export const getAdminGradeImportContext = () => apiFetch('/admin-grade-imports/context') as Promise<AdminGradeImportContext>;
export const getAdminGradeImportItems = () => apiFetch('/admin-grade-imports/items') as Promise<AdminGradeImportItem[]>;
export const downloadAdminGradeImportTemplate = (itemCode: string) => apiDownload(`/admin-grade-imports/template/${encodeURIComponent(itemCode)}`);
export const getAdminGradeImportBatches = () => apiFetch('/admin-grade-imports/batches') as Promise<AdminGradeImportBatch[]>;
export const getAdminGradeImportBatch = (batchId: number, classId?: number) => apiFetch(
  `/admin-grade-imports/batches/${batchId}${classId ? `?classId=${classId}` : ''}`,
) as Promise<AdminGradeImportTable>;

export function importAdminGradeFile(file: File) {
  const form = new FormData();
  form.append('file', file);
  return apiFetch('/admin-grade-imports/import', { method: 'POST', body: form }) as Promise<AdminGradeImportResult>;
}

export function updateAdminGradeImportRow(batchId: number, studentId: number, cells: Array<{ subjectId: number; value: string }>) {
  return apiFetch(`/admin-grade-imports/batches/${batchId}/students/${studentId}`, {
    method: 'PUT', body: JSON.stringify({ cells }),
  }) as Promise<AdminGradeImportTable>;
}
