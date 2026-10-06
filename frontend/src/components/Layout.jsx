import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { initials } from "../utils/format";
import { RoleBadge } from "./Badges";

const Icon = ({ d }) => (
  <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
    <path d={d} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const ICONS = {
  dashboard: "M4 13h6V4H4zM14 20h6v-9h-6zM4 20h6v-3H4zM14 7h6V4h-6z",
  complaints: "M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01",
  add: "M12 5v14M5 12h14",
  admin: "M12 3l8 4v5c0 4.5-3.4 8.3-8 9-4.6-.7-8-4.5-8-9V7z",
};

export default function Layout() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const links = [
    { to: "/", label: "Dashboard", icon: "dashboard", end: true },
    { to: "/complaints", label: "Complaints", icon: "complaints", end: true },
    { to: "/complaints/new", label: "Report an issue", icon: "add" },
    ...(isAdmin ? [{ to: "/admin", label: "Admin", icon: "admin" }] : []),
  ];

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="app-shell">
      <aside className={`sidebar${menuOpen ? " open" : ""}`}>
        <div className="brand">
          <img src="/favicon.svg" alt="" width="34" height="34" />
          <div>
            <strong>CampusConnect</strong>
            <span>Issue &amp; complaint desk</span>
          </div>
          <button className="menu-toggle" onClick={() => setMenuOpen(!menuOpen)} aria-label="Toggle menu">
            ☰
          </button>
        </div>
        <nav className="nav" onClick={() => setMenuOpen(false)}>
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end} className="nav-link">
              <Icon d={ICONS[l.icon]} />
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-user">
          <div className="avatar">{initials(user.name)}</div>
          <div className="sidebar-user-info">
            <strong>{user.name}</strong>
            <RoleBadge role={user.role} />
          </div>
          <button className="btn btn-ghost btn-sm" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
