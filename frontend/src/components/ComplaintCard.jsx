import { Link } from "react-router-dom";
import { timeAgo } from "../utils/format";
import { OverdueBadge, PriorityBadge, StatusBadge } from "./Badges";

export default function ComplaintCard({ complaint: c, compact = false }) {
  return (
    <Link to={`/complaints/${c.id}`} className={`complaint-card${compact ? " compact" : ""}`}>
      <div className="complaint-card-top">
        <span className="ticket">{c.ticketNo}</span>
        <div className="badge-row">
          {c.overdue && <OverdueBadge />}
          <PriorityBadge priority={c.priority} />
          <StatusBadge status={c.status} />
        </div>
      </div>
      <h3 className="complaint-title">{c.title}</h3>
      <div className="complaint-meta">
        <span>{c.category}</span>
        {c.location && <span>📍 {c.location}</span>}
        {!compact && <span>By {c.reporterName}</span>}
        {!compact && <span>{c.assigneeName ? `Assigned to ${c.assigneeName}` : "Not assigned yet"}</span>}
        <span title={new Date(c.createdAt).toLocaleString()}>{timeAgo(c.createdAt)}</span>
        <span className={c.upvotedByMe ? "upvotes mine" : "upvotes"}>▲ {c.upvoteCount}</span>
      </div>
    </Link>
  );
}
