import type { UUID } from "crypto";

export type User = {
  id: UUID
  username: string;
  email: string;
  name?: string;
  // Just meanwhile testing, will be removed in the future when we implement proper authentication
  password: string;
  createdAt: string;
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