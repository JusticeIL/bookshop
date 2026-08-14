import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Book, PageResponse } from '../api/types';
import BookCard from '../components/BookCard';
import { useCartStore } from '../stores/cartStore';

export default function CatalogPage() {
  const [result, setResult] = useState<PageResponse<Book> | null>(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState('id');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const cartError = useCartStore((state) => state.error);
  const clearCartError = useCartStore((state) => state.clearError);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    const [sortField, direction] = sort.split(':');
    const timer = setTimeout(() => {
      api
        .listBooks({ page, size: 12, search, sort: sortField, direction })
        .then((data) => {
          if (!cancelled) {
            setResult(data);
            setError(null);
          }
        })
        .catch(() => {
          if (!cancelled) setError('Could not load the catalog. The server may be waking up - retrying is fine.');
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
    }, search ? 300 : 0); // debounce typing
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [page, search, sort]);

  return (
    <section>
      <div className="catalog-toolbar">
        <input
          type="search"
          placeholder="Search by title or author…"
          value={search}
          onChange={(event) => {
            setSearch(event.target.value);
            setPage(0);
          }}
        />
        <select value={sort} onChange={(event) => setSort(event.target.value)} aria-label="Sort books">
          <option value="id:asc">Newest first</option>
          <option value="title:asc">Title A→Z</option>
          <option value="price:asc">Price: low to high</option>
          <option value="price:desc">Price: high to low</option>
          <option value="pages:desc">Longest reads</option>
        </select>
      </div>

      {cartError && (
        <div className="alert" role="alert">
          {cartError}
          <button type="button" onClick={clearCartError} aria-label="Dismiss">
            ✕
          </button>
        </div>
      )}
      {error && <div className="alert">{error}</div>}
      {loading && !result && <div className="page-loader">Loading books…</div>}

      {result && (
        <>
          <div className="book-grid">
            {result.content.map((book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>
          {result.content.length === 0 && <p className="empty">No books match your search.</p>}
          {result.totalPages > 1 && (
            <div className="pagination">
              <button
                type="button"
                className="btn btn-secondary"
                disabled={page === 0}
                onClick={() => setPage((current) => current - 1)}
              >
                ← Previous
              </button>
              <span>
                Page {result.page + 1} of {result.totalPages}
              </span>
              <button
                type="button"
                className="btn btn-secondary"
                disabled={page + 1 >= result.totalPages}
                onClick={() => setPage((current) => current + 1)}
              >
                Next →
              </button>
            </div>
          )}
        </>
      )}
    </section>
  );
}
