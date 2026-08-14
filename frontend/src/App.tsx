import { useEffect } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import Footer from './components/Footer';
import NavBar from './components/NavBar';
import ProtectedRoute from './components/ProtectedRoute';
import ScrollToTop from './components/ScrollToTop';
import Toasts from './components/Toasts';
import CartPage from './pages/CartPage';
import CatalogPage from './pages/CatalogPage';
import CheckoutPage from './pages/CheckoutPage';
import CreditsPage from './pages/CreditsPage';
import LoginPage from './pages/LoginPage';
import OrdersPage from './pages/OrdersPage';
import RegisterPage from './pages/RegisterPage';
import { useAuthStore } from './stores/authStore';
import { useCartStore } from './stores/cartStore';
import './stores/themeStore'; // applies the persisted theme before first paint

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
    <div className="app-shell">
      <ScrollToTop />
      <NavBar />
      <main className="container">
        <Routes>
          <Route path="/" element={<CatalogPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/credits" element={<CreditsPage />} />
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
      <Footer />
      <Toasts />
    </div>
  );
}
