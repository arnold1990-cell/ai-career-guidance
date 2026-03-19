import { apiClient } from '@/services/apiClient';
import type { Opportunity } from '@/types';

export const opportunityService = {
  list: (params?: Record<string, string | number>) => apiClient.get<Opportunity[]>('/opportunities', { params }).then((r) => r.data),
};
