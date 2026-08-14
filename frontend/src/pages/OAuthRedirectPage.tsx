import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';

/**
 * Landing route for the backend-driven OAuth2 flow. The backend redirects here
 * with the JWT in the URL fragment (#token=...), which never reaches any
 * server. We store it, load the profile, and clean the URL.
 */
export default function OAuthRedirectPage() {
  const acceptToken = useAuthStore((state) => state.acceptToken);
  const navigate = useNavigate();
  const [failed, setFailed] = useState(false);
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) return;
    handled.current = true;

    const params = new URLSearchParams(window.location.hash.replace(/^#/, ''));
    const token = params.get('token');
    // Remove the token from the address bar/history immediately.
    window.history.replaceState(null, '', window.location.pathname);

    if (!token) {
      setFailed(true);
      return;
    }
    acceptToken(token)
      .then(() => navigate('/', { replace: true }))
      .catch(() => setFailed(true));
  }, [acceptToken, navigate]);

  if (failed) {
    return (
      <section className="auth-page">
        <h1>Sign-in failed</h1>
        <p>We couldn't complete the social sign-in.</p>
        <Link to="/login" className="btn btn-primary">
          Back to sign in
        </Link>
      </section>
    );
  }
  return <div className="page-loader">Completing sign-in…</div>;
}
