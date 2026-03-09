import { apiClient } from '@/services/apiClient';
import type { Subscription } from '@/types';

export const subscriptionService = {
  current: () => apiClient.get<Subscription>('/subscriptions/me').then((r) => r.data),
  purchase: (plan: 'BASIC' | 'PREMIUM') => apiClient.post('/subscriptions/purchase', { plan }).then((r) => r.data),
};
