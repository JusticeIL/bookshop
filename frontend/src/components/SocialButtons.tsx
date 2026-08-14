import { useEffect, useState } from 'react';
import { api, oauthLoginUrl } from '../api/client';

/**
 * Renders Google/Facebook buttons only for providers the backend actually has
 * credentials for (discovered via GET /api/auth/providers), so the UI never
 * shows a button that would dead-end.
 */
export default function SocialButtons() {
  const [providers, setProviders] = useState<string[]>([]);

  useEffect(() => {
    api
      .authProviders()
      .then((response) => setProviders(response.providers))
      .catch(() => setProviders([]));
  }, []);

  if (providers.length === 0) {
    return null;
  }

  return (
    <div className="social-buttons">
      <div className="divider">
        <span>or continue with</span>
      </div>
      {providers.includes('google') && (
        <a className="btn btn-social google" href={oauthLoginUrl('google')}>
          <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
            <path fill="#4285F4" d="M23.5 12.3c0-.9-.1-1.5-.3-2.2H12v4.1h6.5c-.1 1.1-.8 2.7-2.4 3.8l3.7 2.9c2.3-2.1 3.7-5.1 3.7-8.6z" />
            <path fill="#34A853" d="M12 24c3.2 0 6-1.1 7.9-2.9l-3.7-2.9c-1 .7-2.4 1.2-4.2 1.2-3.2 0-6-2.1-7-5.1L1.2 17C3.1 21.1 7.2 24 12 24z" />
            <path fill="#FBBC05" d="M5 14.3c-.2-.7-.4-1.5-.4-2.3s.2-1.6.4-2.3L1.2 6.7C.4 8.3 0 10.1 0 12s.4 3.7 1.2 5.3L5 14.3z" />
            <path fill="#EA4335" d="M12 4.7c2.3 0 3.8 1 4.7 1.8l3.4-3.3C18 1.2 15.2 0 12 0 7.2 0 3.1 2.9 1.2 6.7L5 9.7c1-3 3.8-5 7-5z" />
          </svg>
          Continue with Google
        </a>
      )}
      {providers.includes('facebook') && (
        <a className="btn btn-social facebook" href={oauthLoginUrl('facebook')}>
          <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
            <path
              fill="currentColor"
              d="M24 12.07C24 5.4 18.63 0 12 0S0 5.4 0 12.07c0 6.02 4.39 11.02 10.13 11.93v-8.44H7.08v-3.49h3.05V9.41c0-3.02 1.79-4.7 4.53-4.7 1.31 0 2.68.24 2.68.24v2.97h-1.51c-1.49 0-1.96.93-1.96 1.89v2.26h3.33l-.53 3.49h-2.8V24C19.61 23.09 24 18.09 24 12.07z"
            />
          </svg>
          Continue with Facebook
        </a>
      )}
    </div>
  );
}
