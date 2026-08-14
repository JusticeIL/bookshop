import type {
  AuthResponse,
  Book,
  Cart,
  CheckoutRequest,
  Order,
  PageResponse,
  User,
} from './types';

export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

const TOKEN_KEY = 'bookshop.token';

export const tokenStorage = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (token: string): void => localStorage.setItem(TOKEN_KEY, token),
  clear: (): void => localStorage.removeItem(TOKEN_KEY),
};

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  };
  const token = tokenStorage.get();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_URL}${path}`, { ...options, headers });
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) message = body.message;
    } catch {
      // non-JSON error body - keep the generic message
    }
    throw new ApiError(response.status, message);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  // Catalog
  listBooks: (params: { page?: number; size?: number; search?: string; sort?: string; direction?: string }) => {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.set('page', String(params.page));
    if (params.size !== undefined) query.set('size', String(params.size));
    if (params.search) query.set('search', params.search);
    if (params.sort) query.set('sort', params.sort);
    if (params.direction) query.set('direction', params.direction);
    return request<PageResponse<Book>>(`/api/books?${query}`);
  },

  // Auth
  register: (email: string, password: string, displayName: string) =>
    request<AuthResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password, displayName }),
    }),
  login: (email: string, password: string) =>
    request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),
  me: () => request<User>('/api/auth/me'),
  authProviders: () => request<{ providers: string[] }>('/api/auth/providers'),

  // Cart
  getCart: () => request<Cart>('/api/cart'),
  addToCart: (bookId: number, quantity = 1) =>
    request<Cart>('/api/cart/items', {
      method: 'POST',
      body: JSON.stringify({ bookId, quantity }),
    }),
  updateCartItem: (bookId: number, quantity: number) =>
    request<Cart>(`/api/cart/items/${bookId}`, {
      method: 'PUT',
      body: JSON.stringify({ quantity }),
    }),
  removeFromCart: (bookId: number) =>
    request<Cart>(`/api/cart/items/${bookId}`, { method: 'DELETE' }),

  // Orders
  checkout: (payload: CheckoutRequest) =>
    request<Order>('/api/orders', { method: 'POST', body: JSON.stringify(payload) }),
  listOrders: () => request<Order[]>('/api/orders'),
};

/** Entry point for the backend-driven OAuth2 handshake. */
export function oauthLoginUrl(provider: 'google' | 'facebook'): string {
  return `${API_URL}/oauth2/authorization/${provider}`;
}
