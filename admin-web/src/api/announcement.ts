import { apiFetch } from './client';

export type AnnouncementStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export interface AnnouncementItem {
  id: number; title: string; body: string; targetRole: 'PARENT' | 'STUDENT' | 'ALL';
  teacherName: string; classNames: string[]; createdAt: string;
  requiresReply: boolean; totalRecipients: number; readCount: number;
  acknowledgedCount: number; repliedCount: number;
  approvalStatus: AnnouncementStatus; rejectionReason?: string; senderType: string;
  recipientScope: 'SCHOOL' | 'CLASSES' | 'TEACHERS'; teacherAudience?: 'ALL' | 'SUBJECT' | 'HOMEROOM';
  subjectId?: number; subjectName?: string;
}

export const getAnnouncements = (academicYearId: string, status?: string) =>
  apiFetch(`/announcements/admin?academicYearId=${academicYearId}${status ? `&status=${status}` : ''}`) as Promise<AnnouncementItem[]>;

export const reviewAnnouncement = (id: number, approve: boolean, reason?: string) =>
  apiFetch(`/announcements/${id}/review`, { method: 'PUT', body: JSON.stringify({ approve, reason }) });

export const deleteAnnouncement = (id: number) => apiFetch(`/announcements/${id}`, { method: 'DELETE' });

export interface AdminAnnouncementPayload {
  academicYearId: number; title: string; body: string;
  recipientScope: 'SCHOOL' | 'CLASSES' | 'TEACHERS';
  targetRole?: 'PARENT' | 'STUDENT' | 'ALL'; classIds?: number[];
  teacherAudience?: 'ALL' | 'SUBJECT' | 'HOMEROOM'; subjectId?: number;
  requiresReply?: boolean;
}
export const broadcastAnnouncement = (payload: AdminAnnouncementPayload) =>
  apiFetch('/announcements/admin/broadcast', {
    method: 'POST', body: JSON.stringify(payload),
  });

export type AnnouncementRecipientStatus = 'UNREAD' | 'READ' | 'ACKNOWLEDGED' | 'REPLIED' | 'PENDING';

export interface AnnouncementRecipient {
  userId: number;
  userName: string;
  role: 'PARENT' | 'STUDENT' | 'TEACHER';
  studentNames: string[];
  classNames: string[];
  readAt?: string;
  acknowledgedAt?: string;
  replyText?: string;
  repliedAt?: string;
  status: Exclude<AnnouncementRecipientStatus, 'PENDING'>;
}

export interface RecipientPage {
  content: AnnouncementRecipient[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface RecipientFilters {
  academicYearId: string;
  classId?: string;
  role?: string;
  status?: string;
  keyword?: string;
  page?: number;
  size?: number;
}

export function buildRecipientQuery(filters: RecipientFilters) {
  const params = new URLSearchParams({
    academicYearId: filters.academicYearId,
    page: String(filters.page ?? 0),
    size: String(filters.size ?? 20),
  });
  if (filters.classId) params.set('classId', filters.classId);
  if (filters.role) params.set('role', filters.role);
  if (filters.status) params.set('status', filters.status);
  if (filters.keyword?.trim()) params.set('keyword', filters.keyword.trim());
  return params.toString();
}

export const getAnnouncementRecipients = (id: number, filters: RecipientFilters) =>
  apiFetch(`/announcements/${id}/recipients?${buildRecipientQuery(filters)}`) as Promise<RecipientPage>;

export function recipientStats(item: Pick<AnnouncementItem, 'totalRecipients' | 'readCount' | 'acknowledgedCount' | 'repliedCount'>) {
  return {
    total: item.totalRecipients,
    read: item.readCount,
    acknowledged: item.acknowledgedCount,
    replied: item.repliedCount,
  };
}
