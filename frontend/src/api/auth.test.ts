import { describe, expect, it, vi, beforeEach } from 'vitest';
import type { AuthResponse, CreateUserRequest } from '../types';

const post = vi.fn();

vi.mock('./client', () => ({
  apiClient: {
    post,
  },
}));

describe('authApi', () => {
  beforeEach(() => {
    post.mockReset();
  });

  it('posts login credentials as usernameOrEmail and password', async () => {
    const response: AuthResponse = {
      token: 'mock-jwt-token',
      userId: 'user-1',
      username: 'demo-user',
      email: 'demo@example.com',
      authority: 'USER',
    };

    post.mockResolvedValue({ data: response });

    const { authApi } = await import('./auth');
    const result = await authApi.login('demo@example.com', 'secret123');

    expect(post).toHaveBeenCalledWith('/auth/login', {
      usernameOrEmail: 'demo@example.com',
      password: 'secret123',
    });
    expect(result).toEqual(response);
  });

  it('returns AuthResponse from register using backend request fields', async () => {
    const request: CreateUserRequest = {
      username: 'demo-user',
      email: 'demo@example.com',
      password: 'secret123',
      name: 'Demo User',
    };
    const response: AuthResponse = {
      token: 'mock-jwt-token',
      userId: 'user-1',
      username: 'demo-user',
      email: 'demo@example.com',
      authority: 'USER',
    };

    post.mockResolvedValue({ data: response });

    const { authApi } = await import('./auth');
    const result = await authApi.register(request);

    expect(post).toHaveBeenCalledWith('/auth/register', request);
    expect(result).toEqual(response);
  });
});
