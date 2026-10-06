import api from "./client";

const data = (res) => res.data;

/** Drops empty values so they aren't sent as ?status=&q= */
const clean = (params = {}) =>
  Object.fromEntries(Object.entries(params).filter(([, v]) => v !== "" && v !== null && v !== undefined));

export const authApi = {
  login: (body) => api.post("/auth/login", body).then(data),
  register: (body) => api.post("/auth/register", body).then(data),
  me: () => api.get("/auth/me").then(data),
};

export const complaintApi = {
  list: (params) => api.get("/complaints", { params: clean(params) }).then(data),
  get: (id) => api.get(`/complaints/${id}`).then(data),
  create: (body) => api.post("/complaints", body).then(data),
  setStatus: (id, status, note) => api.patch(`/complaints/${id}/status`, { status, note }).then(data),
  assign: (id, staffId) => api.patch(`/complaints/${id}/assign`, { staffId }).then(data),
  comment: (id, message) => api.post(`/complaints/${id}/comments`, { message }).then(data),
  upvote: (id) => api.post(`/complaints/${id}/upvote`).then(data),
  removeUpvote: (id) => api.delete(`/complaints/${id}/upvote`).then(data),
};

export const categoryApi = {
  list: (all = false) => api.get("/categories", { params: all ? { all: true } : {} }).then(data),
  create: (body) => api.post("/categories", body).then(data),
  update: (id, body) => api.put(`/categories/${id}`, body).then(data),
};

export const userApi = {
  list: (role) => api.get("/users", { params: clean({ role }) }).then(data),
  create: (body) => api.post("/users", body).then(data),
  setActive: (id, active) => api.patch(`/users/${id}/status`, { active }).then(data),
};

export const dashboardApi = {
  get: () => api.get("/dashboard").then(data),
};
