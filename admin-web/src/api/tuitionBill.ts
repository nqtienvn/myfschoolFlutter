import { apiFetch } from './client';

export async function getClassTuitionBills(classId: number | string, semesterId: number | string) {
  return apiFetch(`/tuition/bills/class?classId=${classId}&semesterId=${semesterId}`);
}

export async function deleteTuitionBill(id: number) {
  return apiFetch(`/tuition/bills/${id}`, {
    method: 'DELETE'
  });
}
