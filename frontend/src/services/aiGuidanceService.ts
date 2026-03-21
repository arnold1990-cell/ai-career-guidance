import { apiClient } from '@/services/apiClient';
import type {
  CareerAdviceRequest,
  CareerAdviceResponse,
  Recommendation,
  UniversitySourcesAnalysisRequest,
  UniversitySourcesAnalysisResponse,
} from '@/types';

const demoModeEnabled = import.meta.env.VITE_AI_GUIDANCE_DEMO_MODE === 'true';


export const normalizeUniversityResponse = (payload: UniversitySourcesAnalysisResponse): UniversitySourcesAnalysisResponse => {
  const requestedSources = payload.requestedSources ?? payload.sourceUrls ?? [];
  const sourceUrls = payload.sourceUrls ?? requestedSources;
  const sourceCoverage = payload.sourceCoverage ?? null;

  return {
    ...payload,
    status: payload.status ?? (payload.mode === 'PARTIAL' || payload.fallbackUsed ? 'PARTIAL' : payload.mode === 'UNAVAILABLE' ? 'ERROR' : 'SUCCESS'),
    mode: payload.mode ?? (payload.fallbackUsed ? 'PARTIAL' : payload.aiLive ? 'LIVE' : 'UNAVAILABLE'),
    message: payload.message ?? payload.warningMessage ?? null,
    requestedSources,
    sourceUrls,
    successfullyAnalysedUrls: payload.successfullyAnalysedUrls ?? [],
    failedUrls: payload.failedUrls ?? [],
    recommendedCareers: payload.recommendedCareers ?? [],
    recommendedProgrammes: payload.recommendedProgrammes ?? [],
    recommendedUniversities: payload.recommendedUniversities ?? [],
    minimumRequirements: payload.minimumRequirements ?? [],
    keyRequirements: payload.keyRequirements ?? [],
    skillGaps: payload.skillGaps ?? [],
    recommendedNextSteps: payload.recommendedNextSteps ?? [],
    warnings: payload.warnings ?? [],
    suitabilitySignalsUsed: payload.suitabilitySignalsUsed ?? [],
    suitabilityScoreLimitations: payload.suitabilityScoreLimitations ?? [],
    sourceDiagnostics: payload.sourceDiagnostics ?? [],
    sourceCoverage,
    totalSourcesUsed: payload.totalSourcesUsed ?? payload.successfullyAnalysedUrls?.length ?? sourceCoverage?.successfulSourcesCount ?? 0,
  };
};

export const aiGuidanceService = {
  demoModeEnabled,
  getCareerAdvice: (payload: CareerAdviceRequest) =>
    apiClient.post<CareerAdviceResponse>('/ai/career-advice', payload).then((r) => r.data),
  analyseUniversitySources: (payload: UniversitySourcesAnalysisRequest) =>
    apiClient.post<UniversitySourcesAnalysisResponse>('/ai/analyse-university-sources', payload, { timeout: 45000 }).then((r) => normalizeUniversityResponse(r.data)),
  getDefaultUniversitySources: () =>
    apiClient.get<string[]>('/ai/default-university-sources').then((r) => r.data),
  getDemoGuidance: () => apiClient.get<Recommendation>('/recommendations/me').then((r) => r.data),
};
