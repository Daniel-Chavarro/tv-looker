import React from 'react';

interface CardProps {
  children: React.ReactNode;
  onClick?: () => void;
  className?: string;
}

export const Card: React.FC<CardProps> = ({
  children,
  onClick,
  className = '',
}) => {
  const Component = onClick ? 'button' : 'div';
  
  return (
    <Component
      onClick={onClick}
      type={onClick ? 'button' : undefined}
      className={`
        relative bg-neutral-900/60 backdrop-blur-sm
        border border-neutral-800/60
        rounded-sm overflow-hidden
        transition-all duration-500 ease-out
        ${onClick 
          ? 'hover:border-amber-500/30 hover:shadow-lg hover:shadow-amber-500/5 cursor-pointer hover:-translate-y-1' 
          : ''
        }
        ${className}
      `}
    >
      {children}
      {onClick && (
        <div className="absolute inset-0 bg-gradient-to-t from-amber-500/5 to-transparent opacity-0 hover:opacity-100 transition-opacity duration-500 pointer-events-none" />
      )}
    </Component>
  );
};

interface CardHeaderProps {
  children: React.ReactNode;
  className?: string;
}

export const CardHeader: React.FC<CardHeaderProps> = ({ children, className = '' }) => (
  <div className={`px-6 py-4 border-b border-neutral-800/50 ${className}`}>{children}</div>
);

interface CardBodyProps {
  children: React.ReactNode;
  className?: string;
}

export const CardBody: React.FC<CardBodyProps> = ({ children, className = '' }) => (
  <div className={`px-6 py-5 ${className}`}>{children}</div>
);

interface CardFooterProps {
  children: React.ReactNode;
  className?: string;
}

export const CardFooter: React.FC<CardFooterProps> = ({ children, className = '' }) => (
  <div className={`px-6 py-4 border-t border-neutral-800/50 bg-neutral-900/30 ${className}`}>{children}</div>
);