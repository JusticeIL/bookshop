import { create } from 'zustand';
import { api, tokenStorage } from '../api/client';
import type { User } from '../api/types';

interface AuthState {
  user: User | null;
  /** true while the persisted token is being re-validated on app start */
  initializing: boolean;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  login: (email: string, password: string) => Promise<void>;
  /** Called by the /oauth2/redirect route after a social login round-trip. */
  acceptToken: (token: string) => Promise<void>;
  logout: () => void;
  initialize: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  initializing: true,

  initialize: async () => {
    if (!tokenStorage.get()) {
      set({ initializing: false });
      return;
    }
    try {
      const user = await api.me();
      set({ user, initializing: false });
    } catch {
      tokenStorage.clear();
      set({ user: null, initializing: false });
    }
  },

  register: async (email, password, displayName) => {
    const { token, user } = await api.register(email, password, displayName);
    tokenStorage.set(token);
    set({ user });
  },

  login: async (email, password) => {
    const { token, user } = await api.login(email, password);
    tokenStorage.set(token);
    set({ user });
  },

  acceptToken: async (token) => {
    tokenStorage.set(token);
    const user = await api.me();
    set({ user });
  },

  logout: () => {
    tokenStorage.clear();
    set({ user: null });
  },
}));
