import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
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

const BAR_COLORS = ["#2563eb", "#7c3aed", "#0ea5e9", "#16a34a", "#f59e0b", "#ef4444", "#64748b"];

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

  const maxStatus = Math.max(1, ...summary.ticketsByStatus.map((s) => s.count));
  const maxPriority = Math.max(1, ...summary.ticketsByPriority.map((p) => p.count));
  const maxCategory = Math.max(1, ...summary.ticketsByCategory.map((c) => c.count));

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
          {summary.ticketsByStatus.map((s) => (
            <div className="bar-row" key={s.status}>
              <span className="bar-label">{s.status.replace("_", " ")}</span>
              <div className="bar-track">
                <div
                  className="bar-fill"
                  style={{
                    width: `${(s.count / maxStatus) * 100}%`,
                    background: BAR_COLORS[0],
                  }}
                />
              </div>
              <span className="bar-count">{s.count}</span>
            </div>
          ))}
        </Card>

        <Card className="chart-card">
          <h3>By priority</h3>
          {summary.ticketsByPriority.map((p) => (
            <div className="bar-row" key={p.priority}>
              <span className="bar-label">{p.priority}</span>
              <div className="bar-track">
                <div
                  className="bar-fill"
                  style={{
                    width: `${(p.count / maxPriority) * 100}%`,
                    background: BAR_COLORS[2],
                  }}
                />
              </div>
              <span className="bar-count">{p.count}</span>
            </div>
          ))}
        </Card>

        <Card className="chart-card">
          <h3>By category</h3>
          {summary.ticketsByCategory.length === 0 && <EmptyState title="No data yet" />}
          {summary.ticketsByCategory.map((c, i) => (
            <div className="bar-row" key={c.name}>
              <span className="bar-label">{c.name}</span>
              <div className="bar-track">
                <div
                  className="bar-fill"
                  style={{
                    width: `${(c.count / maxCategory) * 100}%`,
                    background: BAR_COLORS[i % BAR_COLORS.length],
                  }}
                />
              </div>
              <span className="bar-count">{c.count}</span>
            </div>
          ))}
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
