import { apiClient } from '@/services/apiClient';
import type { Notification } from '@/types';
export const notificationService = {
  mine: () => apiClient.get<Notification[]>('/notifications').then((r) => r.data),
  markRead: (id: string) => apiClient.patch(`/notifications/${id}/read`),
};
