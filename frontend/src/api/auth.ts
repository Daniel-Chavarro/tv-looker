import { apiClient } from './client';
import type { AuthResponse, AuthUser, CreateUserRequest } from '../types';

export function toUserFromAuthResponse(response: AuthResponse): AuthUser {
  return {
    id: response.userId,
    username: response.username,
    email: response.email,
    authority: response.authority,
  };
}

export const authApi = {
  login: async (usernameOrEmail: string, password: string): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/auth/login', { usernameOrEmail, password });
    return response.data;
  },

  register: async (data: CreateUserRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/auth/register', data);
    return response.data;
  },

  logout: async (): Promise<void> => {
    return Promise.resolve();
  },
};
