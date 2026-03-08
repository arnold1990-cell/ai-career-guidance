import { apiClient } from '@/services/apiClient';
import type { AuthResponse, CompanyRegisterPayload, StudentRegisterPayload } from '@/types';

export const authService = {
  login: (payload: { email: string; password: string }) => apiClient.post<AuthResponse>('/auth/login', payload).then((r) => r.data),
  registerStudent: (payload: StudentRegisterPayload) => apiClient.post<AuthResponse>('/auth/register/student', payload).then((r) => r.data),
  registerCompany: (payload: CompanyRegisterPayload) => apiClient.post<AuthResponse>('/auth/register/company', payload).then((r) => r.data),
  forgotPassword: (email: string) => apiClient.post('/auth/forgot-password', { email }),
  resetPassword: (payload: { token: string; newPassword: string }) => apiClient.post('/auth/reset-password', payload),
  logout: () => apiClient.post('/auth/logout'),
};
