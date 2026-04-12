import React from 'react';
import { Button } from './Button';

interface ErrorMessageProps {
  title?: string;
  message: string;
  retry?: () => void;
  className?: string;
}

export const ErrorMessage: React.FC<ErrorMessageProps> = ({
  title = 'Something went wrong',
  message,
  retry,
  className = '',
}) => {
  return (
    <div
      className={`
        flex flex-col items-center justify-center
        p-8 text-center
        bg-neutral-900/40 border border-red-900/30 rounded-sm
        ${className}
      `}
    >
      <div className="w-12 h-12 mb-4 flex items-center justify-center">
        <svg
          className="w-full h-full text-red-500/70"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={1.5}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
          />
        </svg>
      </div>
      <h3 className="text-neutral-200 text-lg font-medium tracking-wide mb-2">
        {title}
      </h3>
      <p className="text-neutral-500 text-sm max-w-md mb-6">{message}</p>
      {retry && (
        <Button variant="secondary" onClick={retry} className="text-sm">
          Try Again
        </Button>
      )}
    </div>
  );
};