import { apiFetch } from './client';

export interface CreateStudentEnrollmentRequest {
  academicYearId: number;
  classId: number;
  studentCode: string;
  studentName: string;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  studentAddress?: string;
  studentCitizenId?: string;
  parentName: string;
  relationship: 'FATHER' | 'MOTHER' | 'GUARDIAN';
  parentPhone: string;
  parentEmail?: string;
  parentCitizenId?: string;
  parentOccupation?: string;
  parentAddress?: string;
}

export interface StudentEnrollmentResult {
  studentId: number;
  studentCode: string;
  classId: number;
  className: string;
  parentId: number;
  parentReused: boolean;
}

export interface StudentAccountByClass {
  studentId: number;
  studentCode: string;
  studentName: string;
  studentUsername: string;
  guardians: Array<{
    parentId: number;
    parentName: string;
    parentUsername: string;
    parentEmail?: string;
    relationship: 'FATHER' | 'MOTHER' | 'GUARDIAN';
  }>;
}

export function createStudentEnrollment(data: CreateStudentEnrollmentRequest) {
  return apiFetch('/admin/student-enrollments', {
    method: 'POST',
    body: JSON.stringify(data),
  }) as Promise<StudentEnrollmentResult>;
}

export function getStudentAccountsByClass(academicYearId: number, classId: number) {
  const params = new URLSearchParams({ academicYearId: String(academicYearId), classId: String(classId) });
  return apiFetch(`/admin/student-enrollments?${params}`) as Promise<StudentAccountByClass[]>;
}
