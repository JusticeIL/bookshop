import { useToastStore } from '../stores/toastStore';

const ICONS: Record<string, string> = { success: '✓', error: '✕', info: 'ℹ' };

/** Global toast stack - green for success, red for errors, glass style. */
export default function Toasts() {
  const { toasts, dismiss } = useToastStore();

  if (toasts.length === 0) return null;

  return (
    <div className="toast-stack" role="status" aria-live="polite">
      {toasts.map((item) => (
        <div key={item.id} className={`toast toast-${item.kind}`}>
          <span className="toast-icon" aria-hidden="true">
            {ICONS[item.kind]}
          </span>
          <span className="toast-message">{item.message}</span>
          <button
            type="button"
            className="toast-close"
            onClick={() => dismiss(item.id)}
            aria-label="Dismiss notification"
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  );
}
