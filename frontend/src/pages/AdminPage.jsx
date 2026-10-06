import { useCallback, useEffect, useState } from "react";
import { categoryApi, userApi } from "../api";
import { RoleBadge } from "../components/Badges";
import ErrorBox from "../components/ErrorBox";
import Spinner from "../components/Spinner";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { PASSWORD_RULE, fieldErrors, isEmail, isStrongPassword } from "../utils/validation";
import { formatDate } from "../utils/format";

const NEW_USER = { name: "", email: "", password: "", role: "STAFF", department: "" };

function UsersTab() {
  const { user: me } = useAuth();
  const toast = useToast();
  const [users, setUsers] = useState(null);
  const [error, setError] = useState("");
  const [form, setForm] = useState(NEW_USER);
  const [errors, setErrors] = useState({});
  const [filter, setFilter] = useState("");

  const load = useCallback(() => {
    userApi
      .list(filter)
      .then(setUsers)
      .catch((e) => setError(e.message));
  }, [filter]);
  useEffect(load, [load]);

  const create = async (e) => {
    e.preventDefault();
    const found = {};
    if (!form.name.trim()) found.name = "Name is required.";
    if (!isEmail(form.email)) found.email = "Enter a valid email address.";
    if (!isStrongPassword(form.password)) found.password = `Password must be ${PASSWORD_RULE}`;
    setErrors(found);
    if (Object.keys(found).length) return;
    try {
      const created = await userApi.create({ ...form, name: form.name.trim(), email: form.email.trim() });
      toast(`${created.name} added as ${created.role.toLowerCase()}.`);
      setForm(NEW_USER);
      load();
    } catch (err) {
      setErrors(fieldErrors(err.details));
      toast(err.message, "error");
    }
  };

  const toggle = async (u) => {
    try {
      await userApi.setActive(u.id, !u.active);
      toast(`${u.name} ${u.active ? "disabled" : "enabled"}.`);
      load();
    } catch (err) {
      toast(err.message, "error");
    }
  };

  const input = (key, label, type = "text") => (
    <label className={`field${errors[key] ? " has-error" : ""}`}>
      <span>{label}</span>
      <input type={type} value={form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.value })} />
      {errors[key] && <small className="field-error">{errors[key]}</small>}
    </label>
  );

  return (
    <div className="admin-grid">
      <section className="card">
        <div className="card-head">
          <h2>People</h2>
          <select value={filter} onChange={(e) => setFilter(e.target.value)} aria-label="Filter by role">
            <option value="">All roles</option>
            <option value="STUDENT">Students</option>
            <option value="STAFF">Staff</option>
            <option value="ADMIN">Admins</option>
          </select>
        </div>
        <ErrorBox message={error} onRetry={load} />
        {!users && !error && <Spinner />}
        {users && (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Role</th>
                  <th>Department</th>
                  <th>Joined</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id} className={u.active ? "" : "row-disabled"}>
                    <td>
                      <strong>{u.name}</strong>
                      <div className="muted small">{u.email}</div>
                    </td>
                    <td>
                      <RoleBadge role={u.role} />
                    </td>
                    <td>{u.department || "—"}</td>
                    <td className="small nowrap">{formatDate(u.createdAt).split(",")[0]}</td>
                    <td className="right">
                      {u.id !== me.id && (
                        <button className={`btn btn-sm ${u.active ? "btn-danger-ghost" : "btn-outline"}`} onClick={() => toggle(u)}>
                          {u.active ? "Disable" : "Enable"}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <form className="card form-card" onSubmit={create} noValidate>
        <h2>Add staff or admin</h2>
        <p className="muted small">Students sign up themselves. Staff and admin accounts are created here.</p>
        {input("name", "Full name")}
        {input("email", "Email", "email")}
        {input("department", "Department")}
        {input("password", "Temporary password", "password")}
        <label className="field">
          <span>Role</span>
          <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
            <option value="STAFF">Staff</option>
            <option value="ADMIN">Admin</option>
          </select>
        </label>
        <button className="btn btn-primary btn-block">Create account</button>
      </form>
    </div>
  );
}

function CategoriesTab() {
  const toast = useToast();
  const [categories, setCategories] = useState(null);
  const [error, setError] = useState("");
  const [form, setForm] = useState({ name: "", description: "" });
  const [editing, setEditing] = useState(null);

  const load = useCallback(() => {
    categoryApi
      .list(true)
      .then(setCategories)
      .catch((e) => setError(e.message));
  }, []);
  useEffect(load, [load]);

  const save = async (body, id) => {
    try {
      if (id) await categoryApi.update(id, body);
      else await categoryApi.create(body);
      toast(id ? "Category updated." : "Category added.");
      load();
      return true;
    } catch (err) {
      toast(err.message, "error");
      return false;
    }
  };

  const add = async (e) => {
    e.preventDefault();
    if (!form.name.trim()) return toast("Enter a category name.", "error");
    if (await save({ name: form.name.trim(), description: form.description.trim() })) setForm({ name: "", description: "" });
  };

  return (
    <div className="admin-grid">
      <section className="card">
        <h2>Categories</h2>
        <p className="muted small">Hidden categories stay on old complaints but can't be picked for new ones.</p>
        <ErrorBox message={error} onRetry={load} />
        {!categories && !error && <Spinner />}
        <ul className="category-list">
          {categories?.map((c) =>
            editing?.id === c.id ? (
              <li key={c.id} className="category-item editing">
                <input value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} />
                <input
                  value={editing.description || ""}
                  onChange={(e) => setEditing({ ...editing, description: e.target.value })}
                  placeholder="Description"
                />
                <div className="btn-row">
                  <button className="btn btn-ghost btn-sm" onClick={() => setEditing(null)}>
                    Cancel
                  </button>
                  <button
                    className="btn btn-primary btn-sm"
                    onClick={async () => (await save(editing, c.id)) && setEditing(null)}
                  >
                    Save
                  </button>
                </div>
              </li>
            ) : (
              <li key={c.id} className={`category-item${c.active ? "" : " row-disabled"}`}>
                <div>
                  <strong>{c.name}</strong>
                  {!c.active && <span className="badge private">Hidden</span>}
                  <div className="muted small">{c.description || "No description"}</div>
                </div>
                <div className="btn-row">
                  <button className="btn btn-ghost btn-sm" onClick={() => setEditing(c)}>
                    Edit
                  </button>
                  <button className="btn btn-outline btn-sm" onClick={() => save({ ...c, active: !c.active }, c.id)}>
                    {c.active ? "Hide" : "Show"}
                  </button>
                </div>
              </li>
            ),
          )}
        </ul>
      </section>

      <form className="card form-card" onSubmit={add} noValidate>
        <h2>New category</h2>
        <label className="field">
          <span>Name</span>
          <input value={form.name} maxLength={60} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </label>
        <label className="field">
          <span>Description</span>
          <input
            value={form.description}
            maxLength={255}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </label>
        <button className="btn btn-primary btn-block">Add category</button>
      </form>
    </div>
  );
}

export default function AdminPage() {
  const [tab, setTab] = useState("users");
  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Admin</h1>
          <p className="muted">Manage people and complaint categories.</p>
        </div>
      </header>
      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={tab === "users"} className={`tab${tab === "users" ? " active" : ""}`} onClick={() => setTab("users")}>
          Users
        </button>
        <button
          role="tab"
          aria-selected={tab === "categories"}
          className={`tab${tab === "categories" ? " active" : ""}`}
          onClick={() => setTab("categories")}
        >
          Categories
        </button>
      </div>
      {tab === "users" ? <UsersTab /> : <CategoriesTab />}
    </div>
  );
}
