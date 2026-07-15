import { apiFetch } from './client';

export interface PaymentTransaction {
  id: number;
  amount: number;
  paymentMethod: string;
  transactionRef: string;
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
  paidAt: string | null;
  createdAt: string;
}

export interface PendingTuitionBill {
  id: number;
  studentId: number;
  studentName: string;
  studentCode: string;
  classId: number;
  className: string;
  semesterId: number;
  semesterName: string;
  name: string;
  amount: number;
  dueDate: string;
  status: 'PROCESSING';
  transactions: PaymentTransaction[];
  createdAt: string;
}

export async function getPendingPaymentRequests(
  academicYearId: string,
): Promise<PendingTuitionBill[]> {
  const params = new URLSearchParams({ academicYearId });
  return apiFetch(`/tuition/payment-requests?${params}`);
}

export async function confirmPayment(billId: number): Promise<PaymentTransaction> {
  return apiFetch(`/tuition/bills/${billId}/confirm-payment`, { method: 'POST' });
}

export async function rejectPayment(billId: number): Promise<PaymentTransaction> {
  return apiFetch(`/tuition/bills/${billId}/reject-payment`, { method: 'POST' });
}
