import { apiClient } from '@/services/apiClient';
export const notificationService = { mine: () => apiClient.get('/notifications').then((r) => r.data) };
