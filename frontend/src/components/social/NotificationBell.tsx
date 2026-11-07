import { useState, useEffect, useRef } from "react";
import { Link } from "react-router-dom";
import { socialService } from "../../services/socialService";
import { NotificationDto } from "../../types/social";

export function NotificationBell() {
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [notifications, setNotifications] = useState<NotificationDto[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    loadUnread();
    const interval = setInterval(loadUnread, 15000); // Poll every 15s
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  async function loadUnread() {
    try {
      const count = await socialService.getUnreadNotificationCount();
      setUnreadCount(count);
    } catch {
      // Ignored if offline
    }
  }

  async function toggleDropdown() {
    if (!isOpen) {
      try {
        const list = await socialService.getNotifications();
        setNotifications(list);
      } catch (e) {
        console.error(e);
      }
    }
    setIsOpen(!isOpen);
  }

  async function handleMarkRead(id: number) {
    try {
      await socialService.markNotificationRead(id);
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
      setUnreadCount((c) => Math.max(0, c - 1));
    } catch (e) {
      console.error(e);
    }
  }

  async function handleMarkAllRead() {
    try {
      await socialService.markAllNotificationsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (e) {
      console.error(e);
    }
  }

  return (
    <div style={{ position: "relative" }} ref={dropdownRef}>
      <button
        onClick={toggleDropdown}
        style={{
          background: "rgba(15, 23, 42, 0.8)",
          border: "1px solid #1e293b",
          borderRadius: 8,
          color: unreadCount > 0 ? "#00f2fe" : "#94a3b8",
          padding: "6px 12px",
          display: "flex",
          alignItems: "center",
          gap: 6,
          cursor: "pointer",
          fontSize: 14,
        }}
        title="Notifications"
      >
        <span>🔔</span>
        {unreadCount > 0 && (
          <span
            style={{
              background: "#ef4444",
              color: "#fff",
              fontSize: 10,
              fontWeight: 800,
              padding: "2px 6px",
              borderRadius: 10,
              lineHeight: 1,
            }}
          >
            {unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div
          style={{
            position: "absolute",
            top: 42,
            right: 0,
            width: 320,
            maxHeight: 400,
            overflowY: "auto",
            background: "#080d1a",
            border: "1px solid #1e293b",
            borderRadius: 12,
            boxShadow: "0 10px 30px rgba(0,0,0,0.6)",
            zIndex: 1000,
            padding: 12,
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: 10,
              borderBottom: "1px solid #1e293b",
              paddingBottom: 8,
            }}
          >
            <span style={{ fontSize: 13, fontWeight: 700, color: "#f8fafc" }}>
              Notifications
            </span>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                style={{
                  background: "transparent",
                  border: "none",
                  color: "#00f2fe",
                  fontSize: 11,
                  cursor: "pointer",
                  fontWeight: 600,
                }}
              >
                Mark all read
              </button>
            )}
          </div>

          {notifications.length === 0 ? (
            <div style={{ color: "#64748b", fontSize: 12, textAlign: "center", padding: "20px 0" }}>
              No notifications yet.
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {notifications.map((n) => (
                <div
                  key={n.id}
                  style={{
                    background: n.read ? "#0f172a" : "rgba(0, 242, 254, 0.08)",
                    border: `1px solid ${n.read ? "#1e293b" : "rgba(0, 242, 254, 0.3)"}`,
                    borderRadius: 8,
                    padding: "8px 10px",
                    display: "flex",
                    flexDirection: "column",
                    gap: 4,
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                    <span style={{ fontSize: 12, fontWeight: 700, color: n.read ? "#cbd5e1" : "#00f2fe" }}>
                      {n.title}
                    </span>
                    {!n.read && (
                      <button
                        onClick={() => handleMarkRead(n.id)}
                        style={{
                          background: "transparent",
                          border: "none",
                          color: "#64748b",
                          fontSize: 10,
                          cursor: "pointer",
                        }}
                        title="Mark read"
                      >
                        ✓
                      </button>
                    )}
                  </div>
                  <p style={{ margin: 0, fontSize: 11, color: "#94a3b8" }}>{n.message}</p>
                  {n.link && (
                    <Link
                      to={n.link}
                      onClick={() => {
                        handleMarkRead(n.id);
                        setIsOpen(false);
                      }}
                      style={{ fontSize: 10, color: "#00ff87", textDecoration: "none", marginTop: 2, fontWeight: 600 }}
                    >
                      View details →
                    </Link>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
