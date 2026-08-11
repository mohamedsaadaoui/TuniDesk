import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { attachmentApi, categoryApi, ticketApi, userApi } from "../api";
import { errorMessage } from "../api/client";
import type {
  Category,
  TicketAttachment,
  TicketDetail,
  TicketPriority,
  TicketStatus,
  UserOption,
} from "../api/types";
import { useAuth } from "../auth/AuthContext";
import {
  Avatar,
  Button,
  Card,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  PriorityBadge,
  Select,
  SlaBadge,
  Spinner,
  StatusBadge,
  Textarea,
} from "../components/ui";
import { formatBytes, formatDateTime, fullName } from "../lib/format";

const TRANSITIONS: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ["ASSIGNED"],
  ASSIGNED: ["IN_PROGRESS", "OPEN"],
  IN_PROGRESS: ["RESOLVED"],
  RESOLVED: ["CLOSED", "OPEN"],
  CLOSED: [],
};

export function TicketDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAdmin, isAgent } = useAuth();

  const [detail, setDetail] = useState<TicketDetail | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [agents, setAgents] = useState<UserOption[]>([]);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");

  const [comment, setComment] = useState("");
  const [commentInternal, setCommentInternal] = useState(false);

  const [statusTarget, setStatusTarget] = useState<TicketStatus | "">("");
  const [statusComment, setStatusComment] = useState("");

  const [assignToId, setAssignToId] = useState("");
  const [assignNote, setAssignNote] = useState("");

  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [editPriority, setEditPriority] = useState<TicketPriority>("MEDIUM");
  const [editCategoryId, setEditCategoryId] = useState("");

  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");

  const load = () => {
    if (!id) return;
    ticketApi
      .get(id)
      .then(setDetail)
      .catch((err) => setError(errorMessage(err)));
  };

  useEffect(load, [id]);
  useEffect(() => {
    categoryApi.list(true).then(setCategories).catch(() => {});
    userApi.agents().then(setAgents).catch(() => {});
  }, []);

  if (error) return <div className="form-error">{error}</div>;
  if (!detail) return <Spinner label="Loading ticket…" />;

  const ticket = detail.ticket;
  const isCreator = user?.id === ticket.createdById;
  const canManage = isAgent;
  const allowed = TRANSITIONS[ticket.status];
  const statusOptions = canManage ? allowed : allowed.filter((s) => s === "OPEN" || s === "CLOSED");

  const reload = () => load();

  const submitComment = async (e: FormEvent) => {
    e.preventDefault();
    setActionError("");
    try {
      await ticketApi.addComment(ticket.id, { content: comment, isInternal: commentInternal });
      setComment("");
      setCommentInternal(false);
      reload();
    } catch (err) {
      setActionError(errorMessage(err));
    }
  };

  const submitStatus = async (e: FormEvent) => {
    e.preventDefault();
    setActionError("");
    if (!statusTarget) return;
    try {
      await ticketApi.changeStatus(ticket.id, { status: statusTarget, comment: statusComment || undefined });
      setStatusTarget("");
      setStatusComment("");
      reload();
    } catch (err) {
      setActionError(errorMessage(err));
    }
  };

  const submitAssign = async (e: FormEvent) => {
    e.preventDefault();
    setActionError("");
    try {
      await ticketApi.assign(ticket.id, { assignedToId: assignToId || undefined, note: assignNote || undefined });
      setAssignToId("");
      setAssignNote("");
      reload();
    } catch (err) {
      setActionError(errorMessage(err));
    }
  };

  const take = async () => {
    setActionError("");
    try {
      await ticketApi.take(ticket.id);
      reload();
    } catch (err) {
      setActionError(errorMessage(err));
    }
  };

  const openEdit = () => {
    setEditTitle(ticket.title);
    setEditDescription(ticket.description ?? "");
    setEditPriority(ticket.priority);
    setEditCategoryId(ticket.categoryId);
    setEditing(true);
  };

  const submitEdit = async (e: FormEvent) => {
    e.preventDefault();
    setActionError("");
    try {
      await ticketApi.update(ticket.id, {
        title: editTitle,
        description: editDescription,
        priority: editPriority,
        categoryId: editCategoryId,
      });
      setEditing(false);
      reload();
    } catch (err) {
      setActionError(errorMessage(err));
    }
  };

  const remove = async () => {
    if (!window.confirm(`Delete ticket ${ticket.ticketNumber}? This cannot be undone.`)) return;
    setActionError("");
    try {
      await ticketApi.remove(ticket.id);
      navigate("/tickets");
    } catch (err) {
      setActionError(errorMessage(err));
    }
  };

  const submitAttachment = async (e: FormEvent) => {
    e.preventDefault();
    if (!file) return;
    setUploading(true);
    setUploadError("");
    setActionError("");
    try {
      await attachmentApi.upload(ticket.id, file);
      setFile(null);
      reload();
    } catch (err) {
      setUploadError(errorMessage(err));
    } finally {
      setUploading(false);
    }
  };

  const downloadAttachment = async (a: TicketAttachment) => {
    setActionError("");
    try {
      const blob = await attachmentApi.download(a.id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = a.originalName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setActionError(errorMessage(err));
    }
  };

  const deleteAttachment = async (a: TicketAttachment) => {
    if (!window.confirm(`Delete attachment "${a.originalName}"?`)) return;
    setActionError("");
    try {
      await attachmentApi.remove(a.id);
      reload();
    } catch (err) {
      setActionError(errorMessage(err));
    }
  };

  return (
    <div>
      <PageHeader
        title={`${ticket.ticketNumber}`}
        subtitle="Ticket details"
        actions={
          <>
            {canManage && (
              <Button variant="secondary" onClick={openEdit}>
                Edit
              </Button>
            )}
            {isAdmin && (
              <Button variant="danger" onClick={remove}>
                Delete
              </Button>
            )}
            <Button variant="secondary" onClick={() => navigate("/tickets")}>
              Back
            </Button>
          </>
        }
      />

      {actionError && <div className="form-error">{actionError}</div>}

      <div className="detail-layout">
        <div className="detail-main">
          <Card className="detail-header">
            <div className="detail-meta">
              <StatusBadge status={ticket.status} />
              <PriorityBadge priority={ticket.priority} />
              <SlaBadge breached={ticket.slaBreached} remaining={ticket.slaTimeRemaining} />
              <span className="tag">{ticket.categoryName}</span>
            </div>
            <h1 className="detail-title">{ticket.title}</h1>
            <div className="muted" style={{ fontSize: 13 }}>
              Created by {fullName(ticket.createdByFirstName, ticket.createdByLastName)} ·{" "}
              {formatDateTime(ticket.createdAt)}
            </div>
            {ticket.description && <div className="detail-desc">{ticket.description}</div>}
          </Card>

          <Card style={{ padding: 20 }}>
            <h3 style={{ marginBottom: 14 }}>Comments</h3>
            {detail.comments.length === 0 && <EmptyState title="No comments yet" />}
            {detail.comments.map((c) => (
              <div className="comment card" key={c.id} style={{ marginBottom: 10 }}>
                <div className="comment-head">
                  <Avatar firstName={c.authorFirstName} lastName={c.authorLastName} />
                  <div>
                    <div className="comment-author">
                      {fullName(c.authorFirstName, c.authorLastName)}
                      {c.isInternal && <span className="internal-tag">internal</span>}
                    </div>
                    <div className="comment-time">{formatDateTime(c.createdAt)}</div>
                  </div>
                </div>
                <div className="comment-body">{c.content}</div>
              </div>
            ))}

            <form onSubmit={submitComment} style={{ marginTop: 16 }}>
              <Field label="Add a comment">
                <Textarea
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Write a comment…"
                  maxLength={2000}
                />
              </Field>
              {isAgent && (
                <label style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 12, fontSize: 13.5 }}>
                  <input
                    type="checkbox"
                    checked={commentInternal}
                    onChange={(e) => setCommentInternal(e.target.checked)}
                  />
                  Internal note (visible to agents only)
                </label>
              )}
              <div className="form-actions" style={{ marginTop: 4 }}>
                <Button type="submit" disabled={!comment.trim()}>
                  Add comment
                </Button>
              </div>
            </form>
          </Card>

          <Card style={{ padding: 20 }}>
            <h3 style={{ marginBottom: 14 }}>Attachments</h3>

            {uploadError && <div className="form-error">{uploadError}</div>}

            {detail.attachments.length === 0 && <EmptyState title="No attachments" />}

            {detail.attachments.map((a) => (
              <div className="attachment-row" key={a.id}>
                <div className="attachment-info">
                  <div className="attachment-name" title={a.originalName}>
                    {a.originalName}
                  </div>
                  <div className="attachment-meta">
                    {formatBytes(a.size)} ·{" "}
                    {fullName(a.uploadedByFirstName, a.uploadedByLastName)} · {formatDateTime(a.createdAt)}
                  </div>
                </div>
                <div className="attachment-actions">
                  <Button variant="ghost" size="sm" onClick={() => downloadAttachment(a)}>
                    Download
                  </Button>
                  {(isAgent || user?.id === a.uploadedById) && (
                    <Button variant="danger" size="sm" onClick={() => deleteAttachment(a)}>
                      Delete
                    </Button>
                  )}
                </div>
              </div>
            ))}

            <form onSubmit={submitAttachment} style={{ marginTop: 16 }} className="stack">
              <Field label="Add a file (max 10 MB)">
                <input
                  className="input"
                  type="file"
                  onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                  disabled={uploading}
                />
              </Field>
              <div className="form-actions" style={{ marginTop: 4 }}>
                <Button type="submit" disabled={!file || uploading}>
                  {uploading ? "Uploading…" : "Upload"}
                </Button>
              </div>
            </form>
          </Card>

          <Card style={{ padding: 20 }}>
            <h3 style={{ marginBottom: 10 }}>History</h3>
            {detail.history.length === 0 && <EmptyState title="No history yet" />}
            {detail.history.map((h) => (
              <div className="history-item" key={h.id}>
                <span className="history-dot" />
                <div>
                  <div className="history-action">{h.action}</div>
                  <div className="muted">
                    {h.details ?? [h.fromStatus, h.toStatus].filter(Boolean).join(" → ")}
                  </div>
                  <div className="history-meta">
                    {h.actor} · {formatDateTime(h.createdAt)}
                  </div>
                </div>
              </div>
            ))}
          </Card>
        </div>

        <div className="detail-side">
          <Card style={{ padding: 20 }}>
            <h3 style={{ marginBottom: 14 }}>Details</h3>
            <div className="info-grid">
              <div className="info-item">
                <span className="info-label">Status</span>
                <span className="info-value">{ticket.status.replace("_", " ")}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Priority</span>
                <span className="info-value">{ticket.priority}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Category</span>
                <span className="info-value">{ticket.categoryName}</span>
              </div>
              <div className="info-item">
                <span className="info-label">SLA due</span>
                <span className="info-value">{formatDateTime(ticket.slaDueAt)}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Assignee</span>
                <span className="info-value">
                  {ticket.assignedToId
                    ? fullName(ticket.assignedToFirstName, ticket.assignedToLastName)
                    : "Unassigned"}
                </span>
              </div>
              <div className="info-item">
                <span className="info-label">Created</span>
                <span className="info-value">{formatDateTime(ticket.createdAt)}</span>
              </div>
            </div>
          </Card>

          {canManage && (
            <Card style={{ padding: 20 }}>
              <h3 style={{ marginBottom: 14 }}>Actions</h3>

              {ticket.assignedToId ? (
                <div style={{ marginBottom: 12, fontSize: 13.5 }} className="muted">
                  Currently assigned to{" "}
                  <strong style={{ color: "var(--text)" }}>
                    {fullName(ticket.assignedToFirstName, ticket.assignedToLastName)}
                  </strong>
                </div>
              ) : (
                <Button variant="secondary" size="md" style={{ width: "100%", marginBottom: 12 }} onClick={take}>
                  Take ticket
                </Button>
              )}

              <form onSubmit={submitAssign}>
                <Field label="Assign to">
                  <Select value={assignToId} onChange={(e) => setAssignToId(e.target.value)}>
                    <option value="">Unassign</option>
                    {agents.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.firstName} {a.lastName} ({a.email})
                      </option>
                    ))}
                  </Select>
                </Field>
                <Field label="Note">
                  <Input
                    value={assignNote}
                    onChange={(e) => setAssignNote(e.target.value)}
                    placeholder="Optional note"
                  />
                </Field>
                <Button type="submit" variant="secondary" style={{ width: "100%" }}>
                  {assignToId ? "Assign" : "Unassign"}
                </Button>
              </form>
            </Card>
          )}

          {statusOptions.length > 0 && (isAgent || isCreator) && (
            <Card style={{ padding: 20 }}>
              <h3 style={{ marginBottom: 14 }}>Change status</h3>
              <form onSubmit={submitStatus}>
                <Field label="Move to">
                  <Select value={statusTarget} onChange={(e) => setStatusTarget(e.target.value as TicketStatus)}>
                    <option value="">Select…</option>
                    {statusOptions.map((s) => (
                      <option key={s} value={s}>
                        {s.replace("_", " ")}
                      </option>
                    ))}
                  </Select>
                </Field>
                <Field label="Comment (optional)">
                  <Input value={statusComment} onChange={(e) => setStatusComment(e.target.value)} />
                </Field>
                <Button type="submit" disabled={!statusTarget} style={{ width: "100%" }}>
                  Update status
                </Button>
              </form>
            </Card>
          )}
        </div>
      </div>

      <Modal open={editing} title="Edit ticket" onClose={() => setEditing(false)}>
        <form onSubmit={submitEdit}>
          <Field label="Title" required>
            <Input value={editTitle} onChange={(e) => setEditTitle(e.target.value)} maxLength={200} required />
          </Field>
          <Field label="Description">
            <Textarea value={editDescription} onChange={(e) => setEditDescription(e.target.value)} maxLength={4000} />
          </Field>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>
            <Field label="Priority" required>
              <Select value={editPriority} onChange={(e) => setEditPriority(e.target.value as TicketPriority)}>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </Select>
            </Field>
            <Field label="Category" required>
              <Select value={editCategoryId} onChange={(e) => setEditCategoryId(e.target.value)}>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </Select>
            </Field>
          </div>
          <div className="form-actions">
            <Button type="button" variant="secondary" onClick={() => setEditing(false)}>
              Cancel
            </Button>
            <Button type="submit">Save changes</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
