export type UUID = string;

export type UserAuthority = 'USER' | 'ADMIN';

export type AuthUser = {
  id: UUID;
  username: string;
  email: string;
  authority: UserAuthority;
  name?: string;
};

export type User = AuthUser & {
  name?: string;
  createdAt: string;
};

export type AuthResponse = {
  token: string;
  userId: UUID;
  username: string;
  email: string;
  authority: UserAuthority;
};

export type CreateUserRequest = {
  username: string;
  email: string;
  password: string;
  name?: string;
};

export type UpdateUserRequest = {
  password?: string;
  name?: string;
  email?: string;
};
