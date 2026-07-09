import { apiFetch } from './client';

export async function getFeeTemplates(classId: number | string, semesterId: number | string) {
  return apiFetch(`/fee-templates?classId=${classId}&semesterId=${semesterId}`);
}

export interface CreateFeeTemplateData {
  feeCategoryId: number;
  classId: number;
  semesterId: number;
  name: string;
  amount: number;
  dueDate: string;
}

export async function createFeeTemplate(data: CreateFeeTemplateData) {
  return apiFetch('/fee-templates', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

export async function generateTuitionFromTemplate(id: number) {
  return apiFetch(`/fee-templates/${id}/generate`, {
    method: 'POST'
  });
}

export async function deleteFeeTemplate(id: number) {
  return apiFetch(`/fee-templates/${id}`, {
    method: 'DELETE'
  });
}
