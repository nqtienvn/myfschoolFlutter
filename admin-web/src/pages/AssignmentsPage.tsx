import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

export default function AssignmentsPage() {
  const [classes, setClasses] = useState<any[]>([]);
  const [subjects, setSubjects] = useState<any[]>([]);
  const [classId, setClassId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [teacherId, setTeacherId] = useState('');
  const [academicYear, setAcademicYear] = useState('2026-2027');

  useEffect(() => {
    apiFetch('/classes?page=0&size=100').then(d => setClasses(d.content || [])).catch(() => {});
    apiFetch('/subjects').then(setSubjects).catch(() => {});
  }, []);

  async function assign() {
    try {
      await apiFetch(`/classes/${classId}/subjects`, {
        method: 'POST',
        body: JSON.stringify({ classId: +classId, subjectId: +subjectId, teacherId: +teacherId, academicYear }),
      });
      alert('Phân công thành công');
    } catch (err: any) { alert(err.message); }
  }

  return (
    <div>
      <h2>Phân công giáo viên</h2>
      <div className="form-inline">
        <select value={classId} onChange={e => setClassId(e.target.value)}>
          <option value="">Chọn lớp</option>
          {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select value={subjectId} onChange={e => setSubjectId(e.target.value)}>
          <option value="">Chọn môn</option>
          {subjects.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <input placeholder="Teacher ID" type="number" value={teacherId} onChange={e => setTeacherId(e.target.value)} />
        <button onClick={assign}>Phân công</button>
      </div>
    </div>
  );
}
