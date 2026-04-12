export type User = {
  id: string;
  username: string;
  email: string;
  name?: string;
  createdAt: string;
  updatedAt: string;
}

export type CreateUserRequest = {
  username: string;
  email: string;
  password: string;
  name?: string;
}

export type UpdateUserRequest = {
  password?: string;
  name?: string;
  email?: string;
}

export type UserResponse = {
  data: User;
}
