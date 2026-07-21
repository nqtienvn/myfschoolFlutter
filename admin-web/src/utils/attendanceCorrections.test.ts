import { describe, expect, it } from 'vitest';
import type { AttendanceCorrectionRequest } from '../api/attendance';
import { filterAttendanceCorrections } from './attendanceCorrections';

const base: AttendanceCorrectionRequest = {
  id: 1,
  classId: 10,
  className: '12A1',
  teacherId: 20,
  teacherName: 'Nguyễn Văn A',
  date: '2026-07-21',
  shift: 'MORNING',
  status: 'PENDING',
  originalPresentCount: 30,
  originalAbsentWithLeaveCount: 0,
  originalAbsentWithoutLeaveCount: 1,
  presentCount: 31,
  absentWithLeaveCount: 0,
  absentWithoutLeaveCount: 0,
  reason: 'Điểm danh nhầm',
  changes: [],
  createdAt: '2026-07-21T08:00:00',
};

describe('filterAttendanceCorrections', () => {
  it('mặc định chỉ giữ yêu cầu đang chờ duyệt', () => {
    const approved = { ...base, id: 2, status: 'APPROVED' as const };
    expect(filterAttendanceCorrections([base, approved], {
      status: 'PENDING', date: '', classId: '', teacherId: '',
    })).toEqual([base]);
  });

  it('kết hợp bộ lọc ngày, lớp và giáo viên', () => {
    const otherClass = { ...base, id: 3, classId: 11, className: '12A2' };
    expect(filterAttendanceCorrections([base, otherClass], {
      status: '', date: '2026-07-21', classId: '10', teacherId: '20',
    })).toEqual([base]);
  });
});
