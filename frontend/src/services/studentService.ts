import { apiClient } from '@/services/apiClient';
import type { OpportunityType, StudentProfile, UnifiedOpportunity } from '@/types';

export const studentService = {
  getMe: () => apiClient.get<StudentProfile>('/student/profile').then((r) => r.data),
  updateMe: (payload: Partial<StudentProfile>) => apiClient.put<StudentProfile>('/student/profile', payload).then((r) => r.data),
  uploadCv: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post<StudentProfile>('/student/profile/cv', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data);
  },
  uploadTranscript: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post<StudentProfile>('/student/profile/transcript', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data);
  },
  getDashboard: () => apiClient.get('/student/dashboard').then((r) => r.data),
  searchOpportunities: (params?: Record<string, string | number>) => apiClient.get<UnifiedOpportunity[]>('/student/opportunities', { params }).then((r) => r.data),
  saveOpportunity: (type: Exclude<OpportunityType, 'ALL'>, opportunityId: string, title: string) => apiClient.post(`/student/opportunities/${type}/${opportunityId}/save`, { title }),
  unsaveOpportunity: (type: Exclude<OpportunityType, 'ALL'>, opportunityId: string) => apiClient.delete(`/student/opportunities/${type}/${opportunityId}/save`),
  saveCareer: (careerId: string) => apiClient.post(`/student/careers/${careerId}/save`),
  unsaveCareer: (careerId: string) => apiClient.delete(`/student/careers/${careerId}/save`),
  savedCareers: () => apiClient.get<{ items: string[] }>('/student/careers/saved').then((r) => r.data.items),
  saveBursary: (bursaryId: string) => apiClient.post(`/student/bursaries/${bursaryId}/save`),
  unsaveBursary: (bursaryId: string) => apiClient.delete(`/student/bursaries/${bursaryId}/save`),
  savedBursaries: () => apiClient.get<{ items: string[] }>('/student/bursaries/saved').then((r) => r.data.items),
};
