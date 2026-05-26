import { Navigate, useLocation } from 'react-router-dom';
import { type ReactNode } from 'react';
import { useAuth } from '../hooks/useAuth';
import type { UserAuthority } from '../types';

interface ProtectedRouteProps {
  children: ReactNode;
  requiredAuthority?: UserAuthority;
}

export function ProtectedRoute({ children, requiredAuthority }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, user } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requiredAuthority && user?.authority !== requiredAuthority) {
    return <div role="alert">Forbidden</div>;
  }

  return <>{children}</>;
}
