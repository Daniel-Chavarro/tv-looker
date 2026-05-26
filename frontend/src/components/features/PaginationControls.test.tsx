import { describe, expect, it, vi } from 'vitest';
import { fireEvent, screen } from '@testing-library/react';
import { PaginationControls } from './PaginationControls';
import { renderWithProviders } from '../../test/render';

describe('PaginationControls', () => {
  it('does not render for a single page', () => {
    renderWithProviders(
      <PaginationControls currentPage={1} totalPages={1} onPageChange={vi.fn()} />,
    );

    expect(screen.queryByRole('navigation', { name: 'Pagination' })).not.toBeInTheDocument();
  });

  it('navigates between pages and disables boundaries', () => {
    const onPageChange = vi.fn();

    renderWithProviders(
      <PaginationControls currentPage={2} totalPages={4} onPageChange={onPageChange} />,
    );

    expect(screen.getByText('Page 2 of 4')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Previous' }));
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));

    expect(onPageChange).toHaveBeenNthCalledWith(1, 1);
    expect(onPageChange).toHaveBeenNthCalledWith(2, 3);
  });

  it('disables previous on first page and next on last page', () => {
    renderWithProviders(
      <PaginationControls currentPage={1} totalPages={3} onPageChange={vi.fn()} />,
    );

    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).not.toBeDisabled();
  });
});
