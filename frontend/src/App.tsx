import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { Layout } from './components/layout/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { NotFound } from './pages/NotFound';
import { Home } from './pages/Home';
import { ItemDetail } from './pages/ItemDetail';
import { Recommendations } from './pages/Recommendations';
import { MyLists } from './pages/MyLists';
import { ListDetail } from './pages/ListDetail';
import { MyReviews } from './pages/MyReviews';
import { Profile } from './pages/Profile';
import { AdminTmdb } from './pages/AdminTmdb';
import { Login } from './pages/auth/Login';
import { Register } from './pages/auth/Register';

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
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route
                path="/items/:id"
                element={
                  <ProtectedRoute>
                    <ItemDetail />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/recommendations"
                element={
                  <ProtectedRoute>
                    <Recommendations />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/lists"
                element={
                  <ProtectedRoute>
                    <MyLists />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/lists/:id"
                element={
                  <ProtectedRoute>
                    <ListDetail />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/reviews"
                element={
                  <ProtectedRoute>
                    <MyReviews />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/profile"
                element={
                  <ProtectedRoute>
                    <Profile />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/admin/tmdb"
                element={
                  <ProtectedRoute requiredAuthority="ADMIN">
                    <AdminTmdb />
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
