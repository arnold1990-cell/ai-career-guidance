import { apiClient } from '@/services/apiClient';
export const applicationService = { listMine: () => apiClient.get('/applications/me').then((r) => r.data), submit: (payload: Record<string, unknown>) => apiClient.post('/applications', payload) };
