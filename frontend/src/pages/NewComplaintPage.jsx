import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { categoryApi, complaintApi } from "../api";
import ErrorBox from "../components/ErrorBox";
import { useToast } from "../context/ToastContext";
import { PRIORITY_LABELS, PRIORITY_TARGETS } from "../utils/format";
import { fieldErrors } from "../utils/validation";

const EMPTY = {
  title: "",
  categoryId: "",
  priority: "MEDIUM",
  location: "",
  description: "",
  anonymous: false,
  isPublic: true,
};

const PRIORITY_HELP = {
  LOW: "Minor inconvenience",
  MEDIUM: "Affects daily routine",
  HIGH: "Blocks classes or work",
  URGENT: "Safety risk / emergency",
};

function validate(f) {
  const e = {};
  if (f.title.trim().length < 5) e.title = "Title must be at least 5 characters.";
  if (!f.categoryId) e.categoryId = "Choose a category.";
  if (f.description.trim().length < 10) e.description = "Describe the issue in at least 10 characters.";
  return e;
}

export default function NewComplaintPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    categoryApi
      .list()
      .then(setCategories)
      .catch((e) => setError(e.message));
  }, []);

  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));

  const submit = async (e) => {
    e.preventDefault();
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length) return;
    setBusy(true);
    setError("");
    try {
      const created = await complaintApi.create({
        ...form,
        title: form.title.trim(),
        description: form.description.trim(),
        location: form.location.trim(),
        categoryId: Number(form.categoryId),
      });
      toast(`Complaint ${created.ticketNo} submitted.`);
      navigate(`/complaints/${created.id}`);
    } catch (err) {
      setErrors(fieldErrors(err.details));
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="page page-narrow">
      <header className="page-header">
        <div>
          <Link to="/complaints" className="link back-link">
            ← Back to complaints
          </Link>
          <h1>Report an issue</h1>
          <p className="muted">The more specific you are, the faster it gets fixed.</p>
        </div>
      </header>

      <form className="card form-card" onSubmit={submit} noValidate>
        <ErrorBox message={error} />

        <label className={`field${errors.title ? " has-error" : ""}`}>
          <span>Title</span>
          <input
            value={form.title}
            maxLength={150}
            onChange={(e) => set("title", e.target.value)}
            placeholder="e.g. Fan not working in Room 204"
          />
          {errors.title && <small className="field-error">{errors.title}</small>}
        </label>

        <div className="field-row">
          <label className={`field${errors.categoryId ? " has-error" : ""}`}>
            <span>Category</span>
            <select value={form.categoryId} onChange={(e) => set("categoryId", e.target.value)}>
              <option value="">Choose a category</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
            {errors.categoryId && <small className="field-error">{errors.categoryId}</small>}
          </label>
          <label className="field">
            <span>Location (optional)</span>
            <input
              value={form.location}
              maxLength={150}
              onChange={(e) => set("location", e.target.value)}
              placeholder="Block, floor, room"
            />
          </label>
        </div>

        <fieldset className="field">
          <span>Priority</span>
          <div className="priority-picker">
            {Object.keys(PRIORITY_LABELS).map((p) => (
              <label key={p} className={`priority-option p-${p.toLowerCase()}${form.priority === p ? " selected" : ""}`}>
                <input
                  type="radio"
                  name="priority"
                  value={p}
                  checked={form.priority === p}
                  onChange={() => set("priority", p)}
                />
                <strong>{PRIORITY_LABELS[p]}</strong>
                <small>{PRIORITY_HELP[p]}</small>
                <small className="target">Target: {PRIORITY_TARGETS[p]}</small>
              </label>
            ))}
          </div>
        </fieldset>

        <label className={`field${errors.description ? " has-error" : ""}`}>
          <span>Description</span>
          <textarea
            rows={6}
            maxLength={2000}
            value={form.description}
            onChange={(e) => set("description", e.target.value)}
            placeholder="What's wrong, since when, and how it affects you."
          />
          <div className="field-foot">
            {errors.description ? <small className="field-error">{errors.description}</small> : <span />}
            <small className="muted">{form.description.length}/2000</small>
          </div>
        </label>

        <div className="toggles">
          <label className="toggle">
            <input type="checkbox" checked={form.anonymous} onChange={(e) => set("anonymous", e.target.checked)} />
            <span className="toggle-ui" aria-hidden="true" />
            <span>
              <strong>Post anonymously</strong>
              <small className="muted">Your name is hidden from other students and staff (admins can still see it).</small>
            </span>
          </label>
          <label className="toggle">
            <input type="checkbox" checked={!form.isPublic} onChange={(e) => set("isPublic", !e.target.checked)} />
            <span className="toggle-ui" aria-hidden="true" />
            <span>
              <strong>Keep private</strong>
              <small className="muted">Only you, the assigned staff and admins can see it. Use for personal matters.</small>
            </span>
          </label>
        </div>

        <div className="form-actions">
          <Link to="/complaints" className="btn btn-ghost">
            Cancel
          </Link>
          <button className="btn btn-primary" disabled={busy}>
            {busy ? "Submitting…" : "Submit complaint"}
          </button>
        </div>
      </form>
    </div>
  );
}
