import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';

export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const user = useAuthStore((state) => state.user);
  const initializing = useAuthStore((state) => state.initializing);
  const location = useLocation();

  // Wait for the stored token to be validated before deciding: redirecting
  // during initialisation would bounce a signed-in user to the login page.
  if (initializing) {
    return <div className="page-loader">Loading…</div>;
  }
  if (!user) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  return <>{children}</>;
}
