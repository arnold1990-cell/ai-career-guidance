export type Role = 'STUDENT' | 'COMPANY' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  active: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken?: string;
  user: User;
}

export interface StudentProfile {
  id: string;
  fullName: string;
  gradeLevel?: string;
  profileCompleteness: number;
  skills: string[];
}

export interface CompanyProfile {
  id: string;
  companyName: string;
  industry?: string;
  verified: boolean;
}

export interface Career { id: string; title: string; description: string; matchScore?: number; }
export interface Course { id: string; name: string; institutionName: string; duration: string; }
export interface Institution { id: string; name: string; location: string; }
export interface Bursary { id: string; title: string; provider: string; status: 'DRAFT' | 'PUBLISHED' | 'PENDING' | 'APPROVED' | 'REJECTED'; }
export interface Application { id: string; opportunityType: 'BURSARY' | 'COURSE'; status: string; submittedAt: string; }
export interface Recommendation { id: string; type: 'CAREER' | 'BURSARY'; title: string; score: number; rationale: string; }
export interface Notification { id: string; title: string; message: string; read: boolean; }
export interface Subscription { id: string; plan: string; status: string; renewalDate: string; }
export interface Payment { id: string; amount: number; currency: string; status: string; paidAt: string; }

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiError {
  message: string;
  status?: number;
  details?: Record<string, string[]>;
}
