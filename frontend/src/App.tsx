import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { Layout } from './components/layout/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { NotFound } from './pages/NotFound';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,
    },
  },
});

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<Layout />}>
              <Route path="/" element={<Navigate to="/items" replace />} />
              <Route path="/login" element={<div>Login Page</div>} />
              <Route path="/register" element={<div>Register Page</div>} />
              <Route
                path="/items"
                element={
                  <ProtectedRoute>
                    <div>Items Page</div>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/items/:id"
                element={
                  <ProtectedRoute>
                    <div>Item Detail Page</div>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/lists"
                element={
                  <ProtectedRoute>
                    <div>Lists Page</div>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/lists/:id"
                element={
                  <ProtectedRoute>
                    <div>List Detail Page</div>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/profile"
                element={
                  <ProtectedRoute>
                    <div>Profile Page</div>
                  </ProtectedRoute>
                }
              />
              <Route path="*" element={<NotFound />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}