import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { dashboardApi } from "../api";
import ComplaintCard from "../components/ComplaintCard";
import EmptyState from "../components/EmptyState";
import ErrorBox from "../components/ErrorBox";
import Spinner from "../components/Spinner";
import { useAuth } from "../context/AuthContext";
import { PRIORITY_LABELS, greeting } from "../utils/format";

const SUBTITLES = {
  ALL: "Campus-wide overview of every complaint.",
  ASSIGNED: "Complaints assigned to you.",
  OWN: "Complaints you have reported.",
};

function StatTile({ label, value, tone, hint }) {
  return (
    <div className={`stat-tile tone-${tone}`}>
      <span className="stat-label">{label}</span>
      <strong className="stat-value">{value}</strong>
      {hint && <span className="stat-hint">{hint}</span>}
    </div>
  );
}

function BarList({ items, labelOf = (x) => x, toneOf }) {
  const max = Math.max(1, ...items.map((i) => i.count));
  if (!items.length) return <p className="muted">No data yet.</p>;
  return (
    <ul className="bar-list">
      {items.map((i) => (
        <li key={i.label}>
          <div className="bar-head">
            <span>{labelOf(i.label)}</span>
            <strong>{i.count}</strong>
          </div>
          <div className="bar-track">
            <div className={`bar-fill ${toneOf ? toneOf(i.label) : ""}`} style={{ width: `${(i.count / max) * 100}%` }} />
          </div>
        </li>
      ))}
    </ul>
  );
}

export default function DashboardPage() {
  const { user, isStudent } = useAuth();
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    dashboardApi
      .get()
      .then((data) => {
        setStats(data);
        setError("");
      })
      .catch((e) => setError(e.message));
  }, []);

  useEffect(load, [load]);

  const s = stats?.byStatus || {};
  const active = (s.OPEN || 0) + (s.IN_PROGRESS || 0);

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>
            {greeting()}, {user.name.split(" ")[0]}
          </h1>
          <p className="muted">{stats ? SUBTITLES[stats.scope] : "Here's what's happening."}</p>
        </div>
        <Link to="/complaints/new" className="btn btn-primary">
          + Report an issue
        </Link>
      </header>

      <ErrorBox message={error} onRetry={load} />
      {!stats && !error && <Spinner />}

      {stats && (
        <>
          <section className="stat-grid">
            <StatTile label="Total complaints" value={stats.total} tone="indigo" />
            <StatTile label="Active" value={active} tone="amber" hint={`${s.OPEN || 0} open · ${s.IN_PROGRESS || 0} in progress`} />
            <StatTile label="Resolved" value={(s.RESOLVED || 0) + (s.CLOSED || 0)} tone="green" hint={`${s.CLOSED || 0} confirmed closed`} />
            <StatTile label="Overdue" value={stats.overdue} tone="red" hint="Past their resolution target" />
            <StatTile
              label="Avg. resolution time"
              value={stats.avgResolutionHours == null ? "—" : formatHours(stats.avgResolutionHours)}
              tone="slate"
              hint="From report to resolved"
            />
          </section>

          <section className="dash-grid">
            <div className="card">
              <div className="card-head">
                <h2>Recent complaints</h2>
                <Link to="/complaints" className="link">
                  View all →
                </Link>
              </div>
              {stats.recent.length ? (
                <div className="stack">
                  {stats.recent.map((c) => (
                    <ComplaintCard key={c.id} complaint={c} compact />
                  ))}
                </div>
              ) : (
                <EmptyState
                  title="Nothing here yet"
                  text={isStudent ? "Spotted something broken on campus? Report it." : "No complaints to show."}
                  action={
                    isStudent && (
                      <Link to="/complaints/new" className="btn btn-primary btn-sm">
                        Report an issue
                      </Link>
                    )
                  }
                />
              )}
            </div>

            <div className="stack">
              <div className="card">
                <h2>By category</h2>
                <BarList items={stats.byCategory} />
              </div>
              <div className="card">
                <h2>By priority</h2>
                <BarList
                  items={stats.byPriority}
                  labelOf={(p) => PRIORITY_LABELS[p]}
                  toneOf={(p) => `fill-${p.toLowerCase()}`}
                />
              </div>
            </div>
          </section>
        </>
      )}
    </div>
  );
}

function formatHours(h) {
  return h >= 48 ? `${(h / 24).toFixed(1)} days` : `${h} hrs`;
}
