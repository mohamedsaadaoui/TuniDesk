import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useEffect, useRef, useState } from "react";
import { notificationApi } from "../api";
import type { Notification } from "../api/types";

export function useNotifications(userId?: string) {
  const [unread, setUnread] = useState(0);
  const [latest, setLatest] = useState<Notification | null>(null);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  const refreshUnread = () => {
    notificationApi.unreadCount().then(setUnread).catch(() => {});
  };

  useEffect(() => {
    if (!userId) return;

    notificationApi.unreadCount().then(setUnread).catch(() => {});

    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/user/${userId}/queue/notifications`, (message: IMessage) => {
          const notification = JSON.parse(message.body) as Notification;
          setLatest(notification);
          setUnread((n) => n + 1);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [userId]);

  return { unread, latest, connected, refreshUnread };
}
