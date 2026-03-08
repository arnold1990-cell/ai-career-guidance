import { apiClient } from '@/services/apiClient';
export const recommendationService = { mine: () => apiClient.get('/recommendations/me').then((r) => r.data) };
