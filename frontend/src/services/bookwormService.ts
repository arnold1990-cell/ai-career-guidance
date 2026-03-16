import { apiClient } from '@/services/apiClient';

export interface BookwormChatRequest {
  question: string;
  studentProfileId?: string;
}

export interface BookwormUniversityLink {
  name: string;
  website?: string;
}

export interface BookwormChatResponse {
  answer: string;
  recommendedCareers: string[];
  recommendedProgrammes: string[];
  recommendedUniversities: BookwormUniversityLink[];
  links: string[];
}

export const bookwormService = {
  chat: (payload: BookwormChatRequest) => apiClient.post<BookwormChatResponse>('/bookworm/chat', payload).then((r) => r.data),
};
