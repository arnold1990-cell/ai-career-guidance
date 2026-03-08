import { apiClient } from '@/services/apiClient';
export const bursaryService = { list: (params?: Record<string, string | number>) => apiClient.get('/bursaries', { params }).then((r) => r.data), details: (id: string) => apiClient.get(`/bursaries/${id}`).then((r) => r.data) };
