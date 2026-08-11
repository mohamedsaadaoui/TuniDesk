export type TicketStatus = "OPEN" | "ASSIGNED" | "IN_PROGRESS" | "RESOLVED" | "CLOSED";

export type TicketPriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  roles: string[];
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phoneNumber?: string;
}

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber?: string;
  enabled: boolean;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface UserOption {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface CreateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phoneNumber?: string;
  role?: string;
}

export interface UpdateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber?: string;
  enabled: boolean;
  roles: string[];
}

export interface ProfileUpdateRequest {
  firstName: string;
  lastName: string;
  phoneNumber?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface Ticket {
  id: string;
  ticketNumber: string;
  title: string;
  description?: string;
  priority: TicketPriority;
  status: TicketStatus;
  createdById: string;
  createdByFirstName: string;
  createdByLastName: string;
  createdByEmail: string;
  assignedToId?: string;
  assignedToFirstName?: string;
  assignedToLastName?: string;
  assignedToEmail?: string;
  categoryId: string;
  categoryName: string;
  slaDueAt?: string;
  slaBreached: boolean;
  slaTimeRemaining?: string;
  slaRemainingMinutes?: number;
  resolvedAt?: string;
  closedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TicketRequest {
  title: string;
  description?: string;
  priority: TicketPriority;
  categoryId: string;
}

export interface AssignRequest {
  assignedToId?: string;
  note?: string;
}

export interface StatusChangeRequest {
  status: TicketStatus;
  comment?: string;
}

export interface CommentRequest {
  content: string;
  isInternal?: boolean;
}

export interface TicketComment {
  id: string;
  authorId: string;
  authorFirstName: string;
  authorLastName: string;
  authorEmail: string;
  content: string;
  isInternal: boolean;
  createdAt: string;
}

export interface TicketHistory {
  id: string;
  actor: string;
  action: string;
  fromStatus?: string;
  toStatus?: string;
  details?: string;
  createdAt: string;
}

export interface TicketAttachment {
  id: string;
  originalName: string;
  contentType: string;
  size: number;
  uploadedById: string;
  uploadedByFirstName: string;
  uploadedByLastName: string;
  createdAt: string;
}

export interface TicketDetail {
  ticket: Ticket;
  comments: TicketComment[];
  history: TicketHistory[];
  attachments: TicketAttachment[];
}

export interface Category {
  id: string;
  name: string;
  description?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryRequest {
  name: string;
  description?: string;
  active: boolean;
}

export interface SlaPolicy {
  id: string;
  name: string;
  priority: TicketPriority;
  hours: number;
  createdAt: string;
  updatedAt: string;
}

export interface SlaPolicyRequest {
  name: string;
  hours: number;
}

export interface StatusCount {
  status: TicketStatus;
  count: number;
}

export interface PriorityCount {
  priority: TicketPriority;
  count: number;
}

export interface CategoryTicketCount {
  name: string;
  count: number;
}

export interface DashboardSummary {
  totalTickets: number;
  openTickets: number;
  inProgressTickets: number;
  resolvedTickets: number;
  closedTickets: number;
  unassignedTickets: number;
  slaBreachedTickets: number;
  avgResolutionHours: number | null;
  ticketsByStatus: StatusCount[];
  ticketsByPriority: PriorityCount[];
  ticketsByCategory: CategoryTicketCount[];
  recentTickets: Ticket[];
}

export interface Notification {
  id: string;
  senderId?: string;
  senderName?: string;
  recipientId: string;
  title: string;
  message: string;
  type: string;
  read: boolean;
  createdAt: string;
}

export interface NotificationRequest {
  recipientId: string;
  title: string;
  message: string;
}

export interface AuditLog {
  id: string;
  action: string;
  entityType: string;
  entityId?: string;
  details?: string;
  performedBy?: string;
  createdAt: string;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
  details?: Record<string, string>;
}
