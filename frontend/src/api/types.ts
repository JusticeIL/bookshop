/** API contracts - mirrors the backend DTO records. */

export interface Book {
  id: number;
  title: string;
  author: string;
  description: string;
  pages: number;
  imageUrl: string | null;
  price: number;
  stock: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface User {
  id: number;
  email: string;
  displayName: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}

export interface CartItem {
  book: Book;
  quantity: number;
  lineTotal: number;
}

export interface Cart {
  items: CartItem[];
  totalItems: number;
  totalAmount: number;
}

export interface OrderItem {
  bookId: number;
  title: string;
  unitPrice: number;
  quantity: number;
}

export interface Order {
  id: number;
  status: string;
  totalAmount: number;
  shippingName: string;
  shippingAddress: string;
  paymentReference: string;
  createdAt: string;
  items: OrderItem[];
}

export interface CheckoutRequest {
  shippingAddress: string;
  mockCardNumber?: string;
}
