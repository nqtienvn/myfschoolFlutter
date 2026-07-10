import { apiFetch } from './client';

export interface ReadinessCheck {
  code: string;
  label: string;
  passed: boolean;
  detail: string;
}

export interface AcademicYearReadiness {
  academicYearId: number;
  ready: boolean;
  checks: ReadinessCheck[];
}

export function getAcademicYearReadiness(academicYearId: number | string) {
  return apiFetch(`/academic-years/${academicYearId}/readiness`) as Promise<AcademicYearReadiness>;
}

export function activateAcademicYear(academicYearId: number | string) {
  return apiFetch(`/academic-years/${academicYearId}/activate`, { method: 'POST' });
}
