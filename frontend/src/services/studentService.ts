import { apiClient } from '@/services/apiClient';
import type { StudentProfile } from '@/types';

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
  saveCareer: (careerId: string) => apiClient.post(`/student/careers/${careerId}/save`),
  unsaveCareer: (careerId: string) => apiClient.delete(`/student/careers/${careerId}/save`),
  savedCareers: () => apiClient.get<{ items: string[] }>('/student/careers/saved').then((r) => r.data.items),
  saveOpportunity: (type: string, opportunityId: string) => apiClient.post(`/student/opportunities/${type}/${opportunityId}/save`),
  unsaveOpportunity: (type: string, opportunityId: string) => apiClient.delete(`/student/opportunities/${type}/${opportunityId}/save`),
  savedOpportunities: () => apiClient.get<{ items: string[] }>('/student/opportunities/saved').then((r) => r.data.items),
  saveBursary: (bursaryId: string) => apiClient.post(`/student/bursaries/${bursaryId}/save`),
  unsaveBursary: (bursaryId: string) => apiClient.delete(`/student/bursaries/${bursaryId}/save`),
  savedBursaries: () => apiClient.get<{ items: string[] }>('/student/bursaries/saved').then((r) => r.data.items),
};
