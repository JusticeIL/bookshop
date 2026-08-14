import { useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { api } from '../api/client';
import type { Order } from '../api/types';
import { useCartStore } from '../stores/cartStore';

type Step = 'shipping' | 'payment' | 'confirmation';

/**
 * Multi-step checkout: shipping -> (mock) payment -> confirmation.
 * The card form is decorative by design - nothing from it is validated,
 * stored, or transmitted; the backend generates a mock payment reference.
 */
export default function CheckoutPage() {
  const { cart, fetch: refreshCart } = useCartStore();
  const [step, setStep] = useState<Step>('shipping');
  const [shippingName, setShippingName] = useState('');
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
        shippingName,
        shippingAddress,
        mockCardNumber: cardNumber.slice(-4),
      });
      setOrder(placed);
      setStep('confirmation');
      void refreshCart();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Checkout failed');
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
          className="auth-form"
          onSubmit={(event) => {
            event.preventDefault();
            setStep('payment');
          }}
        >
          <label>
            Full name
            <input
              type="text"
              value={shippingName}
              onChange={(event) => setShippingName(event.target.value)}
              required
              maxLength={120}
            />
          </label>
          <label>
            Shipping address
            <textarea
              value={shippingAddress}
              onChange={(event) => setShippingAddress(event.target.value)}
              required
              maxLength={500}
              rows={3}
            />
          </label>
          <button type="submit" className="btn btn-primary">
            Continue to payment →
          </button>
        </form>
      )}

      {step === 'payment' && cart && (
        <form
          className="auth-form"
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
              value={cardNumber}
              onChange={(event) => setCardNumber(event.target.value)}
              inputMode="numeric"
              maxLength={30}
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
            >
              ← Back
            </button>
            <button type="submit" className="btn btn-primary" disabled={placing}>
              {placing ? 'Placing order…' : `Pay $${cart.totalAmount.toFixed(2)} (mock)`}
            </button>
          </div>
        </form>
      )}

      {step === 'confirmation' && order && (
        <div className="confirmation">
          <h2>🎉 Order confirmed!</h2>
          <p>
            Order <strong>#{order.id}</strong> · payment reference{' '}
            <code>{order.paymentReference}</code>
          </p>
          <p>
            {order.items.length} {order.items.length === 1 ? 'title' : 'titles'} shipping to{' '}
            {order.shippingName}. Total charged (mock): ${order.totalAmount.toFixed(2)}
          </p>
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
