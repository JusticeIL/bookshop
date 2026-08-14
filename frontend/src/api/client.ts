import { API_URL, refreshAccessToken, tokenStorage } from './oauth';
import { toast } from '../stores/toastStore';
import type {
  Book,
  Cart,
  CheckoutRequest,
  Order,
  PageResponse,
  TokenResponse,
  User,
} from './types';

export { API_URL, tokenStorage };

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

/** Throttles the red "server unreachable" toast so bursts don't spam the user. */
let lastConnectionToastAt = 0;

function notifyConnectionProblem(): void {
  const now = Date.now();
  if (now - lastConnectionToastAt > 6000) {
    lastConnectionToastAt = now;
    toast.error('Cannot reach the server - it may be starting up. Please try again in a moment.');
  }
}

async function send(path: string, options: RequestInit): Promise<Response> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
  };
  const token = tokenStorage.getAccess();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return fetch(`${API_URL}${path}`, { ...options, headers });
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await send(path, options);
    // Access tokens are short-lived (30 min); transparently rotate the refresh
    // token and replay the call once before surfacing a 401 to the user.
    if (response.status === 401 && (await refreshAccessToken())) {
      response = await send(path, options);
    }
  } catch {
    // Network-level failure: backend down, DB gone, CORS, DNS…
    notifyConnectionProblem();
    throw new ApiError(0, 'Server unreachable');
  }

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

  // Accounts
  register: (email: string, password: string, fullName: string) =>
    request<TokenResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password, fullName }),
    }),
  login: (email: string, password: string) =>
    request<TokenResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),
  me: () => request<User>('/api/auth/me'),

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
  cancelOrder: (orderId: number) =>
    request<Order>(`/api/orders/${orderId}`, { method: 'DELETE' }),
};
