import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { PASSWORD_RULE, fieldErrors, isEmail, isStrongPassword } from "../utils/validation";
import AuthLayout from "./AuthLayout";

const EMPTY = { name: "", email: "", department: "", password: "", confirm: "" };

function validate(f) {
  const errors = {};
  if (!f.name.trim()) errors.name = "Name is required.";
  if (!isEmail(f.email)) errors.email = "Enter a valid email address.";
  if (!isStrongPassword(f.password)) errors.password = `Password must be ${PASSWORD_RULE}`;
  if (f.confirm !== f.password) errors.confirm = "Passwords don't match.";
  return errors;
}

export default function RegisterPage() {
  const { user, register } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const found = validate(form);
    setErrors(found);
    setError("");
    if (Object.keys(found).length) return;
    setBusy(true);
    try {
      const u = await register({
        name: form.name.trim(),
        email: form.email.trim(),
        department: form.department.trim(),
        password: form.password,
      });
      toast(`Welcome to CampusConnect, ${u.name.split(" ")[0]}!`);
      navigate("/", { replace: true });
    } catch (err) {
      setErrors(fieldErrors(err.details));
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const Field = ({ name, label, type = "text", placeholder, autoComplete }) => (
    <label className={`field${errors[name] ? " has-error" : ""}`}>
      <span>{label}</span>
      <input type={type} value={form[name]} onChange={set(name)} placeholder={placeholder} autoComplete={autoComplete} />
      {errors[name] && <small className="field-error">{errors[name]}</small>}
    </label>
  );

  return (
    <AuthLayout>
      <form className="auth-form" onSubmit={submit} noValidate>
        <h2>Create your account</h2>
        <p className="muted">Student accounts can report issues and upvote others.</p>

        {error && <div className="alert alert-error">{error}</div>}

        {Field({ name: "name", label: "Full name", placeholder: "Ananya Reddy", autoComplete: "name" })}
        {Field({ name: "email", label: "College email", type: "email", placeholder: "you@campus.edu", autoComplete: "email" })}
        {Field({ name: "department", label: "Department (optional)", placeholder: "CSE" })}
        <div className="field-row">
          {Field({ name: "password", label: "Password", type: "password", autoComplete: "new-password" })}
          {Field({ name: "confirm", label: "Confirm password", type: "password", autoComplete: "new-password" })}
        </div>
        {!errors.password && <small className="muted hint">Password: {PASSWORD_RULE}</small>}

        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? "Creating account…" : "Create account"}
        </button>
        <p className="auth-switch">
          Already registered? <Link to="/login">Log in</Link>
        </p>
      </form>
    </AuthLayout>
  );
}
