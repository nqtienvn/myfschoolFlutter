import { apiFetch } from './client';

export interface CreateAnnouncementData {
  title: string;
  body: string;
  targetRole: string;
  requiresReply?: boolean;
  classIds?: number[];
}

export async function createAnnouncement(data: CreateAnnouncementData) {
  return apiFetch('/announcements', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}
