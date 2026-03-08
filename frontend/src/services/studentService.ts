import { apiClient } from '@/services/apiClient';
import type { Application, Recommendation, StudentProfile } from '@/types';

export const studentService = {
  getMe: () => apiClient.get<StudentProfile>('/students/me').then((r) => r.data),
  updateMe: (payload: Partial<StudentProfile>) => apiClient.put<StudentProfile>('/students/me', payload).then((r) => r.data),
  getDashboard: () => apiClient.get('/students/me/dashboard').then((r) => r.data),
  getSavedItems: () => apiClient.get('/students/me/saved-items').then((r) => r.data),
  addSavedItem: (payload: Record<string, string>) => apiClient.post('/students/me/saved-items', payload),
  getRecommendations: () => apiClient.get<Recommendation[]>('/recommendations/me').then((r) => r.data),
  getApplications: () => apiClient.get<Application[]>('/applications/me').then((r) => r.data),
};
