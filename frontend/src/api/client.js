import axios from "axios";

const TOKEN_KEY = "campusconnect_token";

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

// Backend address. Locally this is "/api" (Vite forwards it to port 8080).
// When deployed, VITE_API_URL points at the hosted backend; "/api" is added if it was left off.
const rawApiUrl = (import.meta.env.VITE_API_URL || "").trim().replace(/\/+$/, "");
const API_BASE = !rawApiUrl ? "/api" : rawApiUrl.endsWith("/api") ? rawApiUrl : `${rawApiUrl}/api`;

const api = axios.create({
  baseURL: API_BASE,
  // A free hosted backend can take up to a minute to wake up, so wait longer than usual.
  timeout: 60000,
});

api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

/**
 * Every backend error looks like {error, message, details}. We turn it into a normal Error
 * whose .message is safe to show to the user.
 */
api.interceptors.response.use(
  (response) => response,
  (err) => {
    const status = err.response?.status;
    const data = err.response?.data;
    const hasApiError = data && typeof data === "object" && data.message;

    let message;
    if (!err.response || (!hasApiError && [500, 502, 503, 504].includes(status))) {
      message = "Can't reach the server. Make sure the backend is running on port 8080.";
    } else if (hasApiError) {
      message = data.message;
    } else {
      message = `Request failed (${status}).`;
    }

    if (status === 401 && tokenStore.get()) {
      tokenStore.clear();
      window.dispatchEvent(new Event("auth:logout"));
    }

    const error = new Error(message);
    error.status = status;
    error.code = data?.error;
    error.details = data?.details || [];
    return Promise.reject(error);
  },
);

export default api;
