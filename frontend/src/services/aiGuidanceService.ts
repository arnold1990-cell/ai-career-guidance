import { apiClient } from '@/services/apiClient';
import type { CareerAdviceRequest, CareerAdviceResponse, Recommendation } from '@/types';

const demoModeEnabled = import.meta.env.VITE_AI_GUIDANCE_DEMO_MODE === 'true';

export const aiGuidanceService = {
  demoModeEnabled,
  getCareerAdvice: (payload: CareerAdviceRequest) =>
    apiClient.post<CareerAdviceResponse>('/ai/career-advice', payload).then((r) => r.data),
  getDemoGuidance: () => apiClient.get<Recommendation>('/recommendations/me').then((r) => r.data),
};
