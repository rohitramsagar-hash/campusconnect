export const STATUS_LABELS = {
  OPEN: "Open",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
  REJECTED: "Rejected",
};

export const PRIORITY_LABELS = { LOW: "Low", MEDIUM: "Medium", HIGH: "High", URGENT: "Urgent" };

export const PRIORITY_TARGETS = { LOW: "14 days", MEDIUM: "7 days", HIGH: "3 days", URGENT: "24 hours" };

export const ROLE_LABELS = { STUDENT: "Student", STAFF: "Staff", ADMIN: "Admin" };

/** Button text for moving a complaint to a status. */
export const ACTION_LABELS = {
  IN_PROGRESS: "Start work",
  RESOLVED: "Mark resolved",
  CLOSED: "Confirm & close",
  REJECTED: "Reject",
};

export function formatDate(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function timeAgo(iso) {
  if (!iso) return "";
  const seconds = Math.round((Date.now() - new Date(iso).getTime()) / 1000);
  const future = seconds < 0;
  const s = Math.abs(seconds);
  const units = [
    ["year", 31536000],
    ["month", 2592000],
    ["day", 86400],
    ["hour", 3600],
    ["minute", 60],
  ];
  for (const [name, size] of units) {
    const n = Math.floor(s / size);
    if (n >= 1) {
      const text = `${n} ${name}${n > 1 ? "s" : ""}`;
      return future ? `in ${text}` : `${text} ago`;
    }
  }
  return "just now";
}

export function initials(name = "") {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join("");
}

export function greeting() {
  const h = new Date().getHours();
  if (h < 12) return "Good morning";
  if (h < 17) return "Good afternoon";
  return "Good evening";
}
