import { Link, useNavigate } from 'react-router-dom';
import { useCartStore } from '../stores/cartStore';

export default function CartPage() {
  const { cart, error, updateQuantity, remove, clearError } = useCartStore();
  const navigate = useNavigate();

  if (!cart) {
    return <div className="page-loader">Loading cart…</div>;
  }

  if (cart.items.length === 0) {
    return (
      <section className="cart-page">
        <h1>Your cart</h1>
        <p className="empty">Your cart is empty.</p>
        <Link to="/" className="btn btn-primary">
          Browse the catalog
        </Link>
      </section>
    );
  }

  return (
    <section className="cart-page">
      <h1>Your cart</h1>
      {error && (
        <div className="alert" role="alert">
          {error}
          <button type="button" onClick={clearError} aria-label="Dismiss">
            ✕
          </button>
        </div>
      )}
      <ul className="cart-list">
        {cart.items.map((item) => (
          <li key={item.book.id} className="cart-row">
            <div className="cart-book">
              <strong>{item.book.title}</strong>
              <span className="book-author">{item.book.author}</span>
            </div>
            <div className="cart-controls">
              <button
                type="button"
                className="btn btn-secondary qty"
                aria-label={`Decrease quantity of ${item.book.title}`}
                onClick={() =>
                  item.quantity > 1
                    ? void updateQuantity(item.book.id, item.quantity - 1)
                    : void remove(item.book.id)
                }
              >
                −
              </button>
              <span className="qty-value">{item.quantity}</span>
              <button
                type="button"
                className="btn btn-secondary qty"
                aria-label={`Increase quantity of ${item.book.title}`}
                disabled={item.quantity >= item.book.stock}
                onClick={() => void updateQuantity(item.book.id, item.quantity + 1)}
              >
                +
              </button>
            </div>
            <span className="cart-line-total">${item.lineTotal.toFixed(2)}</span>
            <button
              type="button"
              className="btn btn-link"
              onClick={() => void remove(item.book.id)}
            >
              Remove
            </button>
          </li>
        ))}
      </ul>
      <div className="cart-summary">
        <span>
          Total ({cart.totalItems} {cart.totalItems === 1 ? 'item' : 'items'}):{' '}
          <strong>${cart.totalAmount.toFixed(2)}</strong>
        </span>
        <button type="button" className="btn btn-primary" onClick={() => navigate('/checkout')}>
          Proceed to checkout →
        </button>
      </div>
    </section>
  );
}
