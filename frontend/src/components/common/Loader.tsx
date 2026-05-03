import React from 'react';

interface LoaderProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const sizeClasses = {
  sm: 'w-5 h-5 border-2',
  md: 'w-8 h-8 border-2',
  lg: 'w-12 h-12 border-3',
};

export const Loader: React.FC<LoaderProps> = ({ size = 'md', className = '' }) => {
  return (
    <div className={`flex items-center justify-center ${className}`}>
      <div
        className={`
          ${sizeClasses[size]}
          border-amber-500/20 border-t-amber-500
          rounded-full animate-spin
        `}
      />
    </div>
  );
};

interface PageLoaderProps {
  message?: string;
}

export const PageLoader: React.FC<PageLoaderProps> = ({ message }) => {
  return (
    <div className="flex flex-col items-center justify-center min-h-[200px] gap-4">
      <Loader size="lg" />
      {message && (
        <p className="text-neutral-500 text-sm tracking-widest uppercase animate-pulse">
          {message}
        </p>
      )}
    </div>
  );
};