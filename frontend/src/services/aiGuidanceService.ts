import { apiClient } from '@/services/apiClient';
import type {
  CareerAdviceRequest,
  CareerAdviceResponse,
  Recommendation,
  UniversitySourcesAnalysisRequest,
  UniversitySourcesAnalysisResponse,
} from '@/types';

const demoModeEnabled = import.meta.env.VITE_AI_GUIDANCE_DEMO_MODE === 'true';


const normalizeUniversityResponse = (payload: UniversitySourcesAnalysisResponse): UniversitySourcesAnalysisResponse => {
  const requestedSources = payload.requestedSources ?? payload.sourceUrls ?? [];
  const sourceUrls = payload.sourceUrls ?? requestedSources;
  const sourceCoverage = payload.sourceCoverage ?? null;

  return {
    ...payload,
    mode: payload.mode ?? (payload.fallbackUsed ? 'FALLBACK' : payload.aiLive ? 'LIVE' : 'UNAVAILABLE'),
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
    apiClient.post<UniversitySourcesAnalysisResponse>('/ai/analyse-university-sources', payload).then((r) => normalizeUniversityResponse(r.data)),
  getDefaultUniversitySources: () =>
    apiClient.get<string[]>('/ai/default-university-sources').then((r) => r.data),
  getDemoGuidance: () => apiClient.get<Recommendation>('/recommendations/me').then((r) => r.data),
};
