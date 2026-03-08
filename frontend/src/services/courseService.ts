import { apiClient } from '@/services/apiClient';
export const courseService = { list: (params?: Record<string, string | number>) => apiClient.get('/courses', { params }).then((r) => r.data), details: (id: string) => apiClient.get(`/courses/${id}`).then((r) => r.data) };
