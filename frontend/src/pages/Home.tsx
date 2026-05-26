import { startTransition, useState, type FormEvent } from 'react';
import { useItems } from '../hooks/useItems';
import { ItemGrid } from '../components/features/ItemGrid';
import { PageLoader } from '../components/common/Loader';
import { ErrorMessage } from '../components/common/ErrorMessage';
import { PaginationControls } from '../components/features/PaginationControls';
import type { ItemQueryParams } from '../api/items';

export function Home() {
  const [searchInput, setSearchInput] = useState('');
  const [filtersInput, setFiltersInput] = useState<{
    type: '' | 'MOVIE' | 'TV';
    genreId: string;
    year: string;
    rating: string;
  }>({ type: '', genreId: '', year: '', rating: '' });
  const [query, setQuery] = useState<ItemQueryParams>({ page: 1, size: 20 });

  const { data, isLoading, error, refetch } = useItems(query);

  const applyQuery = (nextQuery: ItemQueryParams) => {
    startTransition(() => {
      setQuery({ ...nextQuery, page: 1, size: 20 });
    });
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    applyQuery({
      search: searchInput.trim() || undefined,
      type: filtersInput.type || undefined,
      genreId: filtersInput.genreId ? Number(filtersInput.genreId) : undefined,
      year: filtersInput.year ? Number(filtersInput.year) : undefined,
      rating: filtersInput.rating ? Number(filtersInput.rating) : undefined,
    });
  };

  const handleFilterChange = <K extends keyof typeof filtersInput>(field: K, value: (typeof filtersInput)[K]) => {
    const nextFilters = { ...filtersInput, [field]: value };
    setFiltersInput(nextFilters);
    applyQuery({
      search: searchInput.trim() || undefined,
      type: nextFilters.type || undefined,
      genreId: nextFilters.genreId ? Number(nextFilters.genreId) : undefined,
      year: nextFilters.year ? Number(nextFilters.year) : undefined,
      rating: nextFilters.rating ? Number(nextFilters.rating) : undefined,
    });
  };

  const handlePageChange = (page: number) => {
    startTransition(() => {
      setQuery((current) => ({ ...current, page }));
    });
  };

  if (isLoading) {
    return <PageLoader message="Loading items..." />;
  }

  if (error) {
    return (
      <ErrorMessage
        message="Failed to load items. Please try again."
        retry={() => refetch()}
      />
    );
  }

  const items = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="container mx-auto px-4 py-8">
      <form onSubmit={handleSubmit} className="mb-8 space-y-4 rounded border border-neutral-800 p-4">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
          <input
            type="search"
            name="search"
            aria-label="Search items"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Search items"
            className="rounded border border-neutral-700 bg-transparent px-3 py-2 text-neutral-200"
          />
          <select
            name="type"
            aria-label="Type"
            value={filtersInput.type}
            onChange={(event) => handleFilterChange('type', event.target.value as typeof filtersInput.type)}
            className="rounded border border-neutral-700 bg-transparent px-3 py-2 text-neutral-200"
          >
            <option value="">Type</option>
            <option value="MOVIE">Movie</option>
            <option value="TV">TV</option>
          </select>
          <input
            type="number"
            name="genreId"
            aria-label="Genre ID"
            value={filtersInput.genreId}
            onChange={(event) => handleFilterChange('genreId', event.target.value)}
            placeholder="Genre ID"
            className="rounded border border-neutral-700 bg-transparent px-3 py-2 text-neutral-200"
          />
          <input
            type="number"
            name="year"
            aria-label="Year"
            value={filtersInput.year}
            onChange={(event) => handleFilterChange('year', event.target.value)}
            placeholder="Year"
            className="rounded border border-neutral-700 bg-transparent px-3 py-2 text-neutral-200"
          />
          <input
            type="number"
            name="rating"
            aria-label="Rating"
            value={filtersInput.rating}
            onChange={(event) => handleFilterChange('rating', event.target.value)}
            placeholder="Rating"
            className="rounded border border-neutral-700 bg-transparent px-3 py-2 text-neutral-200"
          />
        </div>
        <button
          type="submit"
          className="rounded bg-neutral-200 px-4 py-2 text-sm font-medium text-neutral-900"
        >
          Search
        </button>
      </form>

      <section className="mb-12">
        <h2 className="text-2xl font-semibold text-neutral-200 tracking-wide mb-6">
          Featured
        </h2>
        <ItemGrid items={items} />
      </section>

      <section>
        <h2 className="text-2xl font-semibold text-neutral-200 tracking-wide mb-6">
          Trending
        </h2>
        <ItemGrid items={items} />
      </section>

      <PaginationControls
        currentPage={query.page ?? 1}
        totalPages={totalPages}
        onPageChange={handlePageChange}
      />
    </div>
  );
}
