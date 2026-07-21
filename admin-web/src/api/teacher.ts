import { clearTeacherToken, getTeacherToken, setTeacherToken, teacherApiFetch } from './client';

export interface TeacherUser { id: number; name: string; role: string; phone: string; accountCode?: string }
export interface TeacherLoginResponse { token: string; user: TeacherUser }

export async function loginTeacher(phone: string, password: string): Promise<TeacherUser> {
  const data = await teacherApiFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ phone, password }),
  }) as TeacherLoginResponse;
  if (data.user.role !== 'TEACHER') throw new Error('Tài khoản không có quyền truy cập cổng Giáo viên');
  setTeacherToken(data.token);
  localStorage.setItem('teacher_user', JSON.stringify(data.user));
  return data.user;
}

export const isTeacherLoggedIn = () => !!getTeacherToken();
export function logoutTeacher() { clearTeacherToken(); localStorage.removeItem('teacher_user'); }
export function getStoredTeacher(): TeacherUser | null {
  try { return JSON.parse(localStorage.getItem('teacher_user') || 'null'); } catch { return null; }
}

export const getTeacherPeriods = () => teacherApiFetch('/academic-years/available');
export const getTeacherDashboard = (academicYearId: number, semesterId: number) =>
  teacherApiFetch(`/dashboard/teacher?academicYearId=${academicYearId}&semesterId=${semesterId}`);
export const getTeacherAssignments = (academicYearId: number) =>
  teacherApiFetch(`/teaching-assignments/mine?academicYearId=${academicYearId}`);
export const getTeacherGradeBook = (classId: number, subjectId: number, semesterId: number) =>
  teacherApiFetch(`/grade-books?classId=${classId}&subjectId=${subjectId}&semesterId=${semesterId}`);
export const getTeacherGradeStudents = (bookId: number) => teacherApiFetch(`/grade-books/${bookId}/students`);
export const submitTeacherScores = (gradeItemId: number, entries: unknown[]) => teacherApiFetch('/grade-books/scores', {
  method: 'PUT', body: JSON.stringify({ gradeItemId, entries, reason: 'Giáo viên submit và công bố điểm trên Web Portal' }),
});

export const getTeacherViolations = (studentId: number, academicYearId: number, semesterId: number, classId: number) =>
  teacherApiFetch(`/students/${studentId}/events?academicYearId=${academicYearId}&semesterId=${semesterId}&classId=${classId}`);
export const saveTeacherViolation = (studentId: number, payload: unknown, id?: number) => teacherApiFetch(
  id ? `/student-events/${id}` : `/students/${studentId}/events`,
  { method: id ? 'PUT' : 'POST', body: JSON.stringify({ ...(payload as object), eventType: 'VIOLATION' }) },
);
export const deleteTeacherViolation = (id: number, academicYearId: number) =>
  teacherApiFetch(`/student-events/${id}?academicYearId=${academicYearId}`, { method: 'DELETE' });
export const submitTeacherViolations = (studentId: number, payload: unknown) =>
  teacherApiFetch(`/students/${studentId}/violations/submit`, { method: 'POST', body: JSON.stringify(payload) });
export const submitHomeroomClassViolations = (payload: unknown) =>
  teacherApiFetch('/student-events/violations/submit-class', { method: 'POST', body: JSON.stringify(payload) });

export const getEligibleAnnouncementClasses = (academicYearId: number) =>
  teacherApiFetch(`/announcements/eligible-classes?academicYearId=${academicYearId}`);
export const getReceivedAnnouncements = (academicYearId: number) =>
  teacherApiFetch(`/announcements?academicYearId=${academicYearId}`);
export const getSentAnnouncements = (academicYearId: number) =>
  teacherApiFetch(`/announcements/mine?academicYearId=${academicYearId}`);
export const markAnnouncementRead = (id: number) => teacherApiFetch(`/announcements/${id}/read`, { method: 'PUT' });
export interface TeacherAnnouncementViolation { ruleId?: number; field: 'TITLE' | 'BODY'; phrase: string }
export interface TeacherAnnouncementSubmissionResult {
  outcome: 'PUBLISHED' | 'SYSTEM_REJECTED';
  message: string;
  announcement: { id: number };
  violations: TeacherAnnouncementViolation[];
}
export const saveTeacherAnnouncement = (payload: unknown) => teacherApiFetch(
  '/announcements', { method: 'POST', body: JSON.stringify(payload) },
) as Promise<TeacherAnnouncementSubmissionResult>;
export const deleteTeacherAnnouncement = (id: number) => teacherApiFetch(`/announcements/${id}`, { method: 'DELETE' });

export const getHomeroomClass = (classId: number) => teacherApiFetch(`/classes/${classId}`);
export const getHomeroomRanking = (classId: number, semesterId: number) =>
  teacherApiFetch(`/semester-results/ranking?classId=${classId}&semesterId=${semesterId}`);
export const getPendingLeaveRequests = (academicYearId: number, semesterId: number) =>
  teacherApiFetch(`/leave-requests/pending?academicYearId=${academicYearId}&semesterId=${semesterId}`);
export const getReviewedLeaveRequests = (academicYearId: number, semesterId: number) =>
  teacherApiFetch(`/leave-requests/reviewed?academicYearId=${academicYearId}&semesterId=${semesterId}`);
export const approveLeaveRequest = (id: number) => teacherApiFetch(`/leave-requests/${id}/approve`, { method: 'PUT' });
export const rejectLeaveRequest = (id: number, response: string) => teacherApiFetch(`/leave-requests/${id}/reject`, {
  method: 'PUT', body: JSON.stringify({ response }),
});

export const getTeacherConversations = () => teacherApiFetch('/conversations');
export const getConversationMessages = (id: number) => teacherApiFetch(`/conversations/${id}?limit=100`);
export const sendConversationMessage = (id: number, content: string) => teacherApiFetch(`/conversations/${id}/messages`, {
  method: 'POST',
  body: JSON.stringify({ clientMessageId: `web-${Date.now()}-${Math.random().toString(16).slice(2)}`, content, messageType: 'TEXT' }),
});
export const searchChatUsers = (keyword: string) => teacherApiFetch(`/conversations/search-users?keyword=${encodeURIComponent(keyword)}`);
export const createConversation = (otherUserId: number) => teacherApiFetch('/conversations', {
  method: 'POST', body: JSON.stringify({ otherUserId }),
});
