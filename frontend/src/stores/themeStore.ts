import { create } from 'zustand';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'bookshop.theme';

function initialTheme(): Theme {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === 'light' || stored === 'dark') return stored;
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function applyTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme;
}

interface ThemeState {
  theme: Theme;
  toggle: () => void;
}

export const useThemeStore = create<ThemeState>((set, get) => ({
  theme: initialTheme(),

  toggle: () => {
    const next: Theme = get().theme === 'light' ? 'dark' : 'light';
    localStorage.setItem(STORAGE_KEY, next);
    applyTheme(next);
    set({ theme: next });
  },
}));

// Apply the persisted/system theme immediately on module load (before first paint).
applyTheme(useThemeStore.getState().theme);
