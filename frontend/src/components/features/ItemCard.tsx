import React from 'react';
import { Link } from 'react-router-dom';
import { Card } from '../common/Card';
import { RatingDisplay } from '../common/Rating';
import type { Item } from '../../types/item';

interface ItemCardProps {
  item: Item;
  className?: string;
}

export const ItemCard: React.FC<ItemCardProps> = ({ item, className = '' }) => {
  const typeLabel = item.type === 'MOVIE' ? 'Movie' : 'TV';

  return (
    <Link to={`/items/${item.id}`} className={className}>
      <Card className="group">
        <div className="relative aspect-[2/3] overflow-hidden bg-neutral-900">
          {item.posterUrl ? (
            <img
              src={item.posterUrl}
              alt={item.title}
              className="w-full h-full object-cover transition-transform duration-700 ease-out group-hover:scale-110"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <span className="text-neutral-700 text-4xl">?</span>
            </div>
          )}
          <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/20 to-transparent" />
          <div className="absolute bottom-0 left-0 right-0 p-4">
            <span className="inline-block px-2 py-0.5 text-[10px] tracking-wider uppercase bg-amber-500/20 text-amber-400 border border-amber-500/30 rounded-sm mb-2">
              {typeLabel}
            </span>
            <h3 className="text-neutral-100 font-medium text-lg leading-tight line-clamp-2">
              {item.title}
            </h3>
            <div className="mt-2">
              <RatingDisplay value={item.voteAverage} className="text-neutral-300" />
            </div>
          </div>
        </div>
      </Card>
    </Link>
  );
};