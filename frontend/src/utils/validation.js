// Same rules as the backend, so users see problems before submitting.
export const PASSWORD_RULE = "8-64 characters with at least one letter and one number.";

export const isEmail = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim());
export const isStrongPassword = (v) => /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/.test(v);

/** Maps backend field errors [{field, message}] to {field: message}. */
export const fieldErrors = (details = []) =>
  Object.fromEntries(details.map((d) => [d.field, d.message]));
