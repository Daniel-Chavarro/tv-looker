import React, { useState } from 'react';

interface RatingProps {
  value: number;
  max?: number;
  interactive?: boolean;
  onChange?: (rating: number) => void;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const sizeClasses = {
  sm: 'w-3.5 h-3.5',
  md: 'w-5 h-5',
  lg: 'w-6 h-6',
};

export const Rating: React.FC<RatingProps> = ({
  value,
  max = 5,
  interactive = false,
  onChange,
  size = 'md',
  className = '',
}) => {
  const [hoverValue, setHoverValue] = useState<number | null>(null);
  
  const displayValue = interactive && hoverValue !== null ? hoverValue : value;

  const handleClick = (rating: number) => {
    if (interactive && onChange) {
      onChange(rating);
    }
  };

  const handleMouseEnter = (rating: number) => {
    if (interactive) {
      setHoverValue(rating);
    }
  };

  const handleMouseLeave = () => {
    setHoverValue(null);
  };

  return (
    <div 
      className={`flex items-center gap-1 ${className}`}
      onMouseLeave={handleMouseLeave}
    >
      {[...Array(max)].map((_, index) => {
        const rating = index + 1;
        const isFilled = rating <= displayValue;
        
        return (
<button
            type="button"
            disabled={!interactive}
            onClick={() => handleClick(rating)}
            onMouseEnter={() => handleMouseEnter(rating)}
            onKeyDown={(e) => {
              if (interactive && (e.key === 'Enter' || e.key === ' ')) {
                e.preventDefault();
                handleClick(rating);
              }
            }}
            aria-label={`Rate ${rating} out of ${max}`}
            aria-pressed={isFilled}
            className={`
              ${interactive ? 'cursor-pointer hover:scale-110 transition-transform' : 'cursor-default'}
              ${!interactive ? 'pointer-events-none' : ''}
            `}
          >
            <svg
              className={`
                ${sizeClasses[size]}
                ${isFilled 
                  ? 'text-amber-400 fill-current' 
                  : 'text-neutral-700 fill-transparent'
                }
                ${interactive && !isFilled && hoverValue !== null && rating <= hoverValue 
                  ? 'text-amber-600/50' 
                  : ''
                }
                transition-colors duration-200
              `}
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={1.5}
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"
              />
            </svg>
          </button>
        );
      })}
    </div>
  );
};

interface RatingDisplayProps {
  value: number;
  max?: number;
  count?: number;
  className?: string;
}

export const RatingDisplay: React.FC<RatingDisplayProps> = ({
  value,
  max = 5,
  count,
  className = '',
}) => {
  return (
    <div className={`flex items-center gap-3 ${className}`}>
      <Rating value={value} max={max} size="md" />
      <span className="text-neutral-400 text-sm">
        {value.toFixed(1)}
        {count !== undefined && (
          <span className="text-neutral-600 ml-1">({count})</span>
        )}
      </span>
    </div>
  );
};