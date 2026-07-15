import { apiFetch } from './client';

export interface PaymentConfiguration {
  id: number;
  academicYearId: number;
  bankCode: string | null;
  bankName: string;
  accountNumber: string;
  accountHolder: string;
  branch: string | null;
  transferContentTemplate: string;
  enabled: boolean;
  method: 'BANK_TRANSFER';
  displayMode: 'MANUAL' | 'MANUAL_AND_QR';
  qrAvailable: boolean;
  updatedAt: string | null;
}

export interface PaymentConfigurationInput {
  bankCode: string;
  bankName: string;
  accountNumber: string;
  accountHolder: string;
  branch: string;
  transferContentTemplate: string;
  enabled: boolean;
}

export async function getPaymentConfiguration(
  academicYearId: string,
): Promise<PaymentConfiguration | null> {
  return apiFetch(`/payment-configurations/academic-years/${academicYearId}`);
}

export async function updatePaymentConfiguration(
  academicYearId: string,
  input: PaymentConfigurationInput,
): Promise<PaymentConfiguration> {
  return apiFetch(`/payment-configurations/academic-years/${academicYearId}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}
