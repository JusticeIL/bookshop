import { create } from 'zustand';

export type ToastKind = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}

interface ToastState {
  toasts: Toast[];
  push: (kind: ToastKind, message: string) => void;
  dismiss: (id: number) => void;
}

let nextId = 1;
const AUTO_DISMISS_MS = 4200;
const MAX_VISIBLE = 4;

export const useToastStore = create<ToastState>((set) => ({
  toasts: [],

  push: (kind, message) => {
    const id = nextId++;
    set((state) => ({ toasts: [...state.toasts, { id, kind, message }].slice(-MAX_VISIBLE) }));
    setTimeout(
      () => set((state) => ({ toasts: state.toasts.filter((toast) => toast.id !== id) })),
      AUTO_DISMISS_MS,
    );
  },

  dismiss: (id) => set((state) => ({ toasts: state.toasts.filter((toast) => toast.id !== id) })),
}));

/** Imperative helpers usable from stores and API code (outside React). */
export const toast = {
  success: (message: string) => useToastStore.getState().push('success', message),
  error: (message: string) => useToastStore.getState().push('error', message),
  info: (message: string) => useToastStore.getState().push('info', message),
};
