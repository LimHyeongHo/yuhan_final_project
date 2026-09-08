// [신규 파일][feature/chat-fixes] 채팅 안읽음 배지 전역 공유 Context
// Header/SellerDashboardPage의 20초 폴링을 개인 토픽(/topic/chat/user/{email}) WebSocket 구독으로 대체.
// 이벤트가 오면 서버 목록 재조회로 배지·미리보기 갱신, 새 메시지는 우하단 토스트.
import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { X } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const API_BASE = `http://${window.location.hostname}:8080/api`;
const WS_URL = `http://${window.location.hostname}:8080/ws`;

const ChatNotificationContext = createContext(null);

const ChatToast = ({ toast, onClose }) => {
  const navigate = useNavigate();
  if (!toast) return null;
  const role = localStorage.getItem('user_role');
  const chatPath = role === 'ROLE_SELLER' ? '/seller/chat' : '/buyer/chat';
  return (
    <div
      onClick={() => { navigate(`${chatPath}?roomId=${toast.roomId}`); onClose(); }}
      className="fixed bottom-5 right-5 z-[100] w-72 bg-white border border-gray-200 shadow-xl rounded-2xl p-4 cursor-pointer hover:shadow-2xl transition"
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex flex-col gap-0.5 overflow-hidden">
          <span className="text-xs font-black text-blue-600">새 채팅 메시지</span>
          <span className="text-sm font-bold text-gray-900 truncate">{toast.title}</span>
          <span className="text-xs text-gray-500 truncate">{toast.preview}</span>
        </div>
        <button
          onClick={(e) => { e.stopPropagation(); onClose(); }}
          className="text-gray-300 hover:text-gray-500 shrink-0"
        >
          <X size={16} />
        </button>
      </div>
    </div>
  );
};

export const ChatNotificationProvider = ({ children }) => {
  const [chatRooms, setChatRooms] = useState([]);
  const [toast, setToast] = useState(null);

  const reloadTimerRef = useRef(null);
  const toastTimerRef = useRef(null);
  const clientRef = useRef(null);
  // 방금 읽은 방 id → 서버 재조회가 아직 커밋 전 상태를 되돌리지 않도록 잠깐 0으로 강제
  const recentlyReadRef = useRef(new Map());
  // 채팅 화면에서 지금 열어 둔 방 id (그 방 메시지는 토스트 생략)
  const activeRoomIdRef = useRef(null);

  const email = localStorage.getItem('email');

  const reloadRooms = useCallback(async () => {
    if (!localStorage.getItem('email')) { setChatRooms([]); return; }
    try {
      const res = await fetch(`${API_BASE}/chat/rooms`, { credentials: 'include' });
      if (!res.ok) return;
      const data = await res.json();
      // 최근 3초 내에 읽음 처리한 방은 서버 응답이 뒤늦더라도 0으로 유지
      const now = Date.now();
      for (const [rid, ts] of recentlyReadRef.current) {
        if (now - ts > 3000) recentlyReadRef.current.delete(rid);
      }
      setChatRooms(data.map(r =>
        recentlyReadRef.current.has(r.roomId) ? { ...r, unreadCount: 0 } : r
      ));
    } catch (e) {
      console.error('전역 채팅 알림 목록 로드 실패', e);
    }
  }, []);

  // 이벤트가 몰려와도 목록 재조회는 300ms 디바운스
  const scheduleReload = useCallback(() => {
    clearTimeout(reloadTimerRef.current);
    reloadTimerRef.current = setTimeout(reloadRooms, 300);
  }, [reloadRooms]);

  const showToast = useCallback((payload) => {
    setToast(payload);
    clearTimeout(toastTimerRef.current);
    toastTimerRef.current = setTimeout(() => setToast(null), 4000);
  }, []);

  // ── WebSocket 연결 + 개인 토픽 구독 ──────────────────────────
  useEffect(() => {
    if (!email) { setChatRooms([]); return; }

    reloadRooms();

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: { 'X-User-Email': email },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/chat/user/${email}`, (frame) => {
          let msg;
          try { msg = JSON.parse(frame.body); } catch (e) { return; }

          const isNewMessage = (msg.type === 'CHAT' || msg.type === 'IMAGE') && msg.senderEmail !== email;
          // 지금 그 방을 열어 두고 있을 때만 토스트 생략 (다른 방을 보고 있어도 토스트는 떠야 함)
          if (isNewMessage && msg.roomId !== activeRoomIdRef.current) {
            showToast({
              roomId: msg.roomId,
              title: msg.senderNickname || '새 메시지',
              preview: msg.type === 'IMAGE' ? '[사진]' : msg.content,
            });
          }
          // 새 메시지 / 읽음 / 전송취소 무엇이든 배지·미리보기는 서버 기준으로 재조회
          scheduleReload();
        });
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [email, reloadRooms, scheduleReload, showToast]);

  // ── 같은 탭 내 이벤트: 읽음 처리 / 방 목록 변경(나가기 등) / 창 복귀 ──
  useEffect(() => {
    const onRead = (e) => {
      const { roomId } = e.detail || {};
      if (roomId == null) return;
      recentlyReadRef.current.set(roomId, Date.now());
      setChatRooms(prev => prev.map(r => r.roomId === roomId ? { ...r, unreadCount: 0 } : r));
    };
    const onChanged = () => scheduleReload();
    const onActiveRoom = (e) => { activeRoomIdRef.current = e.detail?.roomId ?? null; };
    window.addEventListener('chat-read', onRead);
    window.addEventListener('chat-active-room', onActiveRoom);
    window.addEventListener('chat-rooms-changed', onChanged);
    window.addEventListener('focus', onChanged);
    return () => {
      window.removeEventListener('chat-read', onRead);
      window.removeEventListener('chat-active-room', onActiveRoom);
      window.removeEventListener('chat-rooms-changed', onChanged);
      window.removeEventListener('focus', onChanged);
    };
  }, [scheduleReload]);

  const totalChatUnread = chatRooms.reduce((sum, r) => sum + (r.unreadCount || 0), 0);

  return (
    <ChatNotificationContext.Provider
      value={{ chatRooms, totalChatUnread, reloadRooms }}
    >
      {children}
      <ChatToast toast={toast} onClose={() => setToast(null)} />
    </ChatNotificationContext.Provider>
  );
};

// Header / SellerDashboardPage 등에서 실시간 안읽음 수를 꺼내 쓰기 위한 훅
export const useChatNotifications = () => {
  const ctx = useContext(ChatNotificationContext);
  return ctx || { chatRooms: [], totalChatUnread: 0, reloadRooms: () => {} };
};
