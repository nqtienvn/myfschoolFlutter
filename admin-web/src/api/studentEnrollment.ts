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

export function createStudentEnrollment(data: CreateStudentEnrollmentRequest) {
  return apiFetch('/admin/student-enrollments', {
    method: 'POST',
    body: JSON.stringify(data),
  }) as Promise<StudentEnrollmentResult>;
}
