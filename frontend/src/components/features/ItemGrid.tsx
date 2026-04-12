import React from 'react';
import { ItemCard } from './ItemCard';
import { PageLoader } from '../common/Loader';
import type { Item } from '../../types/item';

interface ItemGridProps {
  items: Item[];
  loading?: boolean;
  emptyMessage?: string;
  className?: string;
}

export const ItemGrid: React.FC<ItemGridProps> = ({
  items,
  loading = false,
  emptyMessage = 'No items found',
  className = '',
}) => {
  if (loading) {
    return (
      <div className={className}>
        <PageLoader message="Loading..." />
      </div>
    );
  }

  if (!items || items.length === 0) {
    return (
      <div className={`flex items-center justify-center min-h-[300px] ${className}`}>
        <p className="text-neutral-500 tracking-widest uppercase text-sm">
          {emptyMessage}
        </p>
      </div>
    );
  }

  return (
    <div className={`grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6 ${className}`}>
      {items.map((item) => (
        <ItemCard key={item.id} item={item} />
      ))}
    </div>
  );
};