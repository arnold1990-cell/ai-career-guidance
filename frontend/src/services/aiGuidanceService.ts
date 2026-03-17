import { apiClient } from '@/services/apiClient';
import type { CareerAdviceRequest, CareerAdviceResponse } from '@/types';

const demoModeEnabled = import.meta.env.VITE_AI_GUIDANCE_DEMO_MODE === 'true';

export const aiGuidanceService = {
  demoModeEnabled,
  getCareerAdvice: (payload: CareerAdviceRequest) =>
    apiClient.post<CareerAdviceResponse>('/api/ai/career-advice', payload).then((r) => r.data),
};
