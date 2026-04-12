import React from 'react';
import { Link } from 'react-router-dom';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-neutral-900 border-t border-neutral-800 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div>
            <h3 className="text-sm font-semibold text-neutral-200 uppercase tracking-wider mb-4">
              Browse
            </h3>
            <ul className="space-y-2">
              <li>
                <Link to="/" className="text-neutral-400 hover:text-amber-400 transition-colors text-sm">
                  Home
                </Link>
              </li>
              <li>
                <Link to="/search" className="text-neutral-400 hover:text-amber-400 transition-colors text-sm">
                  Search
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-neutral-200 uppercase tracking-wider mb-4">
              Account
            </h3>
            <ul className="space-y-2">
              <li>
                <Link to="/login" className="text-neutral-400 hover:text-amber-400 transition-colors text-sm">
                  Login
                </Link>
              </li>
              <li>
                <Link to="/register" className="text-neutral-400 hover:text-amber-400 transition-colors text-sm">
                  Register
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-neutral-200 uppercase tracking-wider mb-4">
              User
            </h3>
            <ul className="space-y-2">
              <li>
                <Link to="/my-lists" className="text-neutral-400 hover:text-amber-400 transition-colors text-sm">
                  My Lists
                </Link>
              </li>
              <li>
                <Link to="/my-reviews" className="text-neutral-400 hover:text-amber-400 transition-colors text-sm">
                  My Reviews
                </Link>
              </li>
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-neutral-200 uppercase tracking-wider mb-4">
              About
            </h3>
            <ul className="space-y-2">
              <li>
                <span className="text-neutral-400 text-sm">
                  TV Looker
                </span>
              </li>
              <li>
                <span className="text-neutral-500 text-xs">
                  Track and discover TV shows
                </span>
              </li>
            </ul>
          </div>
        </div>

        <div className="border-t border-neutral-800 mt-8 pt-8 flex flex-col md:flex-row justify-between items-center">
          <p className="text-neutral-500 text-sm">
            &copy; {new Date().getFullYear()} TV Looker. All rights reserved.
          </p>
          <p className="text-neutral-600 text-xs mt-2 md:mt-0">
            Built with passion for TV shows
          </p>
        </div>
      </div>
    </footer>
  );
};