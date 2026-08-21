import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { categoryApi, ticketApi, userApi } from "../api";
import { errorMessage } from "../api/client";
import type { Category, Page, Ticket, TicketPriority, TicketStatus, UserOption } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import {
  Button,
  Card,
  EmptyState,
  Input,
  PageHeader,
  Pagination,
  PriorityBadge,
  Select,
  SlaBadge,
  Spinner,
  StatusBadge,
} from "../components/ui";
import { formatDateTime, fullName } from "../lib/format";

const STATUSES: Array<TicketStatus | ""> = ["", "OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"];
const PRIORITIES: Array<TicketPriority | ""> = ["", "LOW", "MEDIUM", "HIGH", "CRITICAL"];

export function TicketList() {
  const { user, isAgent } = useAuth();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<TicketStatus | "">("");
  const [priority, setPriority] = useState<TicketPriority | "">("");
  const [categoryId, setCategoryId] = useState("");
  const [assignedToId, setAssignedToId] = useState("");
  const [slaBreached, setSlaBreached] = useState<"" | "true" | "false">("");
  const [data, setData] = useState<Page<Ticket> | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [agents, setAgents] = useState<UserOption[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    categoryApi.list(true).then(setCategories).catch(() => {});
    if (isAgent) {
      userApi.agents().then(setAgents).catch(() => {});
    }
  }, [isAgent]);

  const buildQuery = () => ({
    search: search || undefined,
    status: status || undefined,
    priority: priority || undefined,
    categoryId: categoryId || undefined,
    assignedToId: assignedToId || undefined,
    slaBreached: slaBreached === "" ? undefined : slaBreached === "true",
  });

  useEffect(() => {
    setLoading(true);
    ticketApi
      .list({ ...buildQuery(), page, size: 10, sortBy: "createdAt", direction: "desc" })
      .then(setData)
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, [page, search, status, priority, categoryId, assignedToId, slaBreached]);

  const resetPage = () => setPage(0);

  const exportCsv = async () => {
    setError("");
    setExporting(true);
    try {
      const blob = await ticketApi.exportCsv(buildQuery());
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `tickets-${new Date().toISOString().slice(0, 19).replace("T", "-").replaceAll(":", "")}.csv`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setExporting(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Tickets"
        subtitle="Search and manage support tickets"
        actions={
          <>
            <Button variant="secondary" onClick={exportCsv} disabled={exporting}>
              {exporting ? "Exporting…" : "Export CSV"}
            </Button>
            <Link className="btn btn-primary btn-md" to="/tickets/new">
              + New ticket
            </Link>
          </>
        }
      />

      <div className="filters">
        <Input
          className="search"
          placeholder="Search title, number or description…"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            resetPage();
          }}
        />
        <Select
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as TicketStatus | "");
            resetPage();
          }}
        >
          <option value="">All statuses</option>
          {STATUSES.filter(Boolean).map((s) => (
            <option key={s} value={s}>
              {s.replace("_", " ")}
            </option>
          ))}
        </Select>
        <Select
          value={priority}
          onChange={(e) => {
            setPriority(e.target.value as TicketPriority | "");
            resetPage();
          }}
        >
          <option value="">All priorities</option>
          {PRIORITIES.filter(Boolean).map((p) => (
            <option key={p} value={p}>
              {p}
            </option>
          ))}
        </Select>
        <Select
          value={categoryId}
          onChange={(e) => {
            setCategoryId(e.target.value);
            resetPage();
          }}
        >
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </Select>
        {isAgent && (
          <Select
            value={assignedToId}
            onChange={(e) => {
              setAssignedToId(e.target.value);
              resetPage();
            }}
          >
            <option value="">Any assignee</option>
            <option value={user?.id ?? ""}>Assigned to me</option>
            {agents
              .filter((a) => a.id !== user?.id)
              .map((a) => (
                <option key={a.id} value={a.id}>
                  {fullName(a.firstName, a.lastName)}
                </option>
              ))}
          </Select>
        )}
        <Select
          value={slaBreached}
          onChange={(e) => {
            setSlaBreached(e.target.value as "" | "true" | "false");
            resetPage();
          }}
        >
          <option value="">SLA: any</option>
          <option value="true">SLA breached</option>
          <option value="false">SLA ok</option>
        </Select>
      </div>

      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <Spinner label="Loading tickets…" />
      ) : (
        <Card>
          {data && data.content.length === 0 ? (
            <EmptyState title="No tickets found" hint="Try adjusting your filters or create a new ticket." />
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Number</th>
                    <th>Title</th>
                    <th>Status</th>
                    <th>Priority</th>
                    <th>Category</th>
                    <th>Assignee</th>
                    <th>SLA</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {data?.content.map((t) => (
                    <tr key={t.id}>
                      <td className="mono">{t.ticketNumber}</td>
                      <td>
                        <Link className="ticket-title" to={`/tickets/${t.id}`}>
                          {t.title}
                        </Link>
                      </td>
                      <td>
                        <StatusBadge status={t.status} />
                      </td>
                      <td>
                        <PriorityBadge priority={t.priority} />
                      </td>
                      <td>{t.categoryName}</td>
                      <td>{t.assignedToId ? fullName(t.assignedToFirstName, t.assignedToLastName) : "—"}</td>
                      <td>
                        <SlaBadge breached={t.slaBreached} remaining={t.slaTimeRemaining} />
                      </td>
                      <td className="muted">{formatDateTime(t.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {data && (
            <div style={{ padding: "0 16px" }}>
              <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />
            </div>
          )}
        </Card>
      )}
    </div>
  );
}
