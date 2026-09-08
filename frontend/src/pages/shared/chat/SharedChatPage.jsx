import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Store, Send, Image as ImageIcon, MoreVertical, Search, User, Calendar, X, LogOut, Trash2, ChevronRight, ShieldCheck } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useSearchParams, useNavigate } from 'react-router-dom';
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
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
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
  // [CHAT-RQ-001] 대화창 헤더 "더보기" 메뉴(나가기) 열림 상태
  const [roomMenuOpen, setRoomMenuOpen] = useState(false);
  // 구매자 화면에서 상단에 보여줄 판매자 프로필 요약 (만족도/거래횟수/후기)
  const [sellerProfile, setSellerProfile] = useState(null);
  // 안전거래 안내 모달
  const [safetyOpen, setSafetyOpen] = useState(false);

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

          // READ 이벤트: 상대가 읽었을 때만 내 말풍선의 1 제거 (msg.senderEmail = 읽은 사람. 내가 읽은 건 무시)
          if (msg.type === 'READ') {
            if (isThisRoomActive && msg.senderEmail && msg.senderEmail !== currentEmail) {
              setMessages(prev =>
                prev.map(m => m.senderEmail === currentEmail ? { ...m, isRead: true } : m)
              );
            }
            return;
          }

          // [CHAT-RQ-002] DELETE 이벤트: 해당 메시지를 "삭제된 메시지"로 치환 + 목록 미리보기 갱신
          if (msg.type === 'DELETE') {
            if (isThisRoomActive) {
              setMessages(prev =>
                prev.map(m => m.id === msg.id ? { ...m, deleted: true, content: '' } : m)
              );
            }
            setRooms(prev =>
              prev.map(r => r.roomId === room.roomId
                ? { ...r, lastMessage: msg.content || '메시지가 삭제되었습니다' }
                : r)
            );
            return;
          }

          // [CHAT-RQ-001] JOIN/LEAVE 시스템 메시지: 가운데 안내로만 추가, 안읽음은 올리지 않음
          if (msg.type === 'JOIN' || msg.type === 'LEAVE') {
            if (isThisRoomActive) {
              setMessages(prev => [...prev, msg]);
            }
            setRooms(prev =>
              prev.map(r => r.roomId === room.roomId ? { ...r, lastMessage: msg.content } : r)
            );
            return;
          }

          // 방 목록 업데이트 (lastMessage + 미읽음). 열어 둔 방이면 창 포커스와 무관하게 미읽음 0
          const isMine = msg.senderEmail === currentEmail;
          setRooms(prev =>
            prev.map(r => {
              if (r.roomId !== room.roomId) return r;
              return {
                ...r,
                lastMessage: msg.type === 'IMAGE' ? '[사진]' : msg.content,
                unreadCount: isThisRoomActive ? 0 : (!isMine ? r.unreadCount + 1 : r.unreadCount),
              };
            })
          );

          // 활성 방이면 메시지 추가 + 읽음 처리 (헤더/전역 배지도 chat-read로 같이 정리됨)
          if (isThisRoomActive) {
            setMessages(prev => [...prev, msg]);
            if (!isMine) {
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

  // ── 방 선택 시: 메시지 로드 + 읽음 처리 + (나가 있던 방이면) 재입장 ──
  useEffect(() => {
    setRoomMenuOpen(false); // [CHAT-RQ-001] 방 전환 시 더보기 메뉴 닫기
    if (!activeRoom) return;
    // loadMessages/markAsRead는 REST라 connected 가드로 막지 않는다 (막으면 배지가 안 사라짐)
    loadMessages(activeRoom.roomId);
    markAsRead(activeRoom.roomId);

    // [CHAT-RQ-001] 나가 있던 판매자가 방을 다시 열면 재입장 (iLeft는 판매자-나감일 때만 true)
    if (activeRoom.iLeft) {
      fetch(`${API_BASE}/chat/rooms/${activeRoom.roomId}/rejoin`, { method: 'POST', ...fetchOptions })
        .then(() => { loadRooms(); loadMessages(activeRoom.roomId); })
        .catch(e => console.error('채팅방 재입장 실패', e));
    }
  }, [activeRoom?.roomId, connected]);

  // 구매자 화면: 방을 열면 상대(판매자) 프로필 요약을 불러와 상단 카드에 표시
  useEffect(() => {
    setSellerProfile(null);
    if (userRole !== 'BUYER' || !activeRoom?.targetEmail) return;
    fetch(`${API_BASE}/sellers/${encodeURIComponent(activeRoom.targetEmail)}/profile`)
      .then(res => (res.ok ? res.json() : null))
      .then(p => p && setSellerProfile(p))
      .catch(() => {});
  }, [activeRoom?.roomId, activeRoom?.targetEmail, userRole]);

  // ── 새 메시지 → 스크롤 맨 아래 ────────────────────────────────
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // 방을 열고 있는데 상대 메시지가 새로 오면 바로 읽음 처리 (좌측/헤더 배지 안 남게)
  // 내가 보낸 메시지로 messages가 늘어난 경우엔 호출하지 않는다 (불필요한 READ 브로드캐스트 방지).
  useEffect(() => {
    if (!activeRoom || messages.length === 0) return;
    const last = messages[messages.length - 1];
    if (last?.senderEmail && last.senderEmail !== currentEmail) {
      markAsRead(activeRoom.roomId);
    }
  }, [activeRoom?.roomId, messages.length, currentEmail, markAsRead]);

  // 지금 열어 둔 방 id를 전역 알림 Context에 알려 준다 (그 방 메시지만 토스트 생략)
  useEffect(() => {
    window.dispatchEvent(new CustomEvent('chat-active-room', { detail: { roomId: activeRoom?.roomId ?? null } }));
    return () => window.dispatchEvent(new CustomEvent('chat-active-room', { detail: { roomId: null } }));
  }, [activeRoom?.roomId]);

  // ── 초기 로드 ─────────────────────────────────────────────────
  useEffect(() => { loadRooms(); }, [loadRooms]);

  // ?roomId=로 들어오면(헤더 알림·토스트 클릭) 그 방을 연다. 다른 방을 보고 있어도 전환되며,
  // 소비 후 쿼리에서 roomId를 지워 목록 갱신 때 다시 끌려가지 않게 한다.
  useEffect(() => {
    if (!initialRoomId) return;
    const target = rooms.find(r => String(r.roomId) === initialRoomId);
    if (!target) return;
    setActiveRoom(target);
    setSearchParams(prev => {
      const p = new URLSearchParams(prev);
      p.delete('roomId');
      return p;
    }, { replace: true });
  }, [initialRoomId, rooms, setSearchParams]);

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

  // [CHAT-RQ-001] 채팅방 나가기 — 구매자는 영구(복구 불가), 판매자는 다시 열면 재입장
  const isSeller = userRole === 'SELLER';
  const handleLeaveRoom = async () => {
    if (!activeRoom) return;
    setRoomMenuOpen(false);
    const confirmMsg = isSeller
      ? '이 채팅방에서 나가시겠습니까?\n나가 있는 동안에는 새 메시지 알림을 받지 않습니다. (다시 열면 재입장)'
      : '이 채팅방에서 나가시겠습니까?\n나간 뒤에는 이전 대화 내용을 복구할 수 없습니다.';
    if (!window.confirm(confirmMsg)) return;

    const leavingRoomId = activeRoom.roomId;
    try {
      const res = await fetch(`${API_BASE}/chat/rooms/${leavingRoomId}/leave`, {
        method: 'POST',
        ...fetchOptions,
      });
      if (!res.ok) throw new Error('채팅방 나가기에 실패했습니다.');
      setActiveRoom(null);
      if (isSeller) {
        // 판매자: 방은 목록에 유지 (LEAVE 브로드캐스트가 미리보기를 "나가셨습니다"로 갱신)
        loadRooms();
      } else {
        // 구매자: 목록에서도 제거
        setRooms(prev => prev.filter(r => r.roomId !== leavingRoomId));
      }
      // 전역 채팅 알림 Context도 이 방의 안읽음/상태를 다시 읽도록 알림
      window.dispatchEvent(new CustomEvent('chat-rooms-changed'));
    } catch (e) {
      console.error('채팅방 나가기 실패', e);
      alert('채팅방 나가기에 실패했습니다. 잠시 후 다시 시도해주세요.');
    }
  };

  // ── [CHAT-RQ-002] 메시지 전송 취소 ──────────
  // 작성자 본인만 가능. 원문은 양측 화면에서 "삭제된 메시지"로 대체되고 새로고침 후에도 유지된다.
  const handleDeleteMessage = async (messageId) => {
    if (!activeRoom || !messageId) return;
    if (!window.confirm('이 메시지를 삭제할까요?\n상대방 화면에서도 "삭제된 메시지"로 표시됩니다.')) return;
    try {
      const res = await fetch(`${API_BASE}/chat/rooms/${activeRoom.roomId}/messages/${messageId}`, {
        method: 'DELETE',
        ...fetchOptions,
      });
      if (!res.ok) throw new Error('메시지 삭제에 실패했습니다.');
      // WebSocket DELETE 이벤트도 오지만, 내 화면은 즉시 반영
      setMessages(prev => prev.map(m => m.id === messageId ? { ...m, deleted: true, content: '' } : m));
    } catch (e) {
      console.error('메시지 삭제 실패', e);
      alert('메시지 삭제에 실패했습니다. 잠시 후 다시 시도해주세요.');
    }
  };

  // ── [CHAT-RQ-003] 채팅 상단 상품명 → 상품 상세로 이동 ──
  // 삭제·존재하지 않는 상품이면 이동하지 않고 안내만 표시한다.
  const handleGoToProduct = async () => {
    const productId = activeRoom?.productId;
    if (!productId) {
      alert('연결된 상품 정보가 없습니다.');
      return;
    }
    try {
      const res = await fetch(`${API_BASE}/products/${productId}`);
      if (!res.ok) throw new Error('not-found');
      navigate(`/buyer/products/${productId}`);
    } catch (e) {
      alert('삭제되었거나 판매가 종료된 상품입니다.');
    }
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
                    {/* [CHAT-RQ-003] 상품명 클릭 시 상품 상세로 이동 (삭제·종료 상품이면 안내) */}
                    <button
                      onClick={handleGoToProduct}
                      title="상품 상세 보기"
                      className="group flex items-center gap-0.5 text-sm font-extrabold text-gray-900 truncate hover:text-blue-600 hover:underline transition"
                    >
                      <span className="truncate">{activeRoom.productName}</span>
                      <ChevronRight size={14} className="shrink-0 text-gray-300 group-hover:text-blue-600" />
                    </button>
                    <span className={`text-xs font-bold mt-0.5 ${connected ? 'text-blue-600' : 'text-gray-400'}`}>
                      {connected ? '공동구매 진행중' : '연결 중...'}
                    </span>
                  </div>
                </div>
                {/* [CHAT-RQ-001] 더보기 → 채팅방 나가기 메뉴 */}
                <div className="relative shrink-0">
                  <button
                    onClick={() => setRoomMenuOpen(v => !v)}
                    className="p-2 text-gray-400 hover:text-gray-600 transition rounded-lg hover:bg-gray-100"
                  >
                    <MoreVertical size={20} />
                  </button>
                  {roomMenuOpen && (
                    <>
                      <div className="fixed inset-0 z-10" onClick={() => setRoomMenuOpen(false)} />
                      <div className="absolute right-0 mt-1 z-20 w-44 bg-white border border-gray-200 rounded-xl shadow-lg py-1">
                        <button
                          onClick={handleLeaveRoom}
                          className="w-full flex items-center gap-2 px-4 py-2.5 text-sm font-bold text-red-600 hover:bg-red-50 transition"
                        >
                          <LogOut size={16} /> 채팅방 나가기
                        </button>
                      </div>
                    </>
                  )}
                </div>
              </div>

              {/* 안전거래 안내 배너 (구매자·판매자 공통, 클릭 시 모달) */}
              <button
                onClick={() => setSafetyOpen(true)}
                className="mx-4 mt-3 flex items-center gap-2.5 px-4 py-3 rounded-2xl bg-blue-50 text-blue-700 text-xs font-bold shrink-0 hover:bg-blue-100 transition"
              >
                <ShieldCheck size={16} className="shrink-0 text-blue-500" />
                <span className="flex-grow text-left">공동구매 외 개인 거래·선입금 요청은 주의하세요</span>
                <ChevronRight size={16} className="shrink-0 text-blue-400" />
              </button>

              {/* 메시지 목록 */}
              <div className="flex-grow overflow-y-auto p-4 flex flex-col gap-4 bg-gray-50/30">
                {/* 구매자 화면: 상단에 판매자 프로필 요약 카드 */}
                {userRole === 'BUYER' && sellerProfile && (
                  <div className="flex flex-col items-center gap-1.5 py-4">
                    <div className="w-16 h-16 rounded-full bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600 font-black text-2xl">
                      {(sellerProfile.nickname || activeRoom.targetName || '?').charAt(0)}
                    </div>
                    <span className="text-base font-extrabold text-gray-900">
                      {sellerProfile.nickname || activeRoom.targetName}
                    </span>
                    <span className="text-xs font-medium text-gray-500">
                      만족도 {sellerProfile.satisfactionRate}% · 거래 {sellerProfile.tradeCount}회 · 후기 {sellerProfile.totalReviews}
                    </span>
                    <div className="flex flex-col items-center gap-0.5 text-xs text-gray-400 leading-relaxed mt-2">
                      <span>휴대폰 본인인증과 CA 인증서로 검증된 판매자예요.</span>
                      <span>거래·결제 내역은 블록체인에 기록돼 위·변조를 막아요.</span>
                      <span>
                        {sellerProfile.totalReviews > 0 && sellerProfile.satisfactionRate >= 80
                          ? '구매자 대부분이 만족한 거래였어요.'
                          : sellerProfile.tradeCount > 0
                            ? `지금까지 ${sellerProfile.tradeCount}번의 공동구매를 성사시켰어요.`
                            : '아직 후기가 많지 않은 새 판매자예요.'}
                      </span>
                    </div>
                  </div>
                )}
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

                    // [CHAT-RQ-001] 나가기/재입장 안내는 날짜 표시처럼 가운데 회색 텍스트로
                    if (msg.type === 'JOIN' || msg.type === 'LEAVE') {
                      return (
                        <div key={msg.id || `sys-${msg.sentAt}`} className="flex justify-center my-1">
                          <span className="text-[11px] font-medium text-gray-400 bg-gray-100 px-3 py-1 rounded-full">
                            {msg.content}
                          </span>
                        </div>
                      );
                    }

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
                          className={`group flex max-w-[75%] ${isMe ? 'self-end flex-row-reverse' : 'self-start'}`}
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
                              {/* [CHAT-RQ-002] 전송 취소된 메시지는 원문 대신 표식 표시 */}
                              {msg.deleted ? (
                                <div className="p-3 rounded-2xl text-sm italic text-gray-400 bg-gray-100 border border-gray-200">
                                  메시지가 삭제되었습니다
                                </div>
                              ) : msg.type === 'IMAGE' ? (
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
                              {/* [CHAT-RQ-002] 내 메시지에만, hover 시 삭제 버튼 */}
                              {isMe && !msg.deleted && msg.id && (
                                <button
                                  onClick={() => handleDeleteMessage(msg.id)}
                                  title="메시지 삭제"
                                  className="opacity-0 group-hover:opacity-100 transition p-1 text-gray-300 hover:text-red-500 shrink-0 self-center"
                                >
                                  <Trash2 size={14} />
                                </button>
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

      {/* 안전거래 안내 모달 */}
      {safetyOpen && (
        <div
          className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4"
          onClick={() => setSafetyOpen(false)}
        >
          <div
            className="relative w-full max-w-sm bg-white rounded-3xl p-7 flex flex-col items-center text-center gap-4"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={() => setSafetyOpen(false)}
              className="absolute top-4 right-4 text-gray-300 hover:text-gray-500 transition"
            >
              <X size={22} />
            </button>
            <div className="w-14 h-14 rounded-full bg-blue-50 flex items-center justify-center text-blue-500">
              <ShieldCheck size={28} />
            </div>
            <h3 className="text-lg font-extrabold text-gray-900 leading-snug">
              안전한 거래를 위한<br /><span className="text-blue-600">개인 거래 주의 안내</span>
            </h3>
            <p className="text-sm text-gray-500 leading-relaxed">
              모든 결제는 <b className="font-bold text-gray-700">공동구매 참여하기(N빵 탑승)</b>를 통해 앱 안에서 이루어져요.
              공동구매를 거치지 않고 직접 거래하거나, 먼저 돈을 보내달라는 요청은 사기일 수 있습니다.
            </p>
            <p className="text-sm text-gray-500 leading-relaxed">
              모든 판매자는 <b className="font-bold text-gray-700">운영진 승인</b>을 거쳐 등록되며,
              신고된 거래는 운영진이 직접 확인·조치해요.
            </p>
            <div className="w-full bg-gray-50 border border-gray-100 rounded-2xl p-4 text-xs text-gray-500 leading-relaxed">
              계좌번호·외부 링크 등으로 개인 거래를 유도하는 메시지는 상대에게 전달되지 않을 수 있으며,
              반복 시 이용이 제한될 수 있습니다.
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SharedChatPage;