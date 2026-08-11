import { useCallback, useEffect, useState } from "react";
import { notificationApi } from "../api";
import { errorMessage } from "../api/client";
import type { Notification, Page } from "../api/types";
import { Button, Card, EmptyState, PageHeader, Pagination, Spinner } from "../components/ui";
import { useNotifications } from "../hooks/useNotifications";
import { useAuth } from "../auth/AuthContext";
import { timeAgo } from "../lib/format";

type Filter = "ALL" | "UNREAD";

export function Notifications() {
  const { user } = useAuth();
  const { refreshUnread } = useNotifications(user?.id);

  const [data, setData] = useState<Page<Notification> | null>(null);
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState<Filter>("ALL");
  const [error, setError] = useState("");

  const load = useCallback(() => {
    setError("");
    notificationApi
      .list(filter === "UNREAD", page, 10)
      .then(setData)
      .catch((err) => setError(errorMessage(err)));
  }, [filter, page]);

  useEffect(() => {
    load();
  }, [load]);

  const markAllRead = async () => {
    setError("");
    try {
      await notificationApi.markAllRead();
      refreshUnread();
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const markRead = async (n: Notification) => {
    if (n.read) return;
    setError("");
    try {
      await notificationApi.markRead(n.id);
      refreshUnread();
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const remove = async (id: string) => {
    setError("");
    try {
      await notificationApi.remove(id);
      refreshUnread();
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <div>
      <PageHeader
        title="Notifications"
        subtitle="Everything related to your tickets"
        actions={
          <>
            <Button variant="ghost" size="sm" onClick={() => setFilter(filter === "ALL" ? "UNREAD" : "ALL")}>
              {filter === "ALL" ? "Show unread" : "Show all"}
            </Button>
            <Button variant="secondary" size="sm" onClick={markAllRead}>
              Mark all read
            </Button>
          </>
        }
      />

      {error && <div className="form-error">{error}</div>}

      {!data ? (
        <Spinner label="Loading notifications…" />
      ) : data.content.length === 0 ? (
        <EmptyState
          title={filter === "UNREAD" ? "No unread notifications" : "No notifications"}
          hint={filter === "UNREAD" ? "You are all caught up." : "Notifications will appear here."}
        />
      ) : (
        <>
          <Card style={{ padding: 8 }}>
            {data.content.map((n) => (
              <div key={n.id} className="notif-row">
                <div
                  className={`notif-row-main${n.read ? "" : " unread"}`}
                  role="button"
                  tabIndex={0}
                  onClick={() => markRead(n)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      e.preventDefault();
                      markRead(n);
                    }
                  }}
                >
                  <div className="notif-row-title">
                    {n.title}
                    {!n.read && <span className="notif-row-dot" />}
                  </div>
                  <div className="notif-row-msg">{n.message}</div>
                  <div className="notif-row-time">
                    {timeAgo(n.createdAt)}
                  </div>
                </div>
                <button
                  className="btn btn-ghost btn-sm"
                  onClick={() => remove(n.id)}
                  title="Delete notification"
                >
                  Delete
                </button>
              </div>
            ))}
          </Card>
          <Pagination page={data.number} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}
