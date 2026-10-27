/// 사이트 어느 페이지에 있든 항상 켜져 있는 채팅 알림 기능
/// 로그인한 본인 계정으로 온 채팅 알림을 웹소켓으로 구독해서 헤더 종 아이콘과 채팅 메뉴 표시를 바로 갱신함
import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { X } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const API_BASE = `http://${window.location.hostname}:8080/api`;
const WS_URL = `http://${window.location.hostname}:8080/ws`;

const ChatNotificationContext = createContext(null);

/// 화면 오른쪽 아래에 뜨는 새 메시지 알림 창 (누르면 그 채팅방으로 이동)
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
  /// 방금 읽은 방은 서버 응답이 늦게 와도 안 읽은 수를 0으로 유지
  const recentlyReadRef = useRef(new Map());
  /// 지금 열어 둔 채팅방 (그 방 메시지는 알림 창 생략)
  const activeRoomIdRef = useRef(null);

  const email = localStorage.getItem('email');

  const reloadRooms = useCallback(async () => {
    if (!localStorage.getItem('email')) { setChatRooms([]); return; }
    try {
      const res = await fetch(`${API_BASE}/chat/rooms`, { credentials: 'include' });
      if (!res.ok) return;
      const data = await res.json();
      /// 3초 안에 읽은 방은 서버 값과 상관없이 0으로 표시
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

  /// 알림이 연달아 와도 0.3초 모았다가 목록을 한 번만 다시 불러옴
  const scheduleReload = useCallback(() => {
    clearTimeout(reloadTimerRef.current);
    reloadTimerRef.current = setTimeout(reloadRooms, 300);
  }, [reloadRooms]);

  /// 알림 창은 4초 뒤 자동으로 사라짐
  const showToast = useCallback((payload) => {
    setToast(payload);
    clearTimeout(toastTimerRef.current);
    toastTimerRef.current = setTimeout(() => setToast(null), 4000);
  }, []);

  /// 웹소켓 연결 후 로그인한 본인 계정으로 온 채팅 알림 구독 (/topic/chat/user/{이메일})
  useEffect(() => {
    if (!email) { setChatRooms([]); return; }

    reloadRooms();

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      /// 연결이 끊기면 5초마다 자동 재연결
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/chat/user/${email}`, (frame) => {
          let msg;
          try { msg = JSON.parse(frame.body); } catch (e) { return; }

          const isNewMessage = (msg.type === 'CHAT' || msg.type === 'IMAGE') && msg.senderEmail !== email;
          /// 지금 보고 있는 방의 메시지만 알림 창 생략 (다른 방이면 알림)
          if (isNewMessage && msg.roomId !== activeRoomIdRef.current) {
            showToast({
              roomId: msg.roomId,
              title: msg.senderNickname || '새 메시지',
              preview: msg.type === 'IMAGE' ? '[사진]' : msg.content,
            });
          }
          /// 새 메시지, 읽음, 삭제 무엇이든 서버에서 목록을 다시 불러와 숫자 갱신
          scheduleReload();
        });
        /// 다시 연결되면 끊긴 동안 놓친 알림도 반영
        scheduleReload();
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [email, reloadRooms, scheduleReload, showToast]);

  /// 같은 탭 안의 신호 처리 (읽음, 방 목록 변경, 창으로 돌아옴)
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

  /// 헤더에 표시할 전체 안 읽은 메시지 수
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

/// 헤더 등에서 안 읽은 수를 꺼내 쓰는 훅
export const useChatNotifications = () => {
  const ctx = useContext(ChatNotificationContext);
  return ctx || { chatRooms: [], totalChatUnread: 0, reloadRooms: () => {} };
};
