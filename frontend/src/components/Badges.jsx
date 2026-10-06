import { PRIORITY_LABELS, ROLE_LABELS, STATUS_LABELS } from "../utils/format";

export function StatusBadge({ status }) {
  return <span className={`badge status-${status.toLowerCase()}`}>{STATUS_LABELS[status] || status}</span>;
}

export function PriorityBadge({ priority }) {
  return (
    <span className={`badge priority-${priority.toLowerCase()}`}>
      <span className="dot" aria-hidden="true" />
      {PRIORITY_LABELS[priority] || priority}
    </span>
  );
}

export function RoleBadge({ role }) {
  return <span className={`badge role-${role.toLowerCase()}`}>{ROLE_LABELS[role] || role}</span>;
}

export function OverdueBadge() {
  return <span className="badge overdue">Overdue</span>;
}
