import { apiFetch } from './client';

export async function getGradeLevels() {
  return apiFetch('/master-data/grade-levels');
}

export async function getShifts() {
  return apiFetch('/master-data/shifts');
}

export async function getPeriods(shiftId?: number) {
  const path = shiftId ? `/master-data/periods?shiftId=${shiftId}` : '/master-data/periods';
  return apiFetch(path);
}

export async function initializeMasterData() {
  return apiFetch('/master-data/initialize', { method: 'POST' });
}
