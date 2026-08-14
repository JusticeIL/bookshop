import { useEffect } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import NavBar from './components/NavBar';
import ProtectedRoute from './components/ProtectedRoute';
import CartPage from './pages/CartPage';
import CatalogPage from './pages/CatalogPage';
import CheckoutPage from './pages/CheckoutPage';
import LoginPage from './pages/LoginPage';
import OAuthRedirectPage from './pages/OAuthRedirectPage';
import OrdersPage from './pages/OrdersPage';
import RegisterPage from './pages/RegisterPage';
import { useAuthStore } from './stores/authStore';
import { useCartStore } from './stores/cartStore';

export default function App() {
  const { user, initializing, initialize } = useAuthStore();
  const fetchCart = useCartStore((state) => state.fetch);
  const clearCart = useCartStore((state) => state.clearLocal);

  useEffect(() => {
    void initialize();
  }, [initialize]);

  useEffect(() => {
    if (user) {
      void fetchCart();
    } else {
      clearCart();
    }
  }, [user, fetchCart, clearCart]);

  if (initializing) {
    return <div className="page-loader">Loading…</div>;
  }

  return (
    <>
      <NavBar />
      <main className="container">
        <Routes>
          <Route path="/" element={<CatalogPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/oauth2/redirect" element={<OAuthRedirectPage />} />
          <Route
            path="/cart"
            element={
              <ProtectedRoute>
                <CartPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/checkout"
            element={
              <ProtectedRoute>
                <CheckoutPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders"
            element={
              <ProtectedRoute>
                <OrdersPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}
