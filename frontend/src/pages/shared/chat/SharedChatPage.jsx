import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Store, Send, Image as ImageIcon, MoreVertical, Search, User, Calendar, X } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useSearchParams } from 'react-router-dom';
import Header from '../../../components/layout/Header';

// [수정] localhost 고정 → 접속 호스트 기준 동적화
const API_BASE = `http://${window.location.hostname}:8080/api`;
const WS_URL = `http://${window.location.hostname}:8080/ws`;

const formatTime = (dateTimeStr) => {
  if (!dateTimeStr) return '';
  const date = new Date(dateTimeStr);
  const now = new Date();
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);

  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  } else if (date.toDateString() === yesterday.toDateString()) {
    return '어제';
  } else {
    return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
  }
};

// 메시지 말풍선 옆에 붙는 시간 (오전/오후 h:mm)
const formatMessageClock = (dateTimeStr) => {
  if (!dateTimeStr) return '';
  return new Date(dateTimeStr).toLocaleTimeString('ko-KR', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  });
};

// 날짜 구분선 (yyyy년 m월 d일 요일)
const formatDateSeparator = (dateTimeStr) => {
  if (!dateTimeStr) return '';
  return new Date(dateTimeStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  });
};

const isSameDay = (a, b) => {
  if (!a || !b) return false;
  return new Date(a).toDateString() === new Date(b).toDateString();
};

const SharedChatPage = ({ userRole = 'SELLER' }) => {
  // [신규] 상품 상세페이지의 "구매자 문의 확인하기" 등에서 ?productId=로 넘어오면 해당 게시물 채팅만 표시
  const [searchParams] = useSearchParams();
  const filterProductId = searchParams.get('productId');
  // [신규] 헤더 채팅 알림 미리보기에서 ?roomId=로 넘어오면 그 방을 자동으로 오픈
  const initialRoomId = searchParams.get('roomId');

  const [rooms, setRooms] = useState([]);
  const [activeRoom, setActiveRoom] = useState(null);
  const [messages, setMessages] = useState([]);
  const [message, setMessage] = useState('');
  const [connected, setConnected] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [uploadingImage, setUploadingImage] = useState(false);
  const [lightboxImage, setLightboxImage] = useState(null);

  const fileInputRef = useRef(null);
  const stompClientRef = useRef(null);
  const allSubscriptionsRef = useRef({});  // roomId → subscription
  const activeRoomRef = useRef(null);      // 스테일 클로저 방지
  const messagesEndRef = useRef(null);

  const currentEmail = localStorage.getItem('email');
  const fetchOptions = { credentials: 'include' };

  // ── 채팅방 목록 로드 ──────────────────────────────────────────
  const loadRooms = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/chat/rooms`, fetchOptions);
      if (!res.ok) return;
      const data = await res.json();
      setRooms(data);
    } catch (e) {
      console.error('채팅방 목록 로드 실패', e);
    }
  }, []);

  // ── 메시지 내역 로드 ──────────────────────────────────────────
  const loadMessages = useCallback(async (roomId) => {
    try {
      const res = await fetch(`${API_BASE}/chat/rooms/${roomId}/messages`, fetchOptions);
      if (!res.ok) return;
      setMessages(await res.json());
    } catch (e) {
      console.error('메시지 내역 로드 실패', e);
    }
  }, []);

  // ── 읽음 처리 ─────────────────────────────────────────────────
  const markAsRead = useCallback(async (roomId) => {
    // [수정] 서버 응답 기다리지 않고 먼저 지움 (낙관적 업데이트)
    setRooms(prev =>
      prev.map(r => r.roomId === roomId ? { ...r, unreadCount: 0 } : r)
    );
    window.dispatchEvent(new CustomEvent('chat-read', { detail: { roomId } }));

    try {
      await fetch(`${API_BASE}/chat/rooms/${roomId}/read`, {
        method: 'POST',
        ...fetchOptions,
      });
    } catch (e) {
      console.error('읽음 처리 실패', e);
    }
  }, []);

  // ── WebSocket 연결 ─────────────────────────────────────────────
  useEffect(() => {
    if (!currentEmail) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: { 'X-User-Email': currentEmail },
      onConnect: () => setConnected(true),
      onDisconnect: () => setConnected(false),
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      client.deactivate();
      setConnected(false);
    };
  }, [currentEmail]);

  // activeRoom 변경 시 ref 동기화 (스테일 클로저 방지)
  useEffect(() => {
    activeRoomRef.current = activeRoom;
  }, [activeRoom]);

  // ── 창 복귀 시 읽음 처리 ─────────────────────────────────────────
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && activeRoomRef.current) {
        markAsRead(activeRoomRef.current.roomId);
      }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [markAsRead]);

  // ── 전체 방 구독 (연결되면 모든 방을 한 번에 구독) ──────────────
  const roomsKey = rooms.map(r => r.roomId).join(',');
  useEffect(() => {
    if (!connected || rooms.length === 0) return;

    // 기존 구독 해제
    Object.values(allSubscriptionsRef.current).forEach(sub => sub.unsubscribe());
    allSubscriptionsRef.current = {};

    rooms.forEach(room => {
      allSubscriptionsRef.current[room.roomId] = stompClientRef.current.subscribe(
        `/topic/chat/${room.roomId}`,
        (frame) => {
          const msg = JSON.parse(frame.body);
          const isThisRoomActive = activeRoomRef.current?.roomId === room.roomId;
          const isVisible = document.visibilityState === 'visible';

          // READ 이벤트: 활성 방이면 1 표시 제거
          if (msg.type === 'READ') {
            if (isThisRoomActive) {
              setMessages(prev =>
                prev.map(m => m.senderEmail === currentEmail ? { ...m, isRead: true } : m)
              );
            }
            return;
          }

          // 방 목록 업데이트 (lastMessage + 미읽음 카운트)
          const isOthersPending = msg.senderEmail !== currentEmail && !(isThisRoomActive && isVisible);
          setRooms(prev =>
            prev.map(r => {
              if (r.roomId !== room.roomId) return r;
              return {
                ...r,
                lastMessage: msg.type === 'IMAGE' ? '[사진]' : msg.content,
                unreadCount: isOthersPending ? r.unreadCount + 1 : r.unreadCount,
              };
            })
          );

          // 활성 방이면 메시지 추가
          if (isThisRoomActive) {
            setMessages(prev => [...prev, msg]);
            if (msg.senderEmail !== currentEmail && isVisible) {
              markAsRead(room.roomId);
            }
          }
        }
      );
    });

    return () => {
      Object.values(allSubscriptionsRef.current).forEach(sub => sub.unsubscribe());
      allSubscriptionsRef.current = {};
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [connected, roomsKey]);

  // ── 방 선택 시: 메시지 로드 + 읽음 처리 ─────────────────────────
  useEffect(() => {
    if (!activeRoom || !connected) return;
    loadMessages(activeRoom.roomId);
    markAsRead(activeRoom.roomId);
  }, [activeRoom?.roomId, connected]);

  // ── 새 메시지 → 스크롤 맨 아래 ────────────────────────────────
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // ── 초기 로드 ─────────────────────────────────────────────────
  useEffect(() => { loadRooms(); }, [loadRooms]);

  // [신규] roomId로 들어온 경우 방 목록 로드되면 자동 오픈
  useEffect(() => {
    if (!initialRoomId || activeRoom) return;
    const target = rooms.find(r => String(r.roomId) === initialRoomId);
    if (target) setActiveRoom(target);
  }, [initialRoomId, rooms, activeRoom]);

  // ── 메시지 전송 ───────────────────────────────────────────────
  const handleSendMessage = (e) => {
    e.preventDefault();
    if (!message.trim() || !activeRoom || !connected) return;

    stompClientRef.current.publish({
      destination: '/app/chat.message',
      body: JSON.stringify({ roomId: activeRoom.roomId, content: message.trim() }),
    });
    setMessage('');
  };

  // ── 이미지 첨부 ───────────────────────────────────────────────
  const handleImageButtonClick = () => {
    if (!connected || !activeRoom) return;
    fileInputRef.current?.click();
  };

  const handleImageSelected = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = ''; // 같은 파일 다시 선택해도 onChange 발생하도록 초기화
    if (!file || !activeRoom) return;

    setUploadingImage(true);
    try {
      const formData = new FormData();
      formData.append('image', file);

      const res = await fetch(`${API_BASE}/chat/upload`, {
        method: 'POST',
        credentials: 'include',
        body: formData,
      });
      if (!res.ok) throw new Error('이미지 업로드 실패');
      const { url } = await res.json();

      stompClientRef.current.publish({
        destination: '/app/chat.message',
        body: JSON.stringify({ roomId: activeRoom.roomId, content: url, type: 'IMAGE' }),
      });
    } catch (err) {
      console.error('이미지 전송 실패', err);
      alert('이미지 전송에 실패했습니다.');
    } finally {
      setUploadingImage(false);
    }
  };

  const filteredRooms = rooms
    .filter(r => !filterProductId || String(r.productId) === filterProductId)
    .filter(r => r.targetName?.includes(searchQuery) || r.productName?.includes(searchQuery));

  // ── 로그인 안 된 경우 ─────────────────────────────────────────
  if (!currentEmail) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col">
        <Header />
        <div className="flex-grow flex items-center justify-center">
          <p className="text-gray-500">로그인 후 이용할 수 있습니다.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900 h-screen">
      <Header />

      {/* 권한에 따라 변하는 상단 배너 (조건부 렌더링) */}
      {userRole === 'SELLER' ? (
        <section className="bg-slate-900 text-white py-6 px-6 shadow-md shrink-0">
          <div className="max-w-7xl mx-auto flex items-center justify-between">
            <div className="flex flex-col gap-1">
              <span className="bg-emerald-500/20 text-emerald-400 text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full w-max border border-emerald-500/30 flex items-center gap-1">
                <Store size={12} /> Seller Hub
              </span>
              <h2 className="text-2xl font-extrabold tracking-tight">메시지 관리</h2>
              <p className="text-sm text-gray-400">구매자와의 소통을 한 곳에서 관리하세요.</p>
            </div>
          </div>
        </section>
      ) : (
        <section className="bg-white border-b border-gray-200 py-6 px-6 shrink-0">
          <div className="max-w-7xl mx-auto">
            <h2 className="text-2xl font-extrabold tracking-tight text-gray-900">내 채팅</h2>
          </div>
        </section>
      )}

      {/* 메인 채팅 레이아웃 (좌측 리스트 + 우측 대화창) */}
      <main className="flex-grow max-w-7xl w-full mx-auto p-4 md:p-6 pb-8 md:pb-12 flex gap-4 overflow-hidden min-h-0">
        
        {/* 좌측: 채팅방 목록 */}
        <aside className="w-full md:w-1/3 bg-white rounded-2xl border border-gray-200 shadow-sm flex-col overflow-hidden h-full hidden md:flex">
          <div className="p-4 border-b border-gray-100 bg-gray-50/50">
            <div className="relative">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="채팅방 또는 참여자 검색"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500 transition"
              />
            </div>
          </div>

          <div className="flex-grow overflow-y-auto">
            {filteredRooms.length === 0 ? (
              <div className="flex items-center justify-center h-32 text-sm text-gray-400">
                채팅방이 없습니다
              </div>
            ) : (
              filteredRooms.map((room) => (
                <div
                  key={room.roomId}
                  onClick={() => setActiveRoom(room)}
                  className={`p-4 border-b border-gray-50 flex items-start gap-3 cursor-pointer transition ${
                    activeRoom?.roomId === room.roomId ? 'bg-blue-50/50' : 'hover:bg-gray-50'
                  }`}
                >
                  <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 flex-shrink-0">
                    <User size={20} />
                  </div>
                  <div className="flex flex-col flex-grow overflow-hidden">
                    <div className="flex justify-between items-center mb-0.5">
                      <h4 className="text-sm font-bold text-gray-900 truncate">{room.targetName}</h4>
                      <span className="text-[10px] text-gray-400 font-medium whitespace-nowrap ml-2">
                        {formatTime(room.lastSentAt)}
                      </span>
                    </div>
                    <p className="text-xs font-medium text-gray-500 truncate mb-1">{room.productName}</p>
                    <p className={`text-xs truncate ${room.unreadCount > 0 ? 'text-gray-900 font-bold' : 'text-gray-500'}`}>
                      {room.lastMessage || '새 채팅방'}
                    </p>
                  </div>
                  {room.unreadCount > 0 && (
                    <div className="w-5 h-5 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center flex-shrink-0 mt-4">
                      {room.unreadCount}
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        </aside>

        {/* 우측: 대화창 */}
        <section className="flex-grow bg-white rounded-2xl border border-gray-200 shadow-sm flex flex-col overflow-hidden h-full">
          {!activeRoom ? (
            <div className="flex-grow flex items-center justify-center text-sm text-gray-400">
              채팅방을 선택해주세요
            </div>
          ) : (
            <>
              {/* 대화창 헤더 */}
              <div className="p-4 border-b border-gray-100 flex justify-between items-center bg-white shrink-0">
                <div className="flex items-center gap-3 overflow-hidden">
                  <div className="w-12 h-12 bg-gray-100 rounded-xl flex items-center justify-center text-gray-400 shrink-0">
                    <Store size={20} />
                  </div>
                  <div className="flex flex-col truncate">
                    <span className="text-[11px] font-bold text-gray-500">{activeRoom.targetName}</span>
                    <h3 className="text-sm font-extrabold text-gray-900 truncate">{activeRoom.productName}</h3>
                    <span className={`text-xs font-bold mt-0.5 ${connected ? 'text-blue-600' : 'text-gray-400'}`}>
                      {connected ? '공동구매 진행중' : '연결 중...'}
                    </span>
                  </div>
                </div>
                <button className="p-2 text-gray-400 hover:text-gray-600 transition rounded-lg hover:bg-gray-100 shrink-0">
                  <MoreVertical size={20} />
                </button>
              </div>

              {/* 메시지 목록 */}
              <div className="flex-grow overflow-y-auto p-4 flex flex-col gap-4 bg-gray-50/30">
                {messages.length === 0 && (
                  <div className="text-center my-4">
                    <span className="text-[11px] font-medium text-gray-400 bg-gray-100 px-3 py-1 rounded-full">
                      대화를 시작해보세요
                    </span>
                  </div>
                )}
                {(() => {
                  const filteredMessages = messages.filter(msg => msg.type !== 'READ');
                  return filteredMessages.map((msg, idx) => {
                    const isMe = msg.senderEmail === currentEmail;
                    const prevMsg = filteredMessages[idx - 1];
                    const showDateSeparator = !isSameDay(prevMsg?.sentAt, msg.sentAt);
                    return (
                      <React.Fragment key={msg.id || `${msg.senderEmail}-${msg.sentAt}`}>
                        {showDateSeparator && (
                          <div className="flex justify-center my-2">
                            <span className="flex items-center gap-1.5 text-[11px] font-bold text-gray-500 bg-gray-200/70 px-3 py-1 rounded-full">
                              <Calendar size={12} />
                              {formatDateSeparator(msg.sentAt)}
                            </span>
                          </div>
                        )}
                        <div
                          className={`flex max-w-[75%] ${isMe ? 'self-end flex-row-reverse' : 'self-start'}`}
                        >
                          {!isMe && (
                            <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 shrink-0 mr-2">
                              <User size={16} />
                            </div>
                          )}
                          <div className={`flex flex-col gap-1 ${isMe ? 'items-end' : 'items-start'}`}>
                            {!isMe && (
                              <span className="text-[11px] font-medium text-gray-500 px-1">
                                {msg.senderNickname}
                              </span>
                            )}
                            <div className="flex items-end gap-1">
                              {isMe && (
                                <div className="flex flex-col items-end mb-0.5">
                                  {!msg.isRead && (
                                    <span className="text-[10px] font-bold text-yellow-500">1</span>
                                  )}
                                  <span className="text-[10px] text-gray-400 font-medium whitespace-nowrap">
                                    {formatMessageClock(msg.sentAt)}
                                  </span>
                                </div>
                              )}
                              {msg.type === 'IMAGE' ? (
                                <img
                                  src={msg.content}
                                  alt="전송된 이미지"
                                  className="max-w-[220px] max-h-[220px] rounded-2xl object-cover cursor-pointer"
                                  onClick={() => setLightboxImage(msg.content)}
                                />
                              ) : (
                                <div className={`p-3 rounded-2xl text-sm ${
                                  isMe
                                    ? 'bg-blue-600 text-white rounded-tr-none'
                                    : 'bg-white border border-gray-200 text-gray-800 rounded-tl-none shadow-sm'
                                }`}>
                                  {msg.content}
                                </div>
                              )}
                              {!isMe && (
                                <span className="text-[10px] text-gray-400 font-medium self-end mb-0.5 whitespace-nowrap">
                                  {formatMessageClock(msg.sentAt)}
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      </React.Fragment>
                    );
                  });
                })()}
                <div ref={messagesEndRef} />
              </div>

              {/* 메시지 입력창 */}
              <div className="p-4 bg-white border-t border-gray-100 shrink-0">
                <form
                  onSubmit={handleSendMessage}
                  className="flex items-end gap-2 bg-gray-50 p-2 rounded-2xl border border-gray-200 focus-within:border-blue-300 focus-within:ring-2 focus-within:ring-blue-100 transition"
                >
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleImageSelected}
                    className="hidden"
                  />
                  <button
                    type="button"
                    onClick={handleImageButtonClick}
                    disabled={!connected || uploadingImage}
                    className="p-2.5 text-gray-400 hover:text-gray-600 transition rounded-xl hover:bg-gray-200 shrink-0 disabled:opacity-50"
                  >
                    <ImageIcon size={20} />
                  </button>
                  <textarea
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    placeholder={connected ? '메시지를 입력하세요...' : '연결 중...'}
                    disabled={!connected}
                    className="flex-grow bg-transparent outline-none text-sm resize-none py-2.5 max-h-32 min-h-[44px] disabled:opacity-50"
                    rows={1}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
                        e.preventDefault();
                        handleSendMessage(e);
                      }
                    }}
                  />
                  <button
                    type="submit"
                    disabled={!message.trim() || !connected}
                    className={`p-2.5 rounded-xl transition shrink-0 flex items-center justify-center ${
                      message.trim() && connected
                        ? 'bg-blue-600 text-white shadow-md hover:bg-blue-700'
                        : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                    }`}
                  >
                    <Send size={18} />
                  </button>
                </form>
              </div>
            </>
          )}
        </section>

      </main>

      {/* 이미지 라이트박스 모달 */}
      {lightboxImage && (
        <div
          className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4"
          onClick={() => setLightboxImage(null)}
        >
          <button
            onClick={() => setLightboxImage(null)}
            className="absolute top-4 right-4 text-white/80 hover:text-white transition p-2"
          >
            <X size={28} />
          </button>
          <img
            src={lightboxImage}
            alt="원본 이미지"
            className="max-w-full max-h-full rounded-lg object-contain"
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}
    </div>
  );
};

export default SharedChatPage;