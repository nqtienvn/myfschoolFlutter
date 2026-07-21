import type {
  AttendanceCorrectionRequest,
  AttendanceCorrectionStatus,
} from '../api/attendance';

export interface AttendanceCorrectionViewFilters {
  status: AttendanceCorrectionStatus | '';
  date: string;
  classId: string;
  teacherId: string;
}

export function filterAttendanceCorrections(
  items: AttendanceCorrectionRequest[],
  filters: AttendanceCorrectionViewFilters,
) {
  return items.filter(item => (
    (!filters.status || item.status === filters.status)
    && (!filters.date || item.date === filters.date)
    && (!filters.classId || String(item.classId) === filters.classId)
    && (!filters.teacherId || String(item.teacherId) === filters.teacherId)
  ));
}
