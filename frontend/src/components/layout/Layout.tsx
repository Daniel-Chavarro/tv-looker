import React, { type ReactNode } from 'react';
import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import { Footer } from './Footer';

interface LayoutProps {
  children?: ReactNode;
}

export const Layout: React.FC<LayoutProps> = ({ children }) => {
  return (
    <div className="flex flex-col min-h-screen bg-neutral-950">
      <Header />
      <main className="flex-grow">
        {children || <Outlet />}
      </main>
      <Footer />
    </div>
  );
};