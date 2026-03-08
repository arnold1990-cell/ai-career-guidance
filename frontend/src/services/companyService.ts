import { apiClient } from '@/services/apiClient';

export const companyService = {
  getMe: () => apiClient.get('/companies/me').then((r) => r.data),
  updateMe: (payload: Record<string, unknown>) => apiClient.put('/companies/me', payload).then((r) => r.data),
  getBursaries: () => apiClient.get('/companies/bursaries').then((r) => r.data),
  createBursary: (payload: Record<string, unknown>) => apiClient.post('/companies/bursaries', payload),
  updateBursary: (id: string, payload: Record<string, unknown>) => apiClient.put(`/companies/bursaries/${id}`, payload),
  toggleBursaryStatus: (id: string, status: 'PUBLISHED' | 'DRAFT') => apiClient.patch(`/companies/bursaries/${id}/status`, { status }),
  getApplicants: () => apiClient.get('/companies/applicants').then((r) => r.data),
};
