export type Role = 'STUDENT' | 'COMPANY' | 'ADMIN';
export type BackendRole = `ROLE_${Role}`;

export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'MORE_INFO_REQUIRED';

export interface User { id: string; email: string; fullName?: string; companyName?: string; roles: BackendRole[]; role?: Role; primaryRole?: BackendRole; approvalStatus?: ApprovalStatus; }
export interface AuthResponse { accessToken: string; refreshToken?: string; tokenType?: string; accessTokenExpiresIn?: number; role?: string; primaryRole?: string; user: User; }
export interface AuthResponseRaw { accessToken?: string; refreshToken?: string; tokenType?: string; accessTokenExpiresIn?: number; role?: string; primaryRole?: string; approvalStatus?: string; roles?: string[]; user?: Partial<User> & { role?: string; primaryRole?: string; approvalStatus?: string; roles?: string[] }; }

export interface StudentRegisterPayload { fullName?: string; firstName?: string; lastName?: string; email: string; password: string; interests?: string; location?: string; phone?: string; dateOfBirth?: string; gender?: string; qualificationLevel?: string; }
export interface CompanyRegisterPayload { companyName: string; registrationNumber: string; industry?: string; officialEmail: string; mobileNumber?: string; contactPersonName: string; address?: string; website?: string; description?: string; password: string; }

export interface StudentProfile {
  id: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  location?: string;
  qualificationLevel?: string;
  qualifications: string[];
  experience: string[];
  skills: string[];
  interests: string[];
  careerGoals?: string;
  cvFileUrl?: string;
  transcriptFileUrl?: string;
  profileCompleted: boolean;
  profileCompleteness: number;
}

export interface Career { id: string; title: string; description?: string; industry?: string; location?: string; qualificationLevel?: string; matchScore?: number; }
export interface Bursary { id: string; title: string; provider?: string; qualificationLevel?: string; region?: string; eligibility?: string; deadline?: string; status: string; }
export type OpportunityType = 'ALL' | 'CAREER' | 'JOB' | 'INTERNSHIP';
export interface UnifiedOpportunity {
  id: string;
  title: string;
  type: Exclude<OpportunityType, 'ALL'>;
  field?: string;
  industry?: string;
  qualification?: string;
  location?: string;
  demand?: string;
  saved: boolean;
  recommended: boolean;
}

export interface Course { id: string; name: string; institutionName: string; duration: string; }
export interface Institution { id: string; name: string; location?: string; city?: string; province?: string; country?: string; website?: string; logoUrl?: string; category?: string; featured?: boolean; active?: boolean; }

export interface Application { id: string; status: string; createdAt: string; bursaryId: string; }
export interface RecommendationItem { id: string; title: string; score: number; rationale: string; }
export interface Recommendation {
  suggestedCareers: RecommendationItem[];
  suggestedBursaries: RecommendationItem[];
  suggestedCoursesOrImprovements: RecommendationItem[];
  profileImprovementTips: string[];
  modelVersion: string;
}

export interface CareerAdviceRequest {
  qualificationLevel: string;
  interests: string;
  skills: string;
  location: string;
}

export interface CareerAdviceItem {
  name: string;
  matchScore: number;
  reason: string;
  improvements: string[];
}

export interface CareerAdviceResponse {
  recommendedCareers: CareerAdviceItem[];
}

export interface UniversitySourcesAnalysisRequest {
  urls?: string[];
  targetProgram?: string;
  careerInterest?: string;
  qualificationLevel?: string;
  maxRecommendations?: number;
}

export interface UniversityRecommendedCareer {
  name: string;
  reason: string;
  requirements: string[];
  relatedProgrammes: string[];
  recommendationReason?: string | null;
  confidenceLevel?: string | null;
  verifiedFacts?: string[];
  inferredInsights?: string[];
  missingData?: string[];
  sourceStatus?: string | null;
  rankingCategory?: string | null;
  nextBestActions?: string[];
}

export interface UniversityRecommendedProgramme {
  name: string;
  university: string;
  admissionRequirements: string[];
  notes: string;
  recommendationReason?: string | null;
  confidenceLevel?: string | null;
  verifiedFacts?: string[];
  inferredInsights?: string[];
  missingData?: string[];
  sourceStatus?: string | null;
  rankingCategory?: string | null;
  nextBestActions?: string[];
}

export interface UniversitySourceDiagnostic {
  sourceUrl: string;
  fetchStatus: string;
  failureReason?: string | null;
  university?: string | null;
  usableProgrammeData?: boolean;
}

export interface UniversitySourceCoverage {
  requestedSourcesCount: number;
  successfulSourcesCount: number;
  failedSourcesCount: number;
  partialSourcesCount: number;
  universitiesWithUsableProgrammeData: string[];
}

export interface UniversitySourcesAnalysisResponse {
  aiLive: boolean;
  fallbackUsed: boolean;
  warningMessage?: string | null;
  sourceUrls: string[];
  successfullyAnalysedUrls: string[];
  failedUrls: string[];
  totalSourcesUsed: number;
  summary: string;
  recommendedCareers: UniversityRecommendedCareer[];
  recommendedProgrammes: UniversityRecommendedProgramme[];
  recommendedUniversities: string[];
  minimumRequirements: string[];
  keyRequirements: string[];
  skillGaps: string[];
  recommendedNextSteps: string[];
  warnings: string[];
  suitabilityScore: number;
  rawModelUsed: string;
  suitabilityScoreReason?: string | null;
  suitabilitySignalsUsed?: string[];
  suitabilityScoreLimitations?: string[];
  sourceDiagnostics?: UniversitySourceDiagnostic[];
  sourceCoverage?: UniversitySourceCoverage | null;
}
export interface Notification { id: string; title: string; message: string; read: boolean; type?: string; createdAt?: string; isRead?: boolean; }
export interface Subscription { id: string; planCode: string; status: string; renewalDate: string; }
export interface PaginatedResponse<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }
export interface ApiError { message: string; status?: number; details?: Record<string, string[]>; }
