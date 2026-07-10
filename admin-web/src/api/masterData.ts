import { apiFetch } from './client';



export async function getShifts() {
  return apiFetch('/master-data/shifts');
}

export async function getPeriods(shiftId?: number) {
  const path = shiftId ? `/master-data/periods?shiftId=${shiftId}` : '/master-data/periods';
  return apiFetch(path);
}


