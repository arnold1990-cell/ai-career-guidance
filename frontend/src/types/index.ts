export type Role = 'STUDENT' | 'COMPANY' | 'ADMIN';
export type BackendRole = `ROLE_${Role}`;

export interface User { id: string; email: string; fullName?: string; companyName?: string; roles: BackendRole[]; }
export interface AuthResponse { accessToken: string; refreshToken?: string; tokenType?: string; accessTokenExpiresIn?: number; user: User; }
export interface AuthResponseRaw { accessToken?: string; refreshToken?: string; tokenType?: string; accessTokenExpiresIn?: number; role?: string; roles?: string[]; user?: Partial<User> & { role?: string; roles?: string[] }; }

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

export interface Course { id: string; name: string; institutionName: string; duration: string; }
export interface Institution { id: string; name: string; location: string; }

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
export interface Notification { id: string; title: string; message: string; read: boolean; }
export interface Subscription { id: string; planCode: string; status: string; renewalDate: string; }
export interface PaginatedResponse<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }
export interface ApiError { message: string; status?: number; details?: Record<string, string[]>; }
