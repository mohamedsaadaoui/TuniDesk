import { api } from "./client";
import type {
  AuditLog,
  AuthResponse,
  AssignRequest,
  Category,
  CategoryRequest,
  ChangePasswordRequest,
  CommentRequest,
  CreateUserRequest,
  DashboardSummary,
  ForgotPasswordRequest,
  LoginRequest,
  Notification,
  Page,
  ProfileUpdateRequest,
  RegisterRequest,
  ResetPasswordRequest,
  SlaPolicy,
  SlaPolicyRequest,
  StatusChangeRequest,
  Ticket,
  TicketAttachment,
  TicketDetail,
  TicketPriority,
  TicketRequest,
  TicketStatus,
  UpdateUserRequest,
  User,
  UserOption,
} from "./types";

export const authApi = {
  login: (data: LoginRequest) => api.post<AuthResponse>("/auth/login", data).then((r) => r.data),
  register: (data: RegisterRequest) =>
    api.post<AuthResponse>("/auth/register", data).then((r) => r.data),
  me: () => api.get<User>("/auth/me").then((r) => r.data),
  forgotPassword: (data: ForgotPasswordRequest) =>
    api.post<void>("/auth/forgot-password", data).then((r) => r.data),
  resetPassword: (data: ResetPasswordRequest) =>
    api.post<void>("/auth/reset-password", data).then((r) => r.data),
};

export const dashboardApi = {
  summary: () => api.get<DashboardSummary>("/dashboard/summary").then((r) => r.data),
};

export interface TicketQuery {
  search?: string;
  status?: TicketStatus;
  priority?: TicketPriority;
  categoryId?: string;
  assignedToId?: string;
  slaBreached?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: string;
}

export const ticketApi = {
  list: (params: TicketQuery) =>
    api.get<Page<Ticket>>("/tickets", { params }).then((r) => r.data),
  get: (id: string) => api.get<TicketDetail>(`/tickets/${id}`).then((r) => r.data),
  create: (data: TicketRequest) => api.post<Ticket>("/tickets", data).then((r) => r.data),
  update: (id: string, data: TicketRequest) =>
    api.put<Ticket>(`/tickets/${id}`, data).then((r) => r.data),
  assign: (id: string, data: AssignRequest) =>
    api.post<Ticket>(`/tickets/${id}/assign`, data).then((r) => r.data),
  take: (id: string) => api.post<Ticket>(`/tickets/${id}/take`).then((r) => r.data),
  changeStatus: (id: string, data: StatusChangeRequest) =>
    api.post<Ticket>(`/tickets/${id}/status`, data).then((r) => r.data),
  addComment: (id: string, data: CommentRequest) =>
    api.post(`/tickets/${id}/comments`, data).then((r) => r.data),
  remove: (id: string) => api.delete(`/tickets/${id}`),
  exportCsv: (params: TicketQuery) => {
    const { page, size, sortBy, direction, ...filters } = params;
    return api.get<Blob>("/tickets/export", { params: filters, responseType: "blob" }).then((r) => r.data);
  },
};

export const categoryApi = {
  list: (activeOnly = false) =>
    api.get<Category[]>("/categories", { params: { activeOnly } }).then((r) => r.data),
  create: (data: CategoryRequest) => api.post<Category>("/categories", data).then((r) => r.data),
  update: (id: string, data: CategoryRequest) =>
    api.put<Category>(`/categories/${id}`, data).then((r) => r.data),
  remove: (id: string) => api.delete(`/categories/${id}`),
};

export const slaApi = {
  list: () => api.get<SlaPolicy[]>("/sla/policies").then((r) => r.data),
  update: (id: string, data: SlaPolicyRequest) =>
    api.put<SlaPolicy>(`/sla/policies/${id}`, data).then((r) => r.data),
};

export const userApi = {
  list: (params: { search?: string; enabled?: boolean; page?: number; size?: number }) =>
    api.get<Page<User>>("/users", { params }).then((r) => r.data),
  agents: (search?: string) =>
    api.get<UserOption[]>("/users/agents", { params: { search } }).then((r) => r.data),
  options: (search?: string) =>
    api.get<UserOption[]>("/users/options", { params: { search } }).then((r) => r.data),
  create: (data: CreateUserRequest) => api.post<User>("/users", data).then((r) => r.data),
  update: (id: string, data: UpdateUserRequest) =>
    api.put<User>(`/users/${id}`, data).then((r) => r.data),
  remove: (id: string) => api.delete(`/users/${id}`),
  profile: (data: ProfileUpdateRequest) => api.put<User>("/users/me", data).then((r) => r.data),
  changePassword: (data: ChangePasswordRequest) =>
    api.put<void>("/users/me/password", data).then((r) => r.data),
};

export const attachmentApi = {
  upload: (ticketId: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return api
      .post<TicketAttachment>(`/tickets/${ticketId}/attachments`, formData)
      .then((r) => r.data);
  },
  download: (id: string) =>
    api.get<Blob>(`/attachments/${id}/download`, { responseType: "blob" }).then((r) => r.data),
  remove: (id: string) => api.delete(`/attachments/${id}`),
};

export const notificationApi = {
  list: (unreadOnly = false, page = 0, size = 10) =>
    api
      .get<Page<Notification>>("/notifications", { params: { unreadOnly, page, size } })
      .then((r) => r.data),
  unreadCount: () => api.get<number>("/notifications/unread-count").then((r) => r.data),
  markRead: (id: string) => api.patch<Notification>(`/notifications/${id}/read`).then((r) => r.data),
  markAllRead: () => api.post("/notifications/read-all"),
  remove: (id: string) => api.delete(`/notifications/${id}`),
};

export const auditApi = {
  list: (params: { search?: string; page?: number; size?: number }) =>
    api.get<Page<AuditLog>>("/audit-logs", { params }).then((r) => r.data),
};
