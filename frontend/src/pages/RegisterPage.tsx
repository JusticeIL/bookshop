import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';

/**
 * Full name policy (mirrors the backend @Pattern validation): two or more
 * words separated by single spaces, so "Elad Ben David" is as valid as
 * "Gal Rubinstein". Words are letters in any alphabet, with the hyphen as the
 * only permitted non-letter and only between letters - "Jean-Pierre" passes,
 * while "%", digits, underscores and stray hyphens do not.
 */
const FULL_NAME_PATTERN = /^\p{L}+(?:-\p{L}+)*(?: \p{L}+(?:-\p{L}+)*)+$/u;

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
        'Full name must be at least two names separated by a space, using letters and hyphens only (e.g. "Elad Ben David" or "Jean-Pierre Dupont").',
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
            placeholder="Full Name"
            autoFocus
            tabIndex={1}
          />
          <small className="field-hint">
            At least two names separated by a space; letters and hyphens only — used as the
            recipient name at checkout.
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
