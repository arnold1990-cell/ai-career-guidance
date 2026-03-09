import { apiClient } from '@/services/apiClient';
import type { Bursary, PaginatedResponse } from '@/types';
export const bursaryService = {
  list: (params?: Record<string, string | number>) => apiClient.get<PaginatedResponse<Bursary> | Bursary[]>('/bursaries', { params }).then((r) => r.data),
  details: (id: string) => apiClient.get<Bursary>(`/bursaries/${id}`).then((r) => r.data),
};
