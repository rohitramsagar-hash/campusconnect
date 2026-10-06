export default function EmptyState({ title, text, action }) {
  return (
    <div className="empty-state">
      <div className="empty-icon" aria-hidden="true">
        <svg viewBox="0 0 48 48" width="44" height="44">
          <rect x="8" y="10" width="32" height="28" rx="6" fill="none" stroke="currentColor" strokeWidth="2.5" />
          <path d="M8 20h32M17 28h14" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
        </svg>
      </div>
      <h3>{title}</h3>
      {text && <p className="muted">{text}</p>}
      {action}
    </div>
  );
}
