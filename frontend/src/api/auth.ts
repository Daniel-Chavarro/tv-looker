import { apiClient } from './client';
import type { User, CreateUserRequest, UserResponse } from '../types';

export const authApi = {
  login: async (email: string, password: string): Promise<UserResponse> => {
    const response = await apiClient.post<UserResponse>('/auth/login', { email, password });
    return response.data;
  },

  register: async (data: CreateUserRequest): Promise<UserResponse> => {
    const response = await apiClient.post<UserResponse>('/auth/register', data);
    return response.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/auth/logout');
    localStorage.removeItem('token');
  },

  getCurrentUser: async (): Promise<UserResponse> => {
    const response = await apiClient.get<UserResponse>('/auth/me');
    return response.data;
  },
};