import { apiClient } from '@/services/apiClient';
export const careerService = { list: (params?: Record<string, string | number>) => apiClient.get('/careers', { params }).then((r) => r.data), details: (id: string) => apiClient.get(`/careers/${id}`).then((r) => r.data) };
