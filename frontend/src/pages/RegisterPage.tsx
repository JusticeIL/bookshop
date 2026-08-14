import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';

/**
 * Full name policy (mirrors the backend @Pattern validation): exactly two
 * words made of letters only (any alphabet), separated by a single space.
 */
const FULL_NAME_PATTERN = /^\p{L}+ \p{L}+$/u;

export default function RegisterPage() {
  const register = useAuthStore((state) => state.register);
  const navigate = useNavigate();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const trimmedName = fullName.trim();
    if (!FULL_NAME_PATTERN.test(trimmedName)) {
      setError(
        'Full name must be exactly two names separated by a single space, letters only (e.g. "Gal Rubinstein").',
      );
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await register(email, password, trimmedName);
      navigate('/', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="auth-page glass-panel">
      <h1>Create account</h1>
      {error && <div className="alert">{error}</div>}
      <form onSubmit={handleSubmit} className="auth-form">
        <label>
          Full name
          <input
            type="text"
            name="name"
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            required
            maxLength={120}
            autoComplete="name"
            placeholder="First Last"
            autoFocus
            tabIndex={1}
          />
          <small className="field-hint">
            Two names, letters only, separated by one space — used as the recipient name at
            checkout.
          </small>
        </label>
        <label>
          Email
          <input
            type="email"
            name="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
            maxLength={254}
            autoComplete="email"
            tabIndex={2}
          />
          <small className="field-hint">
            Your email is your sign-in identity — one account per address.
          </small>
        </label>
        <label>
          Password (min. 8 characters)
          <input
            type="password"
            name="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            minLength={8}
            maxLength={72}
            autoComplete="new-password"
            tabIndex={3}
          />
        </label>
        <button type="submit" className="btn btn-primary" disabled={submitting} tabIndex={4}>
          {submitting ? 'Creating…' : 'Create account'}
        </button>
      </form>
      <p className="auth-switch">
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </section>
  );
}
