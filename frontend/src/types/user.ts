export interface User {
  id: string;
  username: string;
  email: string;
  name?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  name?: string;
}

export interface UpdateUserRequest {
  password?: string;
  name?: string;
  email?: string;
}

export interface UserResponse {
  data: User;
}
