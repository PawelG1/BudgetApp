import { useEffect, useRef } from "react";
import { toast } from "react-toastify";
import { useAuth } from "../../context/AuthContext";
import { useBalance } from "../BalanceBar/useBalance";
import { useGroupNotificationsCtx } from "../../context/GroupNotificationsContext";

export type GroupNotificationType =
  | "GROUP_EXPENSE_ADDED"
  | "DEBT_MARKED_AS_PAID"
  | "DEBT_PAYMENT_CONFIRMED";

export interface GroupNotification {
  type: GroupNotificationType;
  groupId: number | string;
  groupName: string;
  title: string;
  amount: number;
  message: string;
  userShare?: number;
  createdByEmail?: string;
}

const RECONNECT_DELAY_MS = 3000;

const getWebSocketUrl = (token: string) => {
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://localhost:8080/ws/group-notifications?token=${encodeURIComponent(token)}`;
};

const GroupNotificationsListener = () => {
  const { isAuthenticated } = useAuth();
  const { refreshBalance } = useBalance();
  const { bumpDataVersion } = useGroupNotificationsCtx();

  // Stabilne refy — WS handler nie potrzebuje ponownego połączenia przy zmianie callbacków
  const refreshBalanceRef = useRef(refreshBalance);
  const bumpDataVersionRef = useRef(bumpDataVersion);

  useEffect(() => {
    refreshBalanceRef.current = refreshBalance;
  }, [refreshBalance]);

  useEffect(() => {
    bumpDataVersionRef.current = bumpDataVersion;
  }, [bumpDataVersion]);

  useEffect(() => {
    if (!isAuthenticated) return;

    const token = localStorage.getItem("accessToken");
    if (!token) return;

    let destroyed = false;
    let socket: WebSocket | null = null;
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

    const connect = () => {
      if (destroyed) return;

      socket = new WebSocket(getWebSocketUrl(token));

      socket.onmessage = (event) => {
        if (destroyed) return;
        try {
          const notification = JSON.parse(event.data) as GroupNotification;
          toast.info(notification.message);
          refreshBalanceRef.current(null);
          // Aktualizacja kontekstu — React gwarantuje propagację do wszystkich subskrybentów
          bumpDataVersionRef.current();
        } catch (error) {
          console.error("Nie udało się obsłużyć komunikatu grupowego:", error);
        }
      };

      socket.onerror = () => {};

      socket.onclose = () => {
        socket = null;
        if (!destroyed) {
          reconnectTimer = setTimeout(connect, RECONNECT_DELAY_MS);
        }
      };
    };

    connect();

    return () => {
      destroyed = true;
      if (reconnectTimer !== null) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
      }
      if (socket !== null) {
        socket.close();
        socket = null;
      }
    };
  }, [isAuthenticated]);

  return null;
};

export default GroupNotificationsListener;
