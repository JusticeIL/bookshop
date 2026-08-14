import { create } from 'zustand';
import { api } from '../api/client';
import type { Book, Cart } from '../api/types';
import { toast } from './toastStore';

/**
 * Cart store with optimistic updates: the UI reflects add/remove/quantity
 * changes immediately, and rolls back to the previous server state if the
 * API call fails (e.g. out of stock). User feedback goes through toasts.
 */
interface CartState {
  cart: Cart | null;
  fetch: () => Promise<void>;
  add: (book: Book, quantity?: number) => Promise<void>;
  updateQuantity: (bookId: number, quantity: number) => Promise<void>;
  remove: (bookId: number) => Promise<void>;
  clearLocal: () => void;
}

const emptyCart: Cart = { items: [], totalItems: 0, totalAmount: 0 };

function recalculate(cart: Cart): Cart {
  const items = cart.items.map((item) => ({
    ...item,
    lineTotal: Number((item.book.price * item.quantity).toFixed(2)),
  }));
  return {
    items,
    totalItems: items.reduce((sum, item) => sum + item.quantity, 0),
    totalAmount: Number(items.reduce((sum, item) => sum + item.lineTotal, 0).toFixed(2)),
  };
}

function errorMessage(err: unknown, fallback: string): string {
  return err instanceof Error && err.message !== 'Server unreachable' ? err.message : fallback;
}

export const useCartStore = create<CartState>((set, get) => ({
  cart: null,

  fetch: async () => {
    try {
      set({ cart: await api.getCart() });
    } catch {
      set({ cart: emptyCart });
    }
  },

  add: async (book, quantity = 1) => {
    const previous = get().cart ?? emptyCart;
    const existing = previous.items.find((item) => item.book.id === book.id);
    const optimistic = recalculate({
      ...previous,
      items: existing
        ? previous.items.map((item) =>
            item.book.id === book.id ? { ...item, quantity: item.quantity + quantity } : item,
          )
        : [...previous.items, { book, quantity, lineTotal: 0 }],
    });
    set({ cart: optimistic });
    try {
      set({ cart: await api.addToCart(book.id, quantity) });
      toast.success(`"${book.title}" added to cart`);
    } catch (err) {
      set({ cart: previous });
      toast.error(errorMessage(err, `Could not add "${book.title}" to the cart`));
    }
  },

  updateQuantity: async (bookId, quantity) => {
    const previous = get().cart ?? emptyCart;
    const optimistic = recalculate({
      ...previous,
      items: previous.items.map((item) =>
        item.book.id === bookId ? { ...item, quantity } : item,
      ),
    });
    set({ cart: optimistic });
    try {
      set({ cart: await api.updateCartItem(bookId, quantity) });
    } catch (err) {
      set({ cart: previous });
      toast.error(errorMessage(err, 'Could not update the cart'));
    }
  },

  remove: async (bookId) => {
    const previous = get().cart ?? emptyCart;
    const removed = previous.items.find((item) => item.book.id === bookId);
    const optimistic = recalculate({
      ...previous,
      items: previous.items.filter((item) => item.book.id !== bookId),
    });
    set({ cart: optimistic });
    try {
      set({ cart: await api.removeFromCart(bookId) });
      if (removed) toast.info(`"${removed.book.title}" removed from cart`);
    } catch (err) {
      set({ cart: previous });
      toast.error(errorMessage(err, 'Could not remove the item'));
    }
  },

  clearLocal: () => set({ cart: emptyCart }),
}));
