import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('./client', () => ({ apiFetch: vi.fn() }));

import { getAcademicYearMasterData, updateAcademicYearMasterData } from './academicYearConfig';
import { generateClasses, getClasses } from './class';
import { apiFetch } from './client';
import { createStudentEnrollment, getStudentAccountsByClass } from './studentEnrollment';

const mockedApiFetch = vi.mocked(apiFetch);

describe('academic-year-scoped admin API contracts', () => {
  beforeEach(() => {
    mockedApiFetch.mockReset();
  });

  it('includes the selected year in class list and generation requests', async () => {
    await getClasses({ academicYearId: 2026, page: 2, size: 25 });
    await generateClasses({
      academicYearId: 2026,
      gradeLevel: 10,
      namingPrefix: '10A',
      count: 3,
    });

    expect(mockedApiFetch).toHaveBeenNthCalledWith(
      1,
      '/classes?academicYearId=2026&page=2&size=25',
    );
    expect(mockedApiFetch).toHaveBeenNthCalledWith(2, '/classes/generate', {
      method: 'POST',
      body: JSON.stringify({
        academicYearId: 2026,
        gradeLevel: 10,
        namingPrefix: '10A',
        count: 3,
      }),
    });
  });

  it('keeps student enrollment reads and writes scoped to the selected year', async () => {
    const enrollment = {
      academicYearId: 2026,
      classId: 12,
      studentCode: 'HS001',
      studentName: 'Nguyễn An',
      dateOfBirth: '2010-01-02',
      gender: 'MALE' as const,
      parentName: 'Nguyễn Bình',
      relationship: 'FATHER' as const,
      parentPhone: '0900000000',
    };

    await createStudentEnrollment(enrollment);
    await getStudentAccountsByClass(2026, 12);

    expect(mockedApiFetch).toHaveBeenNthCalledWith(1, '/admin/student-enrollments', {
      method: 'POST',
      body: JSON.stringify(enrollment),
    });
    expect(mockedApiFetch).toHaveBeenNthCalledWith(
      2,
      '/admin/student-enrollments?academicYearId=2026&classId=12',
    );
  });

  it('uses the selected year as the master-data resource boundary', async () => {
    await getAcademicYearMasterData(2026);
    await updateAcademicYearMasterData(2026, {
      subjectIds: [1, 2],
      shiftIds: [3],
      periodIds: [4, 5],
    });

    expect(mockedApiFetch).toHaveBeenNthCalledWith(
      1,
      '/academic-years/2026/master-data',
    );
    expect(mockedApiFetch).toHaveBeenNthCalledWith(
      2,
      '/academic-years/2026/master-data',
      {
        method: 'PUT',
        body: JSON.stringify({ subjectIds: [1, 2], shiftIds: [3], periodIds: [4, 5] }),
      },
    );
  });
});
