import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { dashboardApi } from "../api";
import { errorMessage } from "../api/client";
import type { DashboardSummary } from "../api/types";
import {
  Card,
  EmptyState,
  PageHeader,
  Spinner,
  StatusBadge,
} from "../components/ui";
import { formatDateTime } from "../lib/format";

const STATUS_COLORS: Record<string, string> = {
  OPEN: "#2563eb",
  IN_PROGRESS: "#7c3aed",
  RESOLVED: "#16a34a",
  CLOSED: "#64748b",
};

const PRIORITY_COLORS: Record<string, string> = {
  LOW: "#22c55e",
  MEDIUM: "#f59e0b",
  HIGH: "#ef4444",
  URGENT: "#7f1d1d",
};

const DEFAULT_COLOR = "#94a3b8";

function formatLabel(value: string): string {
  const spaced = value.replace("_", " ").toLowerCase();
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

export function Dashboard() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    dashboardApi
      .summary()
      .then(setSummary)
      .catch((err) => setError(errorMessage(err)));
  }, []);

  if (error) return <div className="form-error">{error}</div>;
  if (!summary) return <Spinner label="Loading dashboard…" />;

  const statusData = summary.ticketsByStatus.map((s) => ({
    name: formatLabel(s.status),
    count: s.count,
  }));
  const priorityData = summary.ticketsByPriority.map((p) => ({
    name: formatLabel(p.priority),
    count: p.count,
  }));
  const categoryData = summary.ticketsByCategory.map((c) => ({
    name: c.name,
    count: c.count,
  }));
  const hasStatusData = statusData.some((d) => d.count > 0);
  const hasPriorityData = priorityData.some((d) => d.count > 0);
  const hasCategoryData = categoryData.some((d) => d.count > 0);

  return (
    <div>
      <PageHeader title="Dashboard" subtitle="Overview of your helpdesk activity" />

      <div className="stat-grid">
        <Card className="stat-card">
          <div className="stat-label">Total tickets</div>
          <div className="stat-value">{summary.totalTickets}</div>
          <div className="stat-sub">All time</div>
        </Card>
        <Card className="stat-card">
          <div className="stat-label">Open</div>
          <div className="stat-value">{summary.openTickets}</div>
          <div className="stat-sub">Awaiting action</div>
        </Card>
        <Card className="stat-card">
          <div className="stat-label">In progress</div>
          <div className="stat-value">{summary.inProgressTickets}</div>
          <div className="stat-sub">Being worked on</div>
        </Card>
        <Card className="stat-card">
          <div className="stat-label">Resolved</div>
          <div className="stat-value">{summary.resolvedTickets}</div>
          <div className="stat-sub">Solved</div>
        </Card>
        <Card className="stat-card">
          <div className="stat-label">SLA breached</div>
          <div className="stat-value" style={{ color: summary.slaBreachedTickets ? "var(--danger)" : undefined }}>
            {summary.slaBreachedTickets}
          </div>
          <div className="stat-sub">Past deadline</div>
        </Card>
        <Card className="stat-card">
          <div className="stat-label">Unassigned</div>
          <div className="stat-value">{summary.unassignedTickets}</div>
          <div className="stat-sub">Needs an agent</div>
        </Card>
        <Card className="stat-card">
          <div className="stat-label">Avg resolution</div>
          <div className="stat-value">{summary.avgResolutionHours?.toFixed(1) ?? "–"}h</div>
          <div className="stat-sub">Per resolved ticket</div>
        </Card>
      </div>

      <div className="dashboard-grid">
        <Card className="chart-card">
          <h3>By status</h3>
          {!hasStatusData && <EmptyState title="No data yet" />}
          {hasStatusData && (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie
                  data={statusData}
                  dataKey="count"
                  nameKey="name"
                  innerRadius={55}
                  outerRadius={85}
                  paddingAngle={2}
                  stroke="none"
                >
                  {statusData.map((d) => (
                    <Cell key={d.name} fill={STATUS_COLORS[d.name.toUpperCase().replace(" ", "_")] ?? DEFAULT_COLOR} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          )}
        </Card>

        <Card className="chart-card">
          <h3>By priority</h3>
          {!hasPriorityData && <EmptyState title="No data yet" />}
          {hasPriorityData && (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={priorityData} margin={{ top: 5, right: 5, left: -24, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="name" tickLine={false} axisLine={false} />
                <YAxis allowDecimals={false} tickLine={false} axisLine={false} />
                <Tooltip cursor={{ fill: "rgba(148, 163, 184, 0.15)" }} />
                <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                  {priorityData.map((d) => (
                    <Cell key={d.name} fill={PRIORITY_COLORS[d.name.toUpperCase()] ?? DEFAULT_COLOR} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </Card>

        <Card className="chart-card">
          <h3>By category</h3>
          {!hasCategoryData && <EmptyState title="No data yet" />}
          {hasCategoryData && (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart
                data={categoryData}
                layout="vertical"
                margin={{ top: 5, right: 16, left: 0, bottom: 0 }}
              >
                <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                <XAxis type="number" allowDecimals={false} tickLine={false} axisLine={false} />
                <YAxis type="category" dataKey="name" width={110} tickLine={false} axisLine={false} />
                <Tooltip cursor={{ fill: "rgba(148, 163, 184, 0.15)" }} />
                <Bar dataKey="count" radius={[0, 6, 6, 0]} fill="#2563eb" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </Card>

        <Card className="chart-card">
          <h3>Recent tickets</h3>
          {summary.recentTickets.length === 0 && <EmptyState title="No tickets yet" />}
          {summary.recentTickets.map((t) => (
            <div className="bar-row" key={t.id} style={{ alignItems: "flex-start" }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <Link className="ticket-title" to={`/tickets/${t.id}`}>
                  {t.ticketNumber}
                </Link>
                <div className="muted" style={{ whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                  {t.title}
                </div>
                <div style={{ marginTop: 4 }}>
                  <StatusBadge status={t.status} />
                </div>
              </div>
              <span className="muted" style={{ fontSize: 12, flexShrink: 0 }}>
                {formatDateTime(t.createdAt)}
              </span>
            </div>
          ))}
        </Card>
      </div>
    </div>
  );
}
