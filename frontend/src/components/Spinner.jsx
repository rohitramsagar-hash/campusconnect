export default function Spinner({ label = "Loading…", full = false }) {
  return (
    <div className={full ? "spinner-wrap spinner-full" : "spinner-wrap"}>
      <span className="spinner" aria-hidden="true" />
      <span className="muted">{label}</span>
    </div>
  );
}
