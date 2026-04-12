import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Button } from '../common/Button';

export const Header: React.FC = () => {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  return (
    <header className="bg-neutral-900/95 backdrop-blur-sm border-b border-neutral-800 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <Link to="/" className="flex items-center space-x-2 group">
            <div className="w-8 h-8 bg-gradient-to-br from-amber-400 to-amber-600 rounded-sm flex items-center justify-center shadow-lg shadow-amber-500/20 group-hover:shadow-amber-500/40 transition-shadow">
              <svg className="w-5 h-5 text-neutral-900" fill="currentColor" viewBox="0 0 20 20">
                <path d="M10 2a8 8 0 100 16 8 8 0 000-16zm0 14a6 6 0 110-12 6 6 0 010 12z" />
                <path d="M10 5v4l3 2" stroke="currentColor" strokeWidth="1.5" fill="none" />
              </svg>
            </div>
            <span className="text-xl font-bold text-neutral-100 tracking-wide">
              TV Looker
            </span>
          </Link>

          <nav className="hidden md:flex items-center space-x-1">
            <Link
              to="/"
              className="px-4 py-2 text-neutral-400 hover:text-amber-400 hover:bg-neutral-800/50 rounded-sm transition-colors duration-200 text-sm font-medium"
            >
              Home
            </Link>

            {isAuthenticated && (
              <>
                <Link
                  to="/recommendations"
                  className="px-4 py-2 text-neutral-400 hover:text-amber-400 hover:bg-neutral-800/50 rounded-sm transition-colors duration-200 text-sm font-medium"
                >
                  Recommendations
                </Link>
                <Link
                  to="/my-lists"
                  className="px-4 py-2 text-neutral-400 hover:text-amber-400 hover:bg-neutral-800/50 rounded-sm transition-colors duration-200 text-sm font-medium"
                >
                  My Lists
                </Link>
                <Link
                  to="/my-reviews"
                  className="px-4 py-2 text-neutral-400 hover:text-amber-400 hover:bg-neutral-800/50 rounded-sm transition-colors duration-200 text-sm font-medium"
                >
                  My Reviews
                </Link>
              </>
            )}
          </nav>

          <div className="flex items-center space-x-3">
            {isAuthenticated ? (
              <div className="flex items-center space-x-3">
                <span className="text-sm text-neutral-400 hidden sm:block">
                  {user?.username}
                </span>
                <Button variant="secondary" onClick={handleLogout} className="py-2 px-4 text-xs">
                  Logout
                </Button>
              </div>
            ) : (
              <>
                <Button variant="ghost" onClick={() => navigate('/login')} className="py-2 px-4 text-xs">
                  Login
                </Button>
                <Button variant="primary" onClick={() => navigate('/register')} className="py-2 px-4 text-xs">
                  Register
                </Button>
              </>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};