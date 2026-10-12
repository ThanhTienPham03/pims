import type { AuditLogItem } from '../types';

type MessageListener = (data: AuditLogItem) => void;
type StatusListener = (connected: boolean) => void;

// Quản lý kết nối WebSocket realtime audit log cho ADMIN
class ActivitySocketService {
  private socket: WebSocket | null = null;
  private messageListeners: Set<MessageListener> = new Set();
  private statusListeners: Set<StatusListener> = new Set();
  private reconnectTimeout: number | null = null;
  private shouldReconnect = false;

  private getSocketUrl(): string {
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8085/api';
    if (!apiUrl.startsWith('http')) {
      const wsProto = window.location.protocol === 'https:' ? 'wss' : 'ws';
      return `${wsProto}://${window.location.host}/ws/activity`;
    }
    const base = apiUrl.replace(/\/api\/?$/, '');
    const wsProto = base.startsWith('https') ? 'wss' : 'ws';
    const host = base.replace(/^https?:\/\//, '');
    return `${wsProto}://${host}/ws/activity`;
  }

  public connect() {
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return;
    }

    this.shouldReconnect = true;
    const url = this.getSocketUrl();

    try {
      this.socket = new WebSocket(url);

      this.socket.onopen = () => {
        this.notifyStatus(true);
      };

      this.socket.onmessage = (event) => {
        try {
          const data: AuditLogItem = JSON.parse(event.data);
          this.messageListeners.forEach((listener) => {
            try {
              listener(data);
            } catch (err) {
              console.error('Error in websocket listener:', err);
            }
          });
        } catch (err) {
          console.error('Failed to parse websocket message:', err);
        }
      };

      this.socket.onclose = () => {
        this.notifyStatus(false);
        if (this.shouldReconnect) {
          this.scheduleReconnect();
        }
      };

      this.socket.onerror = () => {
        this.socket?.close();
      };
    } catch {
      this.notifyStatus(false);
      if (this.shouldReconnect) {
        this.scheduleReconnect();
      }
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }
    this.reconnectTimeout = window.setTimeout(() => {
      if (this.shouldReconnect) {
        this.connect();
      }
    }, 4000);
  }

  public disconnect() {
    this.shouldReconnect = false;
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.notifyStatus(false);
  }

  public subscribe(listener: MessageListener): () => void {
    this.messageListeners.add(listener);
    return () => {
      this.messageListeners.delete(listener);
    };
  }

  public onStatusChange(listener: StatusListener): () => void {
    this.statusListeners.add(listener);
    listener(this.isConnected());
    return () => {
      this.statusListeners.delete(listener);
    };
  }

  public isConnected(): boolean {
    return this.socket !== null && this.socket.readyState === WebSocket.OPEN;
  }

  private notifyStatus(connected: boolean) {
    this.statusListeners.forEach((listener) => listener(connected));
  }
}

export const activitySocket = new ActivitySocketService();
