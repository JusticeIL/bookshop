import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { api } from '../api/client';
import type { Order } from '../api/types';
import ConfettiBurst from '../components/ConfettiBurst';
import { useAuthStore } from '../stores/authStore';
import { useCartStore } from '../stores/cartStore';
import { toast } from '../stores/toastStore';

type Step = 'shipping' | 'payment' | 'confirmation';

/**
 * Multi-step checkout: shipping -> (mock) payment -> confirmation.
 * The recipient name is the signed-in account's full name (validated at
 * registration), so only the address is asked for. The card form is decorative
 * by design - nothing from it is validated, stored, or transmitted; the
 * backend generates a mock payment reference.
 */
export default function CheckoutPage() {
  const { cart, fetch: refreshCart } = useCartStore();
  const user = useAuthStore((state) => state.user);
  const [step, setStep] = useState<Step>('shipping');
  const [shippingAddress, setShippingAddress] = useState('');
  const [cardNumber, setCardNumber] = useState('4242 4242 4242 4242');
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [placing, setPlacing] = useState(false);

  if (!order && (!cart || cart.items.length === 0)) {
    return <Navigate to="/cart" replace />;
  }

  const placeOrder = async () => {
    setPlacing(true);
    setError(null);
    try {
      const placed = await api.checkout({
        shippingAddress,
        mockCardNumber: cardNumber.slice(-4),
      });
      setOrder(placed);
      setStep('confirmation');
      toast.success(`Order #${placed.id} confirmed - thank you!`);
      void refreshCart();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Checkout failed';
      setError(message);
      toast.error(message);
    } finally {
      setPlacing(false);
    }
  };

  return (
    <section className="checkout-page">
      <h1>Checkout</h1>
      <ol className="steps">
        <li className={step === 'shipping' ? 'active' : 'done'}>Shipping</li>
        <li className={step === 'payment' ? 'active' : step === 'confirmation' ? 'done' : ''}>
          Payment
        </li>
        <li className={step === 'confirmation' ? 'active' : ''}>Confirmation</li>
      </ol>

      {error && <div className="alert">{error}</div>}

      {step === 'shipping' && (
        <form
          className="auth-form glass-panel"
          onSubmit={(event) => {
            event.preventDefault();
            setStep('payment');
          }}
        >
          <p className="empty">
            Shipping to <strong>{user?.displayName}</strong> (your account name).
          </p>
          <label>
            Shipping address
            <input
              type="text"
              name="address"
              value={shippingAddress}
              onChange={(event) => setShippingAddress(event.target.value)}
              required
              maxLength={500}
              autoComplete="street-address"
              placeholder="Street, number, city, country"
              autoFocus
              tabIndex={1}
            />
          </label>
          <button type="submit" className="btn btn-primary" tabIndex={2}>
            Continue to payment →
          </button>
        </form>
      )}

      {step === 'payment' && cart && (
        <form
          className="auth-form glass-panel"
          onSubmit={(event) => {
            event.preventDefault();
            void placeOrder();
          }}
        >
          <p className="mock-note">
            💳 Payment is <strong>mocked</strong> - any card number "works" and nothing is charged
            or stored.
          </p>
          <label>
            Card number
            <input
              type="text"
              name="cc-number"
              value={cardNumber}
              onChange={(event) => setCardNumber(event.target.value)}
              inputMode="numeric"
              maxLength={30}
              autoComplete="off"
              tabIndex={1}
            />
          </label>
          <div className="order-review">
            <h2>Order summary</h2>
            <ul>
              {cart.items.map((item) => (
                <li key={item.book.id}>
                  {item.book.title} × {item.quantity} — ${item.lineTotal.toFixed(2)}
                </li>
              ))}
            </ul>
            <p className="checkout-total">
              Total: <strong>${cart.totalAmount.toFixed(2)}</strong>
            </p>
          </div>
          <div className="checkout-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setStep('shipping')}
              tabIndex={3}
            >
              ← Back
            </button>
            <button type="submit" className="btn btn-primary" disabled={placing} tabIndex={2}>
              {placing ? 'Placing order…' : `Pay $${cart.totalAmount.toFixed(2)} (mock)`}
            </button>
          </div>
        </form>
      )}

      {step === 'confirmation' && order && (
        <div className="confirmation glass-panel">
          <ConfettiBurst />
          <h2 className="rainbow-text">🎉 Order confirmed!</h2>
          <p>
            Order <strong>#{order.id}</strong> · payment reference{' '}
            <code>{order.paymentReference}</code>
          </p>
          <p>
            {order.items.length} {order.items.length === 1 ? 'title' : 'titles'} shipping to{' '}
            {order.shippingName}. Total charged (mock): ${order.totalAmount.toFixed(2)}
          </p>
          <p className="empty">Changed your mind? Orders can be cancelled from the history page within 24 hours.</p>
          <div className="checkout-actions">
            <Link to="/orders" className="btn btn-secondary">
              View order history
            </Link>
            <Link to="/" className="btn btn-primary">
              Continue shopping
            </Link>
          </div>
        </div>
      )}
    </section>
  );
}
