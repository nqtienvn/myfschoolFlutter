import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { getTeacherPeriods } from '../api/teacher';

export interface TeacherSemester {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: string;
  isCurrent?: boolean;
}

export interface TeacherAcademicYear {
  id: number;
  name: string;
  status: string;
  semesters: TeacherSemester[];
}

interface TeacherAcademicContextValue {
  years: TeacherAcademicYear[];
  semesters: TeacherSemester[];
  selectedYearId: number | null;
  selectedSemesterId: number | null;
  loading: boolean;
  error: string;
  selectYear: (id: number) => void;
  selectSemester: (id: number) => void;
  reload: () => Promise<void>;
}

const TeacherAcademicContext = createContext<TeacherAcademicContextValue | null>(null);

export function TeacherAcademicProvider({ children }: { children: ReactNode }) {
  const [years, setYears] = useState<TeacherAcademicYear[]>([]);
  const [selectedYearId, setSelectedYearId] = useState<number | null>(null);
  const [selectedSemesterId, setSelectedSemesterId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function reload() {
    setLoading(true); setError('');
    try {
      const values = (await getTeacherPeriods()) as TeacherAcademicYear[];
      const list = values || [];
      setYears(list);
      const year = list.find(item => item.id === selectedYearId)
        || list.find(item => item.status === 'ACTIVE') || list[0];
      setSelectedYearId(year?.id ?? null);
      const semester = year?.semesters.find(item => item.id === selectedSemesterId)
        || year?.semesters.find(item => item.status === 'ACTIVE' || item.isCurrent)
        || year?.semesters[0];
      setSelectedSemesterId(semester?.id ?? null);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể tải phạm vi năm học.');
    } finally { setLoading(false); }
  }

  useEffect(() => { void reload(); }, []);

  function selectYear(id: number) {
    setSelectedYearId(id);
    const year = years.find(item => item.id === id);
    const semester = year?.semesters.find(item => item.status === 'ACTIVE' || item.isCurrent) || year?.semesters[0];
    setSelectedSemesterId(semester?.id ?? null);
  }

  const semesters = years.find(item => item.id === selectedYearId)?.semesters || [];
  const value = useMemo<TeacherAcademicContextValue>(() => ({
    years, semesters, selectedYearId, selectedSemesterId, loading, error,
    selectYear, selectSemester: setSelectedSemesterId, reload,
  }), [years, semesters, selectedYearId, selectedSemesterId, loading, error]);

  return <TeacherAcademicContext.Provider value={value}>{children}</TeacherAcademicContext.Provider>;
}

export function useTeacherAcademic() {
  const value = useContext(TeacherAcademicContext);
  if (!value) throw new Error('TeacherAcademicProvider chưa được khởi tạo');
  return value;
}
