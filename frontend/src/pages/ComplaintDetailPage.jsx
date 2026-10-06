import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { complaintApi, userApi } from "../api";
import { OverdueBadge, PriorityBadge, RoleBadge, StatusBadge } from "../components/Badges";
import ErrorBox from "../components/ErrorBox";
import Spinner from "../components/Spinner";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { ACTION_LABELS, STATUS_LABELS, formatDate, initials, timeAgo } from "../utils/format";

const NOTE_REQUIRED = (from, to) => to === "REJECTED" || (from === "RESOLVED" && to === "IN_PROGRESS");

function historyText(h) {
  if (!h.fromStatus) return "submitted the complaint";
  if (h.fromStatus === h.toStatus)
    return h.note?.startsWith("Assigned to ") ? `assigned it to ${h.note.slice("Assigned to ".length)}` : "updated the complaint";
  if (h.fromStatus === "RESOLVED" && h.toStatus === "IN_PROGRESS") return "reopened the complaint";
  return `changed status to ${STATUS_LABELS[h.toStatus]}`;
}

export default function ComplaintDetailPage() {
  const { id } = useParams();
  const { isAdmin } = useAuth();
  const toast = useToast();
  const [c, setC] = useState(null);
  const [error, setError] = useState("");
  const [staff, setStaff] = useState([]);
  const [pending, setPending] = useState(null); // status being chosen
  const [note, setNote] = useState("");
  const [staffId, setStaffId] = useState("");
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    complaintApi
      .get(id)
      .then((data) => {
        setC(data);
        setError("");
      })
      .catch((e) => setError(e.message));
  }, [id]);

  useEffect(load, [load]);

  useEffect(() => {
    if (isAdmin) userApi.list("STAFF").then((list) => setStaff(list.filter((u) => u.active))).catch(() => {});
  }, [isAdmin]);

  const run = async (action, success) => {
    setBusy(true);
    try {
      const result = await action();
      toast(success);
      return result;
    } catch (e) {
      toast(e.message, "error");
      return null;
    } finally {
      setBusy(false);
    }
  };

  const submitStatus = async () => {
    if (NOTE_REQUIRED(c.status, pending) && !note.trim()) {
      toast("Please add a short note explaining why.", "error");
      return;
    }
    const updated = await run(
      () => complaintApi.setStatus(c.id, pending, note.trim() || null),
      `Status changed to ${STATUS_LABELS[pending]}.`,
    );
    if (updated) {
      setC(updated);
      setPending(null);
      setNote("");
    }
  };

  const submitAssign = async () => {
    if (!staffId) return toast("Choose a staff member first.", "error");
    const updated = await run(() => complaintApi.assign(c.id, Number(staffId)), "Complaint assigned.");
    if (updated) {
      setC(updated);
      setStaffId("");
    }
  };

  const submitComment = async (e) => {
    e.preventDefault();
    if (!comment.trim()) return;
    const added = await run(() => complaintApi.comment(c.id, comment.trim()), "Comment added.");
    if (added) {
      setC({ ...c, comments: [...c.comments, added] });
      setComment("");
    }
  };

  const toggleUpvote = async () => {
    const res = await run(
      () => (c.upvotedByMe ? complaintApi.removeUpvote(c.id) : complaintApi.upvote(c.id)),
      c.upvotedByMe ? "Upvote removed." : "Upvoted — this tells admins more people are affected.",
    );
    if (res) setC({ ...c, upvoteCount: res.upvoteCount, upvotedByMe: res.upvotedByMe });
  };

  if (error)
    return (
      <div className="page">
        <Link to="/complaints" className="link back-link">
          ← Back to complaints
        </Link>
        <ErrorBox message={error} onRetry={load} />
      </div>
    );
  if (!c) return <Spinner />;

  const p = c.permissions;

  return (
    <div className="page">
      <Link to="/complaints" className="link back-link">
        ← Back to complaints
      </Link>

      <header className="detail-header card">
        <div className="detail-title">
          <span className="ticket">{c.ticketNo}</span>
          <h1>{c.title}</h1>
          <div className="badge-row">
            <StatusBadge status={c.status} />
            <PriorityBadge priority={c.priority} />
            {c.overdue && <OverdueBadge />}
            {!c.isPublic && <span className="badge private">Private</span>}
          </div>
        </div>
        <button
          className={`upvote-btn${c.upvotedByMe ? " active" : ""}`}
          onClick={toggleUpvote}
          disabled={!p.canUpvote || busy}
          title={p.canUpvote ? "Me too — I'm affected by this" : "You can't upvote your own complaint"}
        >
          <span aria-hidden="true">▲</span>
          <strong>{c.upvoteCount}</strong>
          <small>{c.upvotedByMe ? "Upvoted" : "Me too"}</small>
        </button>
      </header>

      <div className="detail-grid">
        <div className="stack">
          <section className="card">
            <h2>Description</h2>
            <p className="description">{c.description}</p>
            <dl className="meta-grid">
              <div>
                <dt>Category</dt>
                <dd>{c.category}</dd>
              </div>
              <div>
                <dt>Location</dt>
                <dd>{c.location || "—"}</dd>
              </div>
              <div>
                <dt>Reported by</dt>
                <dd>{c.reporterName}</dd>
              </div>
              <div>
                <dt>Assigned to</dt>
                <dd>{c.assignee?.name || "Not assigned yet"}</dd>
              </div>
              <div>
                <dt>Reported</dt>
                <dd>{formatDate(c.createdAt)}</dd>
              </div>
              <div>
                <dt>{c.resolvedAt ? "Resolved" : "Resolution target"}</dt>
                <dd className={c.overdue ? "text-danger" : ""}>
                  {c.resolvedAt ? formatDate(c.resolvedAt) : `${formatDate(c.dueAt)} (${timeAgo(c.dueAt)})`}
                </dd>
              </div>
            </dl>
          </section>

          <section className="card">
            <h2>Discussion ({c.comments.length})</h2>
            {c.comments.length === 0 && <p className="muted">No comments yet.</p>}
            <ul className="comments">
              {c.comments.map((m) => (
                <li key={m.id} className="comment">
                  <div className="avatar avatar-sm">{initials(m.authorName)}</div>
                  <div>
                    <div className="comment-head">
                      <strong>{m.authorName}</strong>
                      <RoleBadge role={m.authorRole} />
                      <span className="muted" title={formatDate(m.createdAt)}>
                        {timeAgo(m.createdAt)}
                      </span>
                    </div>
                    <p>{m.message}</p>
                  </div>
                </li>
              ))}
            </ul>
            {p.canComment ? (
              <form className="comment-form" onSubmit={submitComment}>
                <textarea
                  rows={3}
                  maxLength={1000}
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Add an update or ask a question…"
                />
                <button className="btn btn-primary btn-sm" disabled={busy || !comment.trim()}>
                  Post comment
                </button>
              </form>
            ) : (
              <p className="muted small">Only the reporter, the assigned staff member and admins can comment.</p>
            )}
          </section>
        </div>

        <aside className="stack">
          {(p.allowedStatuses.length > 0 || p.canAssign) && (
            <section className="card actions-card">
              <h2>Actions</h2>

              {p.allowedStatuses.length > 0 && (
                <div className="action-group">
                  <span className="label">Update status</span>
                  <div className="btn-row">
                    {p.allowedStatuses.map((s) => (
                      <button
                        key={s}
                        className={`btn btn-sm ${s === "REJECTED" ? "btn-danger-ghost" : pending === s ? "btn-primary" : "btn-outline"}`}
                        onClick={() => setPending(pending === s ? null : s)}
                      >
                        {c.status === "RESOLVED" && s === "IN_PROGRESS" ? "Reopen" : ACTION_LABELS[s]}
                      </button>
                    ))}
                  </div>
                  {pending && (
                    <div className="status-form">
                      <textarea
                        rows={3}
                        maxLength={500}
                        value={note}
                        onChange={(e) => setNote(e.target.value)}
                        placeholder={NOTE_REQUIRED(c.status, pending) ? "Reason (required)" : "Note for the reporter (optional)"}
                      />
                      <div className="btn-row">
                        <button className="btn btn-ghost btn-sm" onClick={() => setPending(null)}>
                          Cancel
                        </button>
                        <button className="btn btn-primary btn-sm" disabled={busy} onClick={submitStatus}>
                          Confirm
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {p.canAssign && (
                <div className="action-group">
                  <span className="label">{c.assignee ? "Reassign" : "Assign to staff"}</span>
                  <div className="assign-row">
                    <select value={staffId} onChange={(e) => setStaffId(e.target.value)} aria-label="Staff member">
                      <option value="">Choose staff…</option>
                      {staff
                        .filter((s) => s.id !== c.assignee?.id)
                        .map((s) => (
                          <option key={s.id} value={s.id}>
                            {s.name}
                            {s.department ? ` — ${s.department}` : ""}
                          </option>
                        ))}
                    </select>
                    <button className="btn btn-primary btn-sm" disabled={busy || !staffId} onClick={submitAssign}>
                      Assign
                    </button>
                  </div>
                </div>
              )}
            </section>
          )}

          <section className="card">
            <h2>Activity</h2>
            <ol className="timeline">
              {c.history.map((h) => (
                <li key={h.id} className={`timeline-item to-${h.toStatus.toLowerCase()}`}>
                  <span className="timeline-dot" aria-hidden="true" />
                  <div>
                    <p>
                      <strong>{h.changedByName}</strong> {historyText(h)}
                    </p>
                    {h.note && !(h.fromStatus === h.toStatus) && h.fromStatus && <p className="timeline-note">"{h.note}"</p>}
                    <span className="muted small">{formatDate(h.createdAt)}</span>
                  </div>
                </li>
              ))}
            </ol>
          </section>
        </aside>
      </div>
    </div>
  );
}
