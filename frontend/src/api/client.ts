import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiError } from '../types';

export const API_BASE_URL = '/api/v1';

export const AUTH_STORAGE_KEYS = {
  token: 'token',
  user: 'authUser',
} as const;

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

export function attachAuthHeader(config: InternalAxiosRequestConfig) {
  const token = localStorage.getItem(AUTH_STORAGE_KEYS.token);
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}

export function clearAuthSession() {
  localStorage.removeItem(AUTH_STORAGE_KEYS.token);
  localStorage.removeItem(AUTH_STORAGE_KEYS.user);
}

export function redirectToLogin() {
  window.location.assign('/login');
}

export async function handleApiError(error: AxiosError<ApiError>) {
  if (error.response?.status === 401) {
    clearAuthSession();
    redirectToLogin();
  }

  return Promise.reject(error);
}

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => attachAuthHeader(config),
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  handleApiError
);
