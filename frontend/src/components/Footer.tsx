import { Link } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';

export default function Footer() {
  const user = useAuthStore((state) => state.user);

  return (
    <footer className="footer glass">
      <div className="footer-grid">
        <div className="footer-brand">
          <Link to="/" className="brand">
            📚 The Online Bookshop
          </Link>
          <p className="footer-tagline">
            A full-stack home-assignment demo — React&nbsp;+&nbsp;TypeScript, Java&nbsp;21 /
            Spring&nbsp;Boot&nbsp;3, PostgreSQL.
          </p>
        </div>

        <nav className="footer-col" aria-label="Site map">
          <h3>Site map</h3>
          <Link to="/">Home / Catalog</Link>
          {user ? (
            <>
              <Link to="/cart">Cart</Link>
              <Link to="/orders">Order history</Link>
            </>
          ) : (
            <>
              <Link to="/login">Sign in</Link>
              <Link to="/register">Create account</Link>
            </>
          )}
          <Link to="/credits">Credits &amp; sources</Link>
        </nav>

        <div className="footer-col">
          <h3>About</h3>
          <p>
            Payment and shipping are <strong>mocked</strong> — this is not a real store. See the
            repository's README and DISCLAIMER for details.
          </p>
        </div>
      </div>
      <div className="footer-bottom">
        <span>© {new Date().getFullYear()} The Online Bookshop — home assignment demo. Not for commercial use.</span>
        <span>
          Covers courtesy of <Link to="/credits">OpenLibrary</Link>
        </span>
      </div>
    </footer>
  );
}
