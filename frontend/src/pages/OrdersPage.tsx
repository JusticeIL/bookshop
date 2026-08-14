import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { Order } from '../api/types';

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .listOrders()
      .then(setOrders)
      .catch(() => setError('Could not load your orders.'));
  }, []);

  if (error) return <div className="alert">{error}</div>;
  if (!orders) return <div className="page-loader">Loading orders…</div>;

  return (
    <section className="orders-page">
      <h1>Order history</h1>
      {orders.length === 0 ? (
        <>
          <p className="empty">You haven't placed any orders yet.</p>
          <Link to="/" className="btn btn-primary">
            Browse the catalog
          </Link>
        </>
      ) : (
        <ul className="order-list">
          {orders.map((order) => (
            <li key={order.id} className="order-card">
              <header>
                <strong>Order #{order.id}</strong>
                <span className={`status status-${order.status.toLowerCase()}`}>{order.status}</span>
                <time dateTime={order.createdAt}>
                  {new Date(order.createdAt).toLocaleString()}
                </time>
              </header>
              <ul className="order-items">
                {order.items.map((item) => (
                  <li key={`${order.id}-${item.bookId}`}>
                    {item.title} × {item.quantity} — ${(item.unitPrice * item.quantity).toFixed(2)}
                  </li>
                ))}
              </ul>
              <footer>
                <span>
                  Payment ref: <code>{order.paymentReference}</code>
                </span>
                <strong>${order.totalAmount.toFixed(2)}</strong>
              </footer>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
