import React, { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { authApi, toUserFromAuthResponse } from '../api/auth';
import { AUTH_STORAGE_KEYS, clearAuthSession } from '../api/client';
import type { AuthUser } from '../types';

interface AuthContextType {
  user: AuthUser | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<void>;
  register: (email: string, password: string, username: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const isAuthenticated = !!user;

  const persistAuthSession = (nextUser: AuthUser, token: string) => {
    localStorage.setItem(AUTH_STORAGE_KEYS.token, token);
    localStorage.setItem(AUTH_STORAGE_KEYS.user, JSON.stringify(nextUser));
  };

  const readStoredUser = (): AuthUser | null => {
    const rawUser = localStorage.getItem(AUTH_STORAGE_KEYS.user);
    if (!rawUser) {
      return null;
    }

    try {
      return JSON.parse(rawUser) as AuthUser;
    } catch {
      return null;
    }
  };

  useEffect(() => {
    const token = localStorage.getItem(AUTH_STORAGE_KEYS.token);
    const storedUser = readStoredUser();

    if (token && storedUser) {
      setUser(storedUser);
    } else if (token) {
      clearAuthSession();
    }

    setIsLoading(false);
  }, []);

  const login = async (usernameOrEmail: string, password: string) => {
    const response = await authApi.login(usernameOrEmail, password);
    const nextUser = toUserFromAuthResponse(response);

    persistAuthSession(nextUser, response.token);
    setUser(nextUser);
  };

  const register = async (email: string, password: string, username: string) => {
    const response = await authApi.register({ email, password, username });
    const nextUser = toUserFromAuthResponse(response);

    persistAuthSession(nextUser, response.token);
    setUser(nextUser);
  };

  const logout = async () => {
    await authApi.logout();
    clearAuthSession();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, isLoading, isAuthenticated, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
