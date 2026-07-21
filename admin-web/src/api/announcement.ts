import { apiFetch } from './client';

export type AnnouncementDeliveryStatus = 'PUBLISHED' | 'SYSTEM_REJECTED';
export type AnnouncementPolicyScope = 'TITLE' | 'BODY' | 'ALL';
export type AnnouncementPolicyMatchType = 'CONTAINS' | 'EXACT';

export interface AnnouncementViolation {
  ruleId?: number;
  field: 'TITLE' | 'BODY';
  phrase: string;
}

export interface AnnouncementItem {
  id: number;
  title: string;
  body: string;
  targetRole: 'PARENT' | 'STUDENT' | 'ALL';
  teacherName: string;
  classNames: string[];
  createdAt: string;
  academicYearId: number;
  deliveryStatus: AnnouncementDeliveryStatus;
  systemRejectionMessage?: string;
  senderType: string;
  recipientScope: 'SCHOOL' | 'CLASSES';
  retryOfAnnouncementId?: number;
  violations: AnnouncementViolation[];
}

export interface AnnouncementPage {
  content: AnnouncementItem[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AnnouncementSummary {
  total: number;
  published: number;
  systemRejected: number;
}

export interface AnnouncementPolicyRule {
  id?: number;
  phrase: string;
  scope: AnnouncementPolicyScope;
  matchType: AnnouncementPolicyMatchType;
}

export interface AnnouncementPolicy {
  academicYearId: number;
  enabled: boolean;
  rejectionMessage: string;
  rules: AnnouncementPolicyRule[];
  updatedAt?: string;
}

export interface AnnouncementPolicyPayload {
  academicYearId: number;
  enabled: boolean;
  rejectionMessage: string;
  rules: AnnouncementPolicyRule[];
}

export interface AnnouncementQuery {
  academicYearId: string;
  status?: AnnouncementDeliveryStatus | '';
  keyword?: string;
  page: number;
  size: number;
}

export const getAnnouncements = (query: AnnouncementQuery) => {
  const params = new URLSearchParams({
    academicYearId: query.academicYearId,
    page: String(query.page),
    size: String(query.size),
  });
  if (query.status) params.set('status', query.status);
  if (query.keyword) params.set('keyword', query.keyword);
  return apiFetch(`/announcements/admin?${params}`) as Promise<AnnouncementPage>;
};

export const getAnnouncementSummary = (academicYearId: string) =>
  apiFetch(`/announcements/admin/summary?academicYearId=${academicYearId}`) as Promise<AnnouncementSummary>;

export const getAnnouncementPolicy = (academicYearId: string) =>
  apiFetch(`/announcements/admin/policy?academicYearId=${academicYearId}`) as Promise<AnnouncementPolicy>;

export const updateAnnouncementPolicy = (payload: AnnouncementPolicyPayload) =>
  apiFetch('/announcements/admin/policy', {
    method: 'PUT',
    body: JSON.stringify(payload),
  }) as Promise<AnnouncementPolicy>;

export const deleteAnnouncement = (id: number) =>
  apiFetch(`/announcements/${id}`, { method: 'DELETE' });

export interface AdminAnnouncementPayload {
  academicYearId: number;
  title: string;
  body: string;
}

export const broadcastAnnouncement = (payload: AdminAnnouncementPayload) =>
  apiFetch('/announcements/admin/broadcast', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
