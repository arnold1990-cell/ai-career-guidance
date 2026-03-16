import { apiClient } from '@/services/apiClient';
import type { BookwormChatRequest, BookwormChatResponse } from '@/types';

export const bookwormService = {
  chat: (payload: BookwormChatRequest) =>
    apiClient.post<BookwormChatResponse>('/bookworm/chat', payload).then((r) => r.data),
  suggestions: () => apiClient.get<string[]>('/bookworm/suggestions').then((r) => r.data),
};
