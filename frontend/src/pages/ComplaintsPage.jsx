import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { categoryApi, complaintApi } from "../api";
import ComplaintCard from "../components/ComplaintCard";
import EmptyState from "../components/EmptyState";
import ErrorBox from "../components/ErrorBox";
import Pagination from "../components/Pagination";
import Spinner from "../components/Spinner";
import { useAuth } from "../context/AuthContext";
import { PRIORITY_LABELS, STATUS_LABELS } from "../utils/format";

const TABS = {
  STUDENT: [
    ["all", "Campus feed"],
    ["mine", "My complaints"],
  ],
  STAFF: [
    ["assigned", "Assigned to me"],
    ["all", "All visible"],
  ],
  ADMIN: [["all", "All complaints"]],
};

const SORTS = [
  ["newest", "Newest first"],
  ["oldest", "Oldest first"],
  ["upvotes", "Most upvoted"],
  ["priority", "Highest priority"],
  ["due", "Due soonest"],
];

export default function ComplaintsPage() {
  const { user } = useAuth();
  const tabs = TABS[user.role];
  const [params, setParams] = useSearchParams();
  const [categories, setCategories] = useState([]);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [search, setSearch] = useState(params.get("q") || "");

  const filters = {
    scope: params.get("scope") || tabs[0][0],
    status: params.get("status") || "",
    categoryId: params.get("categoryId") || "",
    priority: params.get("priority") || "",
    q: params.get("q") || "",
    sort: params.get("sort") || "newest",
    page: Number(params.get("page") || 0),
  };
  const key = params.toString();

  const update = (changes) => {
    const next = { ...filters, page: 0, ...changes };
    const entries = Object.entries(next).filter(([k, v]) => v !== "" && !(k === "page" && v === 0));
    setParams(Object.fromEntries(entries));
  };

  useEffect(() => {
    categoryApi.list().then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    let cancelled = false;
    complaintApi
      .list({ ...filters, size: 10 })
      .then((r) => {
        if (cancelled) return;
        setResult(r);
        setError("");
      })
      .catch((e) => {
        if (!cancelled) setError(e.message);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, reload]);

  // Debounced search box.
  useEffect(() => {
    if (search === filters.q) return;
    const t = setTimeout(() => update({ q: search.trim() }), 400);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  const hasFilters = filters.status || filters.categoryId || filters.priority || filters.q;

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Complaints</h1>
          <p className="muted">Search, filter and follow up on campus issues.</p>
        </div>
        <Link to="/complaints/new" className="btn btn-primary">
          + Report an issue
        </Link>
      </header>

      {tabs.length > 1 && (
        <div className="tabs" role="tablist">
          {tabs.map(([value, label]) => (
            <button
              key={value}
              role="tab"
              aria-selected={filters.scope === value}
              className={`tab${filters.scope === value ? " active" : ""}`}
              onClick={() => update({ scope: value })}
            >
              {label}
            </button>
          ))}
        </div>
      )}

      <div className="filters card">
        <input
          className="search"
          type="search"
          placeholder="Search title, description, location or ticket (CC-2026-00001)…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select value={filters.status} onChange={(e) => update({ status: e.target.value })} aria-label="Status">
          <option value="">All statuses</option>
          {Object.entries(STATUS_LABELS).map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </select>
        <select value={filters.categoryId} onChange={(e) => update({ categoryId: e.target.value })} aria-label="Category">
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <select value={filters.priority} onChange={(e) => update({ priority: e.target.value })} aria-label="Priority">
          <option value="">All priorities</option>
          {Object.entries(PRIORITY_LABELS).map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </select>
        <select value={filters.sort} onChange={(e) => update({ sort: e.target.value })} aria-label="Sort">
          {SORTS.map(([v, l]) => (
            <option key={v} value={v}>
              {l}
            </option>
          ))}
        </select>
        {hasFilters && (
          <button
            className="btn btn-ghost btn-sm"
            onClick={() => {
              setSearch("");
              setParams(filters.scope === tabs[0][0] ? {} : { scope: filters.scope });
            }}
          >
            Clear filters
          </button>
        )}
      </div>

      <ErrorBox message={error} onRetry={() => setReload((n) => n + 1)} />
      {!result && !error && <Spinner />}

      {result &&
        (result.content.length ? (
          <>
            <div className="stack">
              {result.content.map((c) => (
                <ComplaintCard key={c.id} complaint={c} />
              ))}
            </div>
            <Pagination {...result} onChange={(page) => update({ page })} />
          </>
        ) : (
          <EmptyState
            title={hasFilters ? "No complaints match these filters" : "No complaints yet"}
            text={hasFilters ? "Try removing a filter or searching for something else." : "When issues are reported they'll show up here."}
          />
        ))}
    </div>
  );
}
