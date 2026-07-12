import { apiFetch } from './client';

export type AnnouncementStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export interface AnnouncementItem {
  id: number; title: string; body: string; targetRole: 'PARENT' | 'STUDENT' | 'ALL';
  teacherName: string; classNames: string[]; createdAt: string;
  approvalStatus: AnnouncementStatus; rejectionReason?: string; senderType: string;
}

export const getAnnouncements = (academicYearId: string, status?: string) =>
  apiFetch(`/announcements/admin?academicYearId=${academicYearId}${status ? `&status=${status}` : ''}`) as Promise<AnnouncementItem[]>;

export const reviewAnnouncement = (id: number, approve: boolean, reason?: string) =>
  apiFetch(`/announcements/${id}/review`, { method: 'PUT', body: JSON.stringify({ approve, reason }) });

export const deleteAnnouncement = (id: number) => apiFetch(`/announcements/${id}`, { method: 'DELETE' });

export const broadcastAnnouncement = (academicYearId: string, title: string, body: string) =>
  apiFetch('/announcements/admin/broadcast', {
    method: 'POST', body: JSON.stringify({ academicYearId: Number(academicYearId), title, body }),
  });
