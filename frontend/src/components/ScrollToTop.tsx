import { useLayoutEffect } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * Resets the scroll position on every route change.
 *
 * <p>Browsers preserve the scroll offset across in-app navigations, which on
 * mobile means opening Orders from a scrolled catalog drops you into the
 * middle of the new page. useLayoutEffect runs before paint, so the reset is
 * never visible as a jump.
 */
export default function ScrollToTop() {
  const { pathname } = useLocation();

  useLayoutEffect(() => {
    window.scrollTo(0, 0);
    // Some mobile browsers scroll the documentElement/body rather than window.
    document.documentElement.scrollTop = 0;
    document.body.scrollTop = 0;
  }, [pathname]);

  return null;
}
