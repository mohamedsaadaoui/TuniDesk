import { useEffect, useState } from "react";
import { auditApi } from "../api";
import { errorMessage } from "../api/client";
import type { Page, AuditLog } from "../api/types";
import {
  Card,
  EmptyState,
  Input,
  PageHeader,
  Pagination,
  Spinner,
} from "../components/ui";
import { formatDateTime } from "../lib/format";

const ACTION_COLORS: Record<string, string> = {
  CREATE: "badge-resolved",
  UPDATE: "badge-open",
  DELETE: "badge-critical",
  LOGIN: "badge-assigned",
  REGISTER: "badge-assigned",
};

export function AuditLogs() {
  const [data, setData] = useState<Page<AuditLog> | null>(null);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    auditApi
      .list({ search: search || undefined, page, size: 20 })
      .then(setData)
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, [page, search]);

  return (
    <div>
      <PageHeader title="Audit logs" subtitle="Record of actions performed in the system" />

      <div className="filters">
        <Input
          className="search"
          placeholder="Search by actor, action or details…"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
        />
      </div>

      {error && <div className="form-error">{error}</div>}

      {loading ? (
        <Spinner label="Loading audit logs…" />
      ) : (
        <Card>
          {data && data.content.length === 0 ? (
            <EmptyState title="No audit logs found" />
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Action</th>
                    <th>Entity</th>
                    <th>Details</th>
                    <th>Actor</th>
                    <th>Time</th>
                  </tr>
                </thead>
                <tbody>
                  {data?.content.map((log) => (
                    <tr key={log.id}>
                      <td>
                        <span className={`badge ${ACTION_COLORS[log.action] ?? "badge-closed"}`}>
                          {log.action}
                        </span>
                      </td>
                      <td style={{ fontWeight: 600 }}>{log.entityType}</td>
                      <td className="muted">{log.details || "—"}</td>
                      <td>{log.performedBy || "—"}</td>
                      <td className="muted">{formatDateTime(log.createdAt)}</td>
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
