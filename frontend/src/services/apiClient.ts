import axios from 'axios';
import { authStore } from '@/features/auth/authStore';
import type { ApiError } from '@/types';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

export const apiClient = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = authStore.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const requestMethod = error.config?.method?.toUpperCase() ?? 'UNKNOWN';
    const requestUrl = error.config?.url ?? 'UNKNOWN_URL';
    const responseStatus = error.response?.status;

    if (error.response?.status === 401 && authStore.getRefreshToken()) {
      try {
        const response = await axios.post(`${baseURL}/auth/refresh`, {
          refreshToken: authStore.getRefreshToken(),
        });
        authStore.setTokens(response.data.accessToken, response.data.refreshToken);
        error.config.headers.Authorization = `Bearer ${response.data.accessToken}`;
        return apiClient.request(error.config);
      } catch {
        authStore.clear();
      }
    }
    if (import.meta.env.DEV) {
      console.error(`[api] ${requestMethod} ${requestUrl} failed`, { status: responseStatus, response: error.response?.data });
    }

    const normalized: ApiError = {
      message: error.response?.data?.message
        ?? error.response?.data?.error
        ?? error.message
        ?? 'Unexpected error occurred',
      status: error.response?.status,
      details: error.response?.data?.errors,
    };
    return Promise.reject(normalized);
  },
);
