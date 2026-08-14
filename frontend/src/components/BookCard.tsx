import { useNavigate } from 'react-router-dom';
import type { Book } from '../api/types';
import { useAuthStore } from '../stores/authStore';
import { useCartStore } from '../stores/cartStore';

/** Deterministic fallback cover for books without an image URL. */
function FallbackCover({ title }: { title: string }) {
  const hues = [210, 260, 20, 160, 340, 45];
  const hue = hues[title.length % hues.length];
  return (
    <div className="book-cover fallback" style={{ background: `hsl(${hue}, 45%, 38%)` }}>
      <span>{title}</span>
    </div>
  );
}

export default function BookCard({ book }: { book: Book }) {
  const user = useAuthStore((state) => state.user);
  const add = useCartStore((state) => state.add);
  const inCart = useCartStore(
    (state) => state.cart?.items.find((item) => item.book.id === book.id)?.quantity ?? 0,
  );
  const navigate = useNavigate();

  const soldOut = book.stock === 0;
  const maxedOut = !soldOut && inCart >= book.stock;

  const handleAdd = () => {
    if (!user) {
      navigate('/login', { state: { from: '/' } });
      return;
    }
    void add(book);
  };

  return (
    <article className={`book-card${soldOut ? ' sold-out' : ''}`}>
      <div className="book-cover-wrap">
        {book.imageUrl ? (
          <img
            className="book-cover"
            src={book.imageUrl}
            alt={`Cover of ${book.title}`}
            loading="lazy"
            onError={(event) => {
              // Broken remote image -> hide it; CSS shows the card background instead.
              event.currentTarget.style.visibility = 'hidden';
            }}
          />
        ) : (
          <FallbackCover title={book.title} />
        )}
        {soldOut && <span className="oos-badge">OUT OF STOCK</span>}
      </div>
      <div className="book-info">
        <h3 title={book.title}>{book.title}</h3>
        <p className="book-description" title={book.description}>
          {book.description}
        </p>
        <p className="book-author">{book.author}</p>
        <p className="book-meta">
          {book.pages} pages · {soldOut ? 'Out of stock' : `${book.stock} in stock`}
        </p>
        <div className="book-footer">
          <span className="book-price">${book.price.toFixed(2)}</span>
          <button
            type="button"
            className="btn btn-primary"
            disabled={soldOut || maxedOut}
            onClick={handleAdd}
          >
            {soldOut ? 'OUT OF STOCK' : inCart > 0 ? `In cart (${inCart})` : 'Add to cart'}
          </button>
        </div>
      </div>
    </article>
  );
}
