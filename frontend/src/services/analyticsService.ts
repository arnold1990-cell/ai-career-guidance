import { apiClient } from '@/services/apiClient';
export const analyticsService = { adminOverview: () => apiClient.get('/admin/analytics').then((r) => r.data) };
