import { apiClient } from './client';
import type { User, CreateUserRequest} from '../types';

export const authApi = {
  login: async (email: string, password: string): Promise<User> => {
    const response = await apiClient.post<User>('/auth/login', { email, password });
    return response.data;
  },

  register: async (data: CreateUserRequest): Promise<User> => {
    const response = await apiClient.post<User>('/auth/register', data);
    return response.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/auth/logout');
    localStorage.removeItem('token');
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await apiClient.get<User>('/auth/me');
    return response.data;
  },
};