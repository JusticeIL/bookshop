import { create } from 'zustand';
import { api } from '../api/client';
import { tokenStorage } from '../api/oauth';
import type { User } from '../api/types';
import { toast } from './toastStore';

interface AuthState {
  user: User | null;
  /** true while a persisted access token is being re-validated on app start */
  initializing: boolean;
  initialize: () => Promise<void>;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string) => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  initializing: true,

  initialize: async () => {
    if (!tokenStorage.getAccess() && !tokenStorage.getRefresh()) {
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

  login: async (email, password) => {
    const { accessToken, refreshToken, user } = await api.login(email, password);
    tokenStorage.set(accessToken, refreshToken);
    set({ user });
    toast.success(`Welcome back, ${user.displayName}!`);
  },

  register: async (email, password, fullName) => {
    const { accessToken, refreshToken, user } = await api.register(email, password, fullName);
    tokenStorage.set(accessToken, refreshToken);
    set({ user });
    toast.success(`Welcome to the Bookshop, ${user.displayName}!`);
  },

  logout: () => {
    tokenStorage.clear();
    set({ user: null });
    toast.info('Signed out. See you soon!');
  },
}));
