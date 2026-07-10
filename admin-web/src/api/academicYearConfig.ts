import { apiFetch } from './client';

export interface AcademicYearMasterDataConfig {
  academicYearId: number;
  subjectIds: number[];
  shiftIds: number[];
  periodIds: number[];
}

export function getAcademicYearMasterData(academicYearId: number | string) {
  return apiFetch(`/academic-years/${academicYearId}/master-data`) as Promise<AcademicYearMasterDataConfig>;
}

export function updateAcademicYearMasterData(
  academicYearId: number | string,
  data: Omit<AcademicYearMasterDataConfig, 'academicYearId'>,
) {
  return apiFetch(`/academic-years/${academicYearId}/master-data`, {
    method: 'PUT',
    body: JSON.stringify(data),
  }) as Promise<AcademicYearMasterDataConfig>;
}
