import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { isEmail } from "../utils/validation";
import AuthLayout from "./AuthLayout";

const DEMO_ACCOUNTS = [
  { label: "Student", email: "student@campus.edu", password: "Student@123" },
  { label: "Staff", email: "ravi.staff@campus.edu", password: "Staff@123" },
  { label: "Admin", email: "admin@campus.edu", password: "Admin@123" },
];

export default function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const submit = async (e) => {
    e.preventDefault();
    if (!isEmail(form.email)) return setError("Enter a valid email address.");
    if (!form.password) return setError("Enter your password.");
    setBusy(true);
    setError("");
    try {
      await login(form.email.trim(), form.password);
      navigate(location.state?.from || "/", { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout>
      <form className="auth-form" onSubmit={submit} noValidate>
        <h2>Welcome back</h2>
        <p className="muted">Log in to report and track campus issues.</p>

        {error && <div className="alert alert-error">{error}</div>}

        <label className="field">
          <span>Email</span>
          <input
            type="email"
            autoComplete="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            placeholder="you@campus.edu"
          />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            type="password"
            autoComplete="current-password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            placeholder="Your password"
          />
        </label>

        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? "Logging in…" : "Log in"}
        </button>

        <p className="auth-switch">
          New here? <Link to="/register">Create a student account</Link>
        </p>

        <div className="demo-box">
          <span className="muted">Demo accounts — click to fill</span>
          <div className="demo-buttons">
            {DEMO_ACCOUNTS.map((a) => (
              <button
                type="button"
                key={a.label}
                className="chip"
                onClick={() => {
                  setForm({ email: a.email, password: a.password });
                  setError("");
                }}
              >
                {a.label}
              </button>
            ))}
          </div>
        </div>
      </form>
    </AuthLayout>
  );
}
