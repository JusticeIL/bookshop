import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { useCartStore } from '../stores/cartStore';
import { useThemeStore } from '../stores/themeStore';

export default function NavBar() {
  const { user, logout, initializing } = useAuthStore();
  const totalItems = useCartStore((state) => state.cart?.totalItems ?? 0);
  const { theme, toggle } = useThemeStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header className="navbar glass">
      <Link to="/" className="brand">
        📚 The Online Bookshop
      </Link>
      <nav>
        <NavLink to="/" end>
          🏠 Home
        </NavLink>
        {user && (
          <>
            <NavLink to="/cart">
              Cart{totalItems > 0 && <span className="badge">{totalItems}</span>}
            </NavLink>
            <NavLink to="/orders">Orders</NavLink>
          </>
        )}
      </nav>
      <div className="nav-auth">
        <button
          type="button"
          role="switch"
          aria-checked={theme === 'dark'}
          className="theme-switch"
          onClick={toggle}
          title={theme === 'light' ? 'Switch to dark mode' : 'Switch to light mode'}
          aria-label={theme === 'light' ? 'Switch to dark mode' : 'Switch to light mode'}
        >
          <span className="theme-switch-knob" aria-hidden="true">
            {theme === 'dark' ? '🌙' : '☀️'}
          </span>
        </button>
        {/* Hold the auth area blank until the stored token is validated,
            so a signed-in user never sees "Sign in" flash first. */}
        {initializing ? null : user ? (
          <>
            <span className="nav-user" title={user.email}>
              {user.displayName}
            </span>
            <button type="button" className="btn btn-secondary" onClick={handleLogout}>
              Sign out
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="btn btn-secondary">
              Sign in
            </Link>
            <Link to="/register" className="btn btn-primary">
              Create account
            </Link>
          </>
        )}
      </div>
    </header>
  );
}
