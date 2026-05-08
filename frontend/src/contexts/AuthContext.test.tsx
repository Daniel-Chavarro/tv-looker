import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import type { AuthResponse } from '../types';
import { AuthProvider, useAuth } from './AuthContext';
import { AUTH_STORAGE_KEYS } from '../api/client';

const authApiMocks = vi.hoisted(() => ({
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('../api/auth', async () => {
  const actual = await vi.importActual<typeof import('../api/auth')>('../api/auth');

  return {
    ...actual,
    authApi: {
      login: authApiMocks.login,
      register: authApiMocks.register,
      logout: authApiMocks.logout,
    },
  };
});

const authResponse: AuthResponse = {
  token: 'mock-jwt-token',
  userId: 'user-123',
  username: 'demo-user',
  email: 'demo@example.com',
  authority: 'USER',
};

describe('AuthContext', () => {
  beforeEach(() => {
    authApiMocks.login.mockReset();
    authApiMocks.register.mockReset();
    authApiMocks.logout.mockReset();
    authApiMocks.logout.mockResolvedValue(undefined);
  });

  it('stores the backend token and maps userId to user.id on login', async () => {
    authApiMocks.login.mockResolvedValue(authResponse);

    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    });

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => {
      await result.current.login('demo@example.com', 'secret123');
    });

    expect(localStorage.getItem(AUTH_STORAGE_KEYS.token)).toBe('mock-jwt-token');
    expect(localStorage.getItem(AUTH_STORAGE_KEYS.token)).not.toBe('user-123');
    expect(result.current.user).toEqual({
      id: 'user-123',
      username: 'demo-user',
      email: 'demo@example.com',
      authority: 'USER',
    });
  });

  it('stores the backend token and auth snapshot on register', async () => {
    authApiMocks.register.mockResolvedValue(authResponse);

    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    });

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(async () => {
      await result.current.register('demo@example.com', 'secret123', 'demo-user');
    });

    expect(localStorage.getItem(AUTH_STORAGE_KEYS.token)).toBe('mock-jwt-token');
    expect(localStorage.getItem(AUTH_STORAGE_KEYS.user)).toBe(JSON.stringify({
      id: 'user-123',
      username: 'demo-user',
      email: 'demo@example.com',
      authority: 'USER',
    }));
  });

  it('hydrates auth state from stored token and user snapshot', async () => {
    localStorage.setItem(AUTH_STORAGE_KEYS.token, 'mock-jwt-token');
    localStorage.setItem(AUTH_STORAGE_KEYS.user, JSON.stringify({
      id: 'user-123',
      username: 'demo-user',
      email: 'demo@example.com',
      authority: 'USER',
    }));

    const { result } = renderHook(() => useAuth(), {
      wrapper: AuthProvider,
    });

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.user?.id).toBe('user-123');
  });
});
