import { apiClient } from './client';
import type { User, UpdateUserRequest, UserResponse } from '../types';

export const usersApi = {
  getUser: async (id: string): Promise<UserResponse> => {
    const response = await apiClient.get<UserResponse>(`/users/${id}`);
    return response.data;
  },

  updateUser: async (id: string, data: UpdateUserRequest): Promise<UserResponse> => {
    const response = await apiClient.patch<UserResponse>(`/users/${id}`, data);
    return response.data;
  },

  deleteUser: async (id: string): Promise<void> => {
    await apiClient.delete(`/users/${id}`);
  },
};