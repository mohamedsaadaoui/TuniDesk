import { useEffect, useRef, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { notificationApi } from "../api";
import type { Notification } from "../api/types";
import { useAuth } from "../auth/AuthContext";
import { useNotifications } from "../hooks/useNotifications";
import { timeAgo } from "../lib/format";
import { Avatar } from "./ui";

interface NavItem {
  to: string;
  label: string;
  icon: string;
  adminOnly?: boolean;
}

const NAV_ITEMS: NavItem[] = [
  { to: "/", label: "Dashboard", icon: "M3 12l9-9 9 9M5 10v10h5v-6h4v6h5V10" },
  { to: "/tickets", label: "Tickets", icon: "M20 6H4v12h16V6zM4 6l8 5 8-5" },
  { to: "/tickets/new", label: "New ticket", icon: "M12 5v14M5 12h14" },
  { to: "/categories", label: "Categories", icon: "M4 7h16M4 12h16M4 17h10", adminOnly: true },
  { to: "/sla", label: "SLA policies", icon: "M12 3v10l6 4M12 3a9 9 0 100 18 9 9 0 000-18", adminOnly: true },
  { to: "/users", label: "Users", icon: "M16 7a4 4 0 11-8 0 4 4 0 018 0zM4 21v-1a6 6 0 0112 0v1", adminOnly: true },
  { to: "/audit-logs", label: "Audit logs", icon: "M9 12h6M9 16h6M9 8h6M5 3h14a1 1 0 011 1v17l-4-3-4 3-4-3-4 3V4a1 1 0 011-1z", adminOnly: true },
];

export function Layout() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const { unread, refreshUnread } = useNotifications(user?.id);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [open, setOpen] = useState(false);
  const bellRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (open) {
      notificationApi.list(false, 0, 8).then((page) => setNotifications(page.content)).catch(() => {});
    }
  }, [open, unread]);

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (bellRef.current && !bellRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  const markAllRead = async () => {
    await notificationApi.markAllRead();
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    refreshUnread();
  };

  const markRead = async (n: Notification) => {
    if (n.read) return;
    await notificationApi.markRead(n.id);
    refreshUnread();
  };

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-logo">TD</span>
          <span className="brand-name">TuniDesk</span>
        </div>
        <nav className="sidebar-nav">
          {NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin).map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/"}
              className={({ isActive }) => `nav-link${isActive ? " active" : ""}`}
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d={item.icon} />
              </svg>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <button className="nav-link" onClick={logout}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9" />
            </svg>
            <span>Sign out</span>
          </button>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <div className="topbar-title">HelpDesk</div>
          <div className="topbar-actions">
            <div className="bell" ref={bellRef}>
              <button
                className="bell-btn"
                onClick={() => setOpen((o) => !o)}
                aria-label="Notifications"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M18 8a6 6 0 10-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 01-3.4 0" />
                </svg>
                {unread > 0 && <span className="bell-badge">{unread > 9 ? "9+" : unread}</span>}
              </button>
              {open && (
                <div className="bell-dropdown">
                  <div className="bell-header">
                    <strong>Notifications</strong>
                    <button className="btn btn-ghost btn-sm" onClick={markAllRead}>
                      Mark all read
                    </button>
                  </div>
                  <div className="bell-list">
                    {notifications.length === 0 && (
                      <div className="bell-empty">No notifications yet</div>
                    )}
                    {notifications.map((n) => (
                      <button
                        key={n.id}
                        className={`bell-item${n.read ? "" : " unread"}`}
                        onClick={() => markRead(n)}
                      >
                        <div className="bell-item-title">{n.title}</div>
                        <div className="bell-item-msg">{n.message}</div>
                        <div className="bell-item-time">{timeAgo(n.createdAt)}</div>
                      </button>
                    ))}
                  </div>
                  <div className="bell-footer">
                    <button
                      className="btn btn-ghost btn-sm"
                      onClick={() => {
                        setOpen(false);
                        navigate("/tickets");
                      }}
                    >
                      Go to tickets
                    </button>
                  </div>
                </div>
              )}
            </div>
            <div className="topbar-user">
              <Avatar firstName={user?.firstName} lastName={user?.lastName} />
              <div className="topbar-user-meta">
                <span className="topbar-user-name">
                  {user ? `${user.firstName} ${user.lastName}` : "…"}
                </span>
                <span className="topbar-user-role">{user?.roles.join(" · ")}</span>
              </div>
            </div>
          </div>
        </header>

        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
