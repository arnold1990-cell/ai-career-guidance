import { apiClient } from '@/services/apiClient';
import type { StudentProfile } from '@/types';

export const studentService = {
  getMe: () => apiClient.get<StudentProfile>('/students/me').then((r) => r.data),
  updateMe: (payload: Partial<StudentProfile>) => apiClient.put<StudentProfile>('/students/me', payload).then((r) => r.data),
  upload: (file: File, type: 'cv' | 'transcript') => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);
    return apiClient.post<StudentProfile>('/students/me/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } }).then((r) => r.data);
  },
  getDashboard: () => apiClient.get('/students/me/dashboard').then((r) => r.data),
  saveCareer: (careerId: string) => apiClient.post(`/students/me/saved-careers/${careerId}`),
  saveBursary: (bursaryId: string) => apiClient.post(`/students/me/saved-bursaries/${bursaryId}`),
};
