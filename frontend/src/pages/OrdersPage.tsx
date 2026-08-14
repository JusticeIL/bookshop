import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { Order } from '../api/types';
import { toast } from '../stores/toastStore';

const CANCELLATION_WINDOW_MS = 24 * 60 * 60 * 1000;

function isCancellable(order: Order): boolean {
  return (
    order.status === 'CONFIRMED' &&
    Date.now() - new Date(order.createdAt).getTime() < CANCELLATION_WINDOW_MS
  );
}

function BinIcon() {
  return (
    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M3 6h18" />
      <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
      <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
      <path d="M10 11v6M14 11v6" />
    </svg>
  );
}

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState<number | null>(null);

  useEffect(() => {
    api
      .listOrders()
      .then(setOrders)
      .catch(() => setError('Could not load your orders.'));
  }, []);

  const handleCancel = async (order: Order) => {
    if (!window.confirm(`Cancel order #${order.id}? The books return to stock and the (mock) charge is voided.`)) {
      return;
    }
    setCancelling(order.id);
    try {
      const cancelled = await api.cancelOrder(order.id);
      setOrders((current) =>
        (current ?? []).map((item) => (item.id === cancelled.id ? cancelled : item)),
      );
      toast.success(`Order #${order.id} cancelled - items returned to stock`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Could not cancel the order');
    } finally {
      setCancelling(null);
    }
  };

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
            <li key={order.id} className="order-card glass-panel">
              <header>
                <strong>Order #{order.id}</strong>
                <span className={`status status-${order.status.toLowerCase()}`}>{order.status}</span>
                <time dateTime={order.createdAt}>
                  {new Date(order.createdAt).toLocaleString()}
                </time>
                {isCancellable(order) && (
                  <button
                    type="button"
                    className="btn btn-danger btn-icon"
                    title="Cancel this order (available for 24 hours after purchase)"
                    aria-label={`Cancel order ${order.id}`}
                    disabled={cancelling === order.id}
                    onClick={() => void handleCancel(order)}
                  >
                    <BinIcon />
                    {cancelling === order.id ? 'Cancelling…' : 'Cancel'}
                  </button>
                )}
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
