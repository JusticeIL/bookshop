import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { useCartStore } from '../stores/cartStore';

export default function NavBar() {
  const { user, logout } = useAuthStore();
  const totalItems = useCartStore((state) => state.cart?.totalItems ?? 0);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header className="navbar">
      <Link to="/" className="brand">
        📚 The Online Bookshop
      </Link>
      <nav>
        <NavLink to="/">Catalog</NavLink>
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
        {user ? (
          <>
            <span className="nav-user" title={user.email}>
              {user.displayName}
              {user.authProvider !== 'LOCAL' && (
                <small> (via {user.authProvider.toLowerCase()})</small>
              )}
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
