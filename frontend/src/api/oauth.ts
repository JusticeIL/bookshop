/**
 * Token handling for the SPA.
 *
 * Sign-in happens in the application's own UI: the login form posts to
 * POST /api/auth/login over TLS and receives an RS256 access token plus a
 * rotating refresh token - the same tokens this backend's OAuth2
 * authorization server issues, signed with the same key. Renewal posts the
 * refresh token to POST /api/auth/refresh, which consumes it and returns a
 * fresh pair.
 */

export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

const ACCESS_TOKEN_KEY = 'bookshop.accessToken';
const REFRESH_TOKEN_KEY = 'bookshop.refreshToken';

export const tokenStorage = {
  getAccess: (): string | null => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefresh: (): string | null => localStorage.getItem(REFRESH_TOKEN_KEY),
  set: (accessToken: string, refreshToken?: string): void => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },
  clear: (): void => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};

interface RefreshedTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

/** In-flight refresh, so parallel 401s trigger exactly one token renewal. */
let refreshInFlight: Promise<boolean> | null = null;

/**
 * Exchanges the rotating refresh token for a fresh pair. Returns false when
 * the refresh token is missing or rejected, meaning the user must sign in
 * again.
 */
export function refreshAccessToken(): Promise<boolean> {
  if (refreshInFlight) return refreshInFlight;

  const refreshToken = tokenStorage.getRefresh();
  if (!refreshToken) return Promise.resolve(false);

  refreshInFlight = fetch(`${API_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
    .then(async (response) => {
      if (!response.ok) {
        tokenStorage.clear();
        return false;
      }
      const tokens = (await response.json()) as RefreshedTokens;
      tokenStorage.set(tokens.accessToken, tokens.refreshToken);
      return true;
    })
    .catch(() => false)
    .finally(() => {
      refreshInFlight = null;
    });

  return refreshInFlight;
}
