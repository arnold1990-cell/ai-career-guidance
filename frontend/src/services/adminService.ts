import { apiClient } from '@/services/apiClient';

export const adminService = {
  getUsers: (params?: Record<string, string | number>) => apiClient.get('/admin/users', { params }).then((r) => r.data),
  updateUserStatus: (id: string, active: boolean) => apiClient.patch(`/admin/users/${id}/status`, { active }),
  getRoles: () => apiClient.get('/admin/roles').then((r) => r.data),
  createRole: (payload: Record<string, unknown>) => apiClient.post('/admin/roles', payload),
  updateRole: (id: string, payload: Record<string, unknown>) => apiClient.put(`/admin/roles/${id}`, payload),
  deleteRole: (id: string) => apiClient.delete(`/admin/roles/${id}`),
  getPendingBursaries: () => apiClient.get('/admin/bursaries/pending').then((r) => r.data),
  reviewBursary: (id: string, decision: 'APPROVED' | 'REJECTED', comment?: string) => apiClient.patch(`/admin/bursaries/${id}/review`, { decision, comment }),
  listPendingCompanies: () => apiClient.get('/admin/companies/pending').then((r) => r.data),
  getCompanyDetail: (id: string) => apiClient.get(`/admin/companies/${id}`).then((r) => r.data),
  approveCompany: (id: string, notes: string) => apiClient.patch(`/admin/companies/${id}/approve`, { notes }).then((r) => r.data),
  rejectCompany: (id: string, notes: string) => apiClient.patch(`/admin/companies/${id}/reject`, { notes }).then((r) => r.data),
  requestCompanyMoreInfo: (id: string, notes: string) => apiClient.patch(`/admin/companies/${id}/more-info`, { notes }).then((r) => r.data),
};
