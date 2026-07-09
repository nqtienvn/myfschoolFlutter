import { apiFetch } from './client';

export async function getFeeCategories() {
  return apiFetch('/fee-categories');
}

export async function createFeeCategory(data: { name: string; description: string }) {
  return apiFetch('/fee-categories', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

export async function deleteFeeCategory(id: number) {
  return apiFetch(`/fee-categories/${id}`, {
    method: 'DELETE'
  });
}
