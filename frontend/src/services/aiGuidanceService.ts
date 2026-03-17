import { apiClient } from '@/services/apiClient';
import type { CareerAdviceRequest, CareerAdviceResponse } from '@/types';

export const aiGuidanceService = {
  getCareerAdvice: (payload: CareerAdviceRequest) =>
    apiClient.post<CareerAdviceResponse>('/ai/career-advice', payload).then((r) => r.data),
};
