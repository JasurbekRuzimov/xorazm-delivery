import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = process.env.REACT_APP_WS_URL || 'http://localhost:8080/ws';

/**
 * WebSocket orqali kuryer joylashuvini real-vaqtda kuzatish.
 * Backend TrackingService har 15 soniyada /topic/tracking/{orderId} ga yuboradi.
 */
export function useTracking(orderId) {
  const [location, setLocation] = useState(null);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef(null);

  useEffect(() => {
    if (!orderId) return;

    const token = localStorage.getItem('accessToken');

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/tracking/${orderId}`, (message) => {
          try {
            const data = JSON.parse(message.body);
            setLocation(data);
          } catch {}
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      setConnected(false);
    };
  }, [orderId]);

  return { location, connected };
}

/**
 * Kuryer o'z joylashuvini backendga yuborish uchun hook.
 * Har 30 soniyada GPS koordinatini POST /v1/tracking/location ga yuboradi.
 */
export function useCourierLocation() {
  const [active, setActive] = useState(false);
  const intervalRef = useRef(null);

  const start = () => {
    if (!navigator.geolocation) return;
    setActive(true);

    const send = () => {
      navigator.geolocation.getCurrentPosition(
        ({ coords }) => {
          import('../api/client').then(({ trackingApi }) => {
            trackingApi.updateLocation(coords.latitude, coords.longitude);
          });
        },
        (err) => console.warn('Geolocation error:', err)
      );
    };

    send(); // Darhol yuborish
    intervalRef.current = setInterval(send, 30_000);
  };

  const stop = () => {
    setActive(false);
    if (intervalRef.current) clearInterval(intervalRef.current);
  };

  useEffect(() => () => stop(), []);

  return { active, start, stop };
}
