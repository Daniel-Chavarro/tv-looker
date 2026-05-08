import type { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ApiError } from '../types';
import { API_BASE_URL, AUTH_STORAGE_KEYS, apiClient, attachAuthHeader, clearAuthSession, handleApiError, redirectToLogin } from './client';

describe('apiClient', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('uses the /api/v1 base URL', () => {
    expect(API_BASE_URL).toBe('/api/v1');
    expect(apiClient.defaults.baseURL).toBe('/api/v1');
  });

  it('adds the bearer token from localStorage', () => {
    localStorage.setItem(AUTH_STORAGE_KEYS.token, 'mock-jwt-token');

    const config = attachAuthHeader({ headers: {} } as InternalAxiosRequestConfig);

    expect(config.headers.Authorization).toBe('Bearer mock-jwt-token');
  });

  it('clears auth session and redirects on 401 responses', async () => {
    const assign = vi.fn();
    vi.stubGlobal('location', { assign });
    localStorage.setItem(AUTH_STORAGE_KEYS.token, 'mock-jwt-token');
    localStorage.setItem(AUTH_STORAGE_KEYS.user, JSON.stringify({ id: 'user-1' }));

    const error = {
      response: {
        status: 401,
      },
    } as AxiosError<ApiError>;

    await expect(handleApiError(error)).rejects.toBe(error);

    expect(localStorage.getItem(AUTH_STORAGE_KEYS.token)).toBeNull();
    expect(localStorage.getItem(AUTH_STORAGE_KEYS.user)).toBeNull();
    expect(assign).toHaveBeenCalledWith('/login');
  });

  it('exposes redirect helper for login redirects', () => {
    expect(typeof redirectToLogin).toBe('function');
    expect(typeof clearAuthSession).toBe('function');
  });
});
