import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const Input: React.FC<InputProps> = ({
  label,
  error,
  className = '',
  id,
  ...props
}) => {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-');

  return (
    <div className="w-full">
      {label && (
        <label
          htmlFor={inputId}
          className="block mb-2 text-xs tracking-widest uppercase text-neutral-400"
        >
          {label}
        </label>
      )}
<input
        id={inputId}
        aria-describedby={error ? `${inputId}-error` : undefined}
        className={`
          w-full px-4 py-3 bg-neutral-900/80 border 
          text-neutral-100 placeholder-neutral-500
          rounded-sm text-sm tracking-wide
          transition-all duration-300 ease-out
          focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500-50
          hover:border-neutral-600
          ${error 
            ? 'border-red-800/80 focus:border-red-600' 
            : 'border-neutral-800'
          }
          ${className}
        `}
        {...props}
      />
      {error && (
        <p id={`${inputId}-error`} className="mt-2 text-xs text-red-400 tracking-wide">{error}</p>
      )}
    </div>
  );
};