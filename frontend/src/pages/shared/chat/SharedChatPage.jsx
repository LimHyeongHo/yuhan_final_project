import { API_BASE, WS_URL } from '../../../config/api';
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Store, Send, Image as ImageIcon, MoreVertical, Search, User, Calendar, X, LogOut, Trash2, ChevronRight, ChevronLeft, ShieldCheck, Star, MessageCircle, TriangleAlert, Flag, Check } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useSearchParams, useNavigate } from 'react-router-dom';
import V3SiteHeader from '../../../components/layout/V3SiteHeader';
import './SharedChatPage.css';
import ChatReportModal from './ChatReportModal';

// 접속한 주소 기준으로 서버 주소를 정함 (다른 기기에서 접속해도 동작)
const API_BASE = `http://${window.location.hostname}:8080/api`;
const WS_URL = `http://${window.location.hostname}:8080/ws`;
const FETCH_OPTIONS = { credentials: 'include' };

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

// 말풍선 옆 시간 (오전/오후 h:mm)
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
  // 상품 상세에서 넘어오면 그 상품 채팅만 표시 (?productId=)
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const filterProductId = searchParams.get('productId');
  // 알림을 눌러 들어오면 그 채팅방을 바로 열기 (?roomId=)
  const initialRoomId = searchParams.get('roomId');

  const [rooms, setRooms] = useState([]);
  const [activeRoom, setActiveRoom] = useState(null);
  const [messages, setMessages] = useState([]);
  const [message, setMessage] = useState('');
  // 방마다 쓰던 글 임시 저장 (페이지를 벗어나면 사라짐)
  const draftsRef = useRef({});
  // 마지막으로 보낸 글 (전송 실패 시 입력창에 되돌리기용)
  const lastSentRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [uploadingImage, setUploadingImage] = useState(false);
  const [lightboxImage, setLightboxImage] = useState(null);
  // 더보기 메뉴(신고하기, 나가기) 열림 여부
  const [roomMenuOpen, setRoomMenuOpen] = useState(false);
  // 신고 모드 여부, 고른 메시지, 신고 창 열림 여부
  const [reportMode, setReportMode] = useState(false);
  const [reportSelectedId, setReportSelectedId] = useState(null);
  const [reportModalOpen, setReportModalOpen] = useState(false);
  // 구매자 화면 상단의 판매자 정보 (만족도, 거래 횟수, 후기)
  const [sellerProfile, setSellerProfile] = useState(null);
  // 후기 작성 배너를 띄울지 판단하는 정보 (정산 완료된 방)
  const [reviewEligibility, setReviewEligibility] = useState(null);
  // 안전거래 안내 창
  const [safetyOpen, setSafetyOpen] = useState(false);
  // 브라우저 기본 alert 대신 쓰는 안내 창
  const [dialog, setDialog] = useState(null);
  const showDialog = (message, { confirm = false } = {}) =>
    new Promise((resolve) => setDialog({ message, confirm, resolve }));
  const closeDialog = (result) => {
    dialog?.resolve(result);
    setDialog(null);
  };
  // 왼쪽 채팅방 목록 폭 (구분선을 드래그할 때만 바뀜)
  const [listWidth, setListWidth] = useState(300);

  const fileInputRef = useRef(null);
  const stompClientRef = useRef(null);
  const allSubscriptionsRef = useRef({});  // 방 번호별 구독 정보
  const activeRoomRef = useRef(null);      // 웹소켓 콜백에서도 지금 열린 방을 정확히 알기 위한 참조
  const messagesEndRef = useRef(null);
  const asideRef = useRef(null);
  const isResizingRef = useRef(false);

  const currentEmail = localStorage.getItem('email');

  // 채팅방 목록 불러오기
  const loadRooms = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/chat/rooms`, FETCH_OPTIONS);
      if (!res.ok) return;
      const data = await res.json();
      setRooms(data);
    } catch (e) {
      console.error('채팅방 목록 로드 실패', e);
    }
  }, []);

  // 메시지 내역 불러오기
  const loadMessages = useCallback(async (roomId) => {
    try {
      const res = await fetch(`${API_BASE}/chat/rooms/${roomId}/messages`, FETCH_OPTIONS);
      if (!res.ok) return;
      setMessages(await res.json());
    } catch (e) {
      console.error('메시지 내역 로드 실패', e);
    }
  }, []);

  // 읽음 처리
  const markAsRead = useCallback(async (roomId) => {
    // 서버 응답을 기다리지 않고 화면의 숫자부터 지움 (빠르게 보이도록)
    setRooms(prev =>
      prev.map(r => r.roomId === roomId ? { ...r, unreadCount: 0 } : r)
    );
    // 헤더 종 아이콘과 채팅 메뉴 표시에도 읽었다고 알림
    window.dispatchEvent(new CustomEvent('chat-read', { detail: { roomId } }));

    try {
      await fetch(`${API_BASE}/chat/rooms/${roomId}/read`, {
        method: 'POST',
        ...FETCH_OPTIONS,
      });
    } catch (e) {
      console.error('읽음 처리 실패', e);
    }
  }, []);

  // 웹소켓 연결 (/ws, 끊기면 5초마다 재연결)
  useEffect(() => {
    if (!currentEmail) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      // 다시 연결되면 끊긴 동안 바뀐 채팅방 목록을 다시 불러옴
      onConnect: () => {
        setConnected(true);
        loadRooms();
        // 서버가 메시지 전송을 거부하면 나에게만 에러가 옴 (/user/queue/errors)
        // 안내 창을 띄우고 보내려던 글을 입력창에 되돌림
        client.subscribe('/user/queue/errors', (frame) => {
          let err;
          try { err = JSON.parse(frame.body); } catch (e) { err = null; }
          const last = lastSentRef.current;
          lastSentRef.current = null;
          if (last && activeRoomRef.current?.roomId === last.roomId) {
            setMessage(prev => prev || last.text);
            draftsRef.current[last.roomId] = draftsRef.current[last.roomId] || last.text;
          }
          setDialog({
            message: `메시지를 보내지 못했어요.\n${err?.message || '잠시 후 다시 시도해주세요.'}`,
            confirm: false,
            resolve: () => { },
          });
        });
      },
      onDisconnect: () => setConnected(false),
      // 서버가 꺼지거나 네트워크가 끊기면 입력을 막음
      onWebSocketClose: () => setConnected(false),
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      client.deactivate();
      setConnected(false);
    };
  }, [currentEmail, loadRooms]);

  // 웹소켓 콜백에서 쓸 수 있게 지금 열린 방을 참조에 저장
  useEffect(() => {
    activeRoomRef.current = activeRoom;
  }, [activeRoom]);

  // 다른 탭에 갔다가 돌아오면 열린 방 읽음 처리
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && activeRoomRef.current) {
        markAsRead(activeRoomRef.current.roomId);
      }
    };
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange);
  }, [markAsRead]);

  // 내 채팅방 전체를 한 번에 구독 (목록 미리보기와 안 읽은 수 실시간 갱신)
  const roomsKey = rooms.map(r => r.roomId).join(',');
  // 이미 끊긴 연결의 구독 해제 에러는 무시
  const clearAllSubscriptions = () => {
    Object.values(allSubscriptionsRef.current).forEach(sub => {
      try { sub.unsubscribe(); } catch (e) { }
    });
    allSubscriptionsRef.current = {};
  };

  useEffect(() => {
    if (!connected || rooms.length === 0) return;

    clearAllSubscriptions();

    rooms.forEach(room => {
      allSubscriptionsRef.current[room.roomId] = stompClientRef.current.subscribe(
        `/topic/chat/${room.roomId}`,
        (frame) => {
          const msg = JSON.parse(frame.body);
          const isThisRoomActive = activeRoomRef.current?.roomId === room.roomId;

          // 상대가 읽었다는 이벤트가 오면 내 말풍선의 '1'을 지움 (내가 읽은 건 무시)
          if (msg.type === 'READ') {
            if (isThisRoomActive && msg.senderEmail && msg.senderEmail !== currentEmail) {
              setMessages(prev =>
                prev.map(m => m.senderEmail === currentEmail ? { ...m, isRead: true } : m)
              );
            }
            return;
          }

          // 삭제 이벤트: 그 메시지를 '삭제된 메시지'로 바꿈
          // 마지막 메시지가 삭제된 경우에만 목록 미리보기도 바꿈
          if (msg.type === 'DELETE') {
            if (isThisRoomActive) {
              setMessages(prev =>
                prev.map(m => m.id === msg.id ? { ...m, deleted: true, content: '' } : m)
              );
            }
            if (msg.previewChanged) {
              setRooms(prev =>
                prev.map(r => r.roomId === room.roomId
                  ? { ...r, lastMessage: msg.content || '메시지가 삭제되었습니다' }
                  : r)
              );
            }
            return;
          }

          // 입장, 퇴장 안내는 가운데 안내로만 표시 (안 읽은 수는 안 올림)
          if (msg.type === 'JOIN' || msg.type === 'LEAVE') {
            // 다른 탭이나 기기에서 내가 나가거나 다시 들어오면 이 탭도 맞춰서 동기화
            if (msg.senderEmail === currentEmail) {
              if (msg.type === 'LEAVE' && isThisRoomActive) setActiveRoom(null);
              else if (isThisRoomActive) setMessages(prev => [...prev, msg]);
              loadRooms();
              window.dispatchEvent(new CustomEvent('chat-rooms-changed'));
              return;
            }
            if (isThisRoomActive) {
              setMessages(prev => [...prev, msg]);
            }
            setRooms(prev =>
              prev.map(r => r.roomId === room.roomId ? { ...r, lastMessage: msg.content } : r)
            );
            return;
          }

          // 새 메시지가 오면 목록 미리보기와 안 읽은 수를 바꾸고 그 방을 맨 위로 올림
          // (지금 열어 둔 방이면 안 읽은 수는 0)
          const isMine = msg.senderEmail === currentEmail;
          setRooms(prev => {
            const idx = prev.findIndex(r => r.roomId === room.roomId);
            if (idx === -1) return prev;
            const updated = {
              ...prev[idx],
              lastMessage: msg.type === 'IMAGE' ? '[사진]' : msg.content,
              unreadCount: isThisRoomActive ? 0 : (!isMine ? prev[idx].unreadCount + 1 : prev[idx].unreadCount),
            };
            const rest = prev.filter((_, i) => i !== idx);
            return [updated, ...rest];
          });

          // 열어 둔 방이면 메시지를 화면에 추가하고 바로 읽음 처리
          if (isThisRoomActive) {
            setMessages(prev => [...prev, msg]);
            if (!isMine) {
              markAsRead(room.roomId);
            }
          }
        }
      );
    });

    return clearAllSubscriptions;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [connected, roomsKey]);

  // 방을 열면 메시지 불러오기, 읽음 처리, 나갔던 판매자는 재입장
  const activeRoomId = activeRoom?.roomId;
  const activeRoomLeft = activeRoom?.iLeft;

  useEffect(() => {
    setRoomMenuOpen(false);
    if (!activeRoomId) return;
    // 메시지 불러오기와 읽음 처리는 REST라 웹소켓 연결과 상관없이 실행
    loadMessages(activeRoomId);
    markAsRead(activeRoomId);

    // 나갔던 판매자가 방을 다시 열면 자동 재입장
    if (activeRoomLeft) {
      fetch(`${API_BASE}/chat/rooms/${activeRoomId}/rejoin`, { method: 'POST', ...FETCH_OPTIONS })
        .then(() => { loadRooms(); loadMessages(activeRoomId); })
        .catch(e => console.error('채팅방 재입장 실패', e));
    }
  }, [activeRoomId, activeRoomLeft, connected, loadMessages, loadRooms, markAsRead]);

  // 방을 바꾸면 그 방에서 쓰던 글로 입력창 교체 (다른 방으로 잘못 보내는 것 방지)
  useEffect(() => {
    setMessage(draftsRef.current[activeRoomId] ?? '');
    // 방을 옮기면 신고 선택 해제
    setReportMode(false);
    setReportSelectedId(null);
    setReportModalOpen(false);
  }, [activeRoomId]);

  // 고른 메시지를 상대가 그사이 지우면 신고 선택 해제
  useEffect(() => {
    if (reportSelectedId == null) return;
    const selected = messages.find(m => m.id === reportSelectedId);
    if (!selected || selected.deleted) {
      setReportSelectedId(null);
      setReportModalOpen(false);
    }
  }, [messages, reportSelectedId]);

  // 구매자 화면: 방을 열면 판매자 정보를 불러와 상단에 표시
  useEffect(() => {
    setSellerProfile(null);
    if (userRole !== 'BUYER' || !activeRoom?.targetEmail) return;
    fetch(`${API_BASE}/sellers/${encodeURIComponent(activeRoom.targetEmail)}/profile`)
      .then(res => (res.ok ? res.json() : null))
      .then(p => p && setSellerProfile(p))
      .catch(() => { });
  }, [activeRoom?.roomId, activeRoom?.targetEmail, userRole]);

  // 정산 완료된 상품의 방이면 후기 작성 자격을 확인해 배너 표시 여부 결정
  useEffect(() => {
    setReviewEligibility(null);
    if (userRole !== 'BUYER' || !activeRoom?.productId || activeRoom.productStatus !== 'CLOSED_SUCCESS') return;
    fetch(`${API_BASE}/reviews/eligibility?productId=${activeRoom.productId}`, FETCH_OPTIONS)
      .then(res => (res.ok ? res.json() : null))
      .then(elig => elig && setReviewEligibility(elig))
      .catch(() => { });
  }, [activeRoom?.roomId, activeRoom?.productId, activeRoom?.productStatus, userRole]);

  // 새 메시지가 오면 대화 목록만 맨 아래로 스크롤 (페이지는 그대로)
  const scrollToBottom = useCallback(() => {
    const messageList = messagesEndRef.current?.parentElement;
    messageList?.scrollTo({ top: messageList.scrollHeight, behavior: 'smooth' });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  // 사진이 늦게 로드돼도 맨 아래를 보고 있을 때만 다시 스크롤 (이전 대화를 볼 땐 그대로)
  const handleImageLoad = useCallback(() => {
    const messageList = messagesEndRef.current?.parentElement;
    if (!messageList) return;
    const distanceFromBottom = messageList.scrollHeight - messageList.scrollTop - messageList.clientHeight;
    if (distanceFromBottom < 300) scrollToBottom();
  }, [scrollToBottom]);

  // 열어 둔 방에 상대 메시지가 오면 바로 읽음 처리 (내가 보낸 메시지는 제외)
  const lastMessageSenderEmail = messages[messages.length - 1]?.senderEmail;

  useEffect(() => {
    if (!activeRoomId || !lastMessageSenderEmail) return;
    if (lastMessageSenderEmail !== currentEmail) {
      markAsRead(activeRoomId);
    }
  }, [activeRoomId, messages.length, lastMessageSenderEmail, currentEmail, markAsRead]);

  // 지금 열어 둔 방을 채팅 알림 기능에 알려 줌 (그 방 메시지는 알림 창 생략)
  useEffect(() => {
    window.dispatchEvent(new CustomEvent('chat-active-room', { detail: { roomId: activeRoom?.roomId ?? null } }));
    return () => window.dispatchEvent(new CustomEvent('chat-active-room', { detail: { roomId: null } }));
  }, [activeRoom?.roomId]);

  // 왼쪽 목록 폭 드래그로 조절
  const handleResizeMouseDown = (e) => {
    e.preventDefault();
    isResizingRef.current = true;
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
  };

  useEffect(() => {
    const handleMouseMove = (e) => {
      if (!isResizingRef.current || !asideRef.current) return;
      const left = asideRef.current.getBoundingClientRect().left;
      const newWidth = e.clientX - left;
      setListWidth(Math.min(600, Math.max(260, newWidth)));
    };
    const handleMouseUp = () => {
      if (!isResizingRef.current) return;
      isResizingRef.current = false;
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, []);

  // 처음 들어오면 채팅방 목록 불러오기
  useEffect(() => { loadRooms(); }, [loadRooms]);

  // 알림을 눌러 ?roomId=로 들어오면 그 방을 열고, 주소에서 roomId는 지움
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

  // 메시지 전송 (웹소켓으로 /app/chat.message에 보냄)
  const handleSendMessage = (e) => {
    e.preventDefault();
    if (!message.trim() || !activeRoom || !connected || activeRoom.targetWithdrawn) return;

    stompClientRef.current.publish({
      destination: '/app/chat.message',
      body: JSON.stringify({ roomId: activeRoom.roomId, content: message.trim() }),
    });
    lastSentRef.current = { roomId: activeRoom.roomId, text: message.trim() };
    setMessage('');
    delete draftsRef.current[activeRoom.roomId];
  };

  // 채팅방 나가기 (구매자는 이전 대화 복구 불가, 판매자는 다시 열면 재입장)
  const isSeller = userRole === 'SELLER';
  // 탈퇴한 상대 표시 문구 (판매자 화면은 '탈퇴한 구매자', 구매자 화면은 '탈퇴한 판매자')
  const withdrawnLabel = isSeller ? '탈퇴한 구매자' : '탈퇴한 판매자';
  const handleLeaveRoom = async () => {
    if (!activeRoom) return;
    setRoomMenuOpen(false);
    const confirmMsg = isSeller
      ? '이 채팅방에서 나가시겠습니까?\n나가 있는 동안에는 새 메시지 알림을 받지 않습니다. (다시 열면 재입장)'
      : '이 채팅방에서 나가시겠습니까?\n나간 뒤에는 이전 대화 내용을 복구할 수 없습니다.';
    if (!(await showDialog(confirmMsg, { confirm: true }))) return;

    const leavingRoomId = activeRoom.roomId;
    try {
      const res = await fetch(`${API_BASE}/chat/rooms/${leavingRoomId}/leave`, {
        method: 'POST',
        ...FETCH_OPTIONS,
      });
      if (!res.ok) throw new Error('채팅방 나가기에 실패했습니다.');
      setActiveRoom(null);
      if (isSeller) {
        // 판매자는 방이 목록에 남음
        loadRooms();
      } else {
        // 구매자는 목록에서도 사라짐
        setRooms(prev => prev.filter(r => r.roomId !== leavingRoomId));
      }
      // 헤더 종 아이콘과 채팅 메뉴 표시도 다시 불러오도록 알림
      window.dispatchEvent(new CustomEvent('chat-rooms-changed'));
    } catch (e) {
      console.error('채팅방 나가기 실패', e);
      showDialog('채팅방 나가기에 실패했습니다.\n잠시 후 다시 시도해주세요.');
    }
  };

  // 메시지 삭제 (본인 메시지만, 양쪽 화면에서 '삭제된 메시지'로 바뀜)
  const handleDeleteMessage = async (messageId) => {
    if (!activeRoom || !messageId) return;
    if (!(await showDialog('이 메시지를 삭제할까요?\n상대방 화면에서도 "삭제된 메시지"로 표시됩니다.', { confirm: true }))) return;
    try {
      const res = await fetch(`${API_BASE}/chat/rooms/${activeRoom.roomId}/messages/${messageId}`, {
        method: 'DELETE',
        ...FETCH_OPTIONS,
      });
      if (!res.ok) throw new Error('메시지 삭제에 실패했습니다.');
      // 웹소켓 이벤트를 기다리지 않고 내 화면부터 바로 반영
      setMessages(prev => prev.map(m => m.id === messageId ? { ...m, deleted: true, content: '' } : m));
    } catch (e) {
      console.error('메시지 삭제 실패', e);
      showDialog('메시지 삭제에 실패했습니다.\n잠시 후 다시 시도해주세요.');
    }
  };

  // 신고할 수 있는 메시지: 상대가 보낸 글, 사진만 (서버 검사와 같은 기준)
  const isReportable = (msg) =>
    msg.senderEmail !== currentEmail && !msg.deleted && msg.id
    && (msg.type === 'CHAT' || msg.type === 'IMAGE');

  const handleStartReport = () => {
    setRoomMenuOpen(false);
    if (!messages.some(isReportable)) {
      showDialog('신고할 상대 메시지가 없어요.');
      return;
    }
    setReportSelectedId(null);
    setReportMode(true);
  };

  // 메시지는 1개만 선택 (다른 걸 누르면 선택이 옮겨감)
  const toggleReportSelect = (id) => {
    setReportSelectedId(current => (current === id ? null : id));
  };

  const cancelReport = () => {
    setReportMode(false);
    setReportSelectedId(null);
  };

  const handleReportSubmitted = () => {
    setReportModalOpen(false);
    cancelReport();
    showDialog('신고가 접수되었어요.\n운영진이 확인 후 조치할게요.');
  };

  // 상단 상품명을 누르면 상품 상세로 이동 (삭제된 상품이면 안내만)
  const handleGoToProduct = async () => {
    const productId = activeRoom?.productId;
    if (!productId) {
      showDialog('연결된 상품 정보가 없습니다.');
      return;
    }
    try {
      const res = await fetch(`${API_BASE}/products/${productId}`);
      if (!res.ok) throw new Error('not-found');
      navigate(`/buyer/products/${productId}`);
    } catch (e) {
      showDialog('삭제되었거나 판매가 종료된 상품입니다.');
    }
  };

  // 후기 작성 배너를 누르면 마이페이지에서 후기 작성 창을 바로 엶
  const handleGoToReview = () => {
    if (!activeRoom?.productId) return;
    navigate(`/buyer/mypage/orders?openReview=${activeRoom.productId}`);
  };

  // 사진 첨부: 먼저 업로드해서 받은 주소를 메시지로 보냄
  const handleImageButtonClick = () => {
    if (!connected || !activeRoom || activeRoom.targetWithdrawn) return;
    fileInputRef.current?.click();
  };

  const handleImageSelected = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = ''; // 같은 파일을 다시 골라도 동작하게 초기화
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
      lastSentRef.current = null; // 사진 전송 실패 시엔 입력창에 주소를 되돌리지 않음
    } catch (err) {
      console.error('이미지 전송 실패', err);
      showDialog('이미지 전송에 실패했습니다.');
    } finally {
      setUploadingImage(false);
    }
  };

  const filteredRooms = rooms
    .filter(r => !filterProductId || String(r.productId) === filterProductId)
    .filter(r => r.targetName?.includes(searchQuery) || r.productName?.includes(searchQuery));

  // 로그인 안 된 경우
  if (!currentEmail) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col">
        <V3SiteHeader />
        <div className="flex-grow flex items-center justify-center">
          <p className="text-gray-500">로그인 후 이용할 수 있습니다.</p>
        </div>
      </div>
    );
  }

  return (
    // 채팅 페이지 전용 여백 (SharedChatPage.css)
    <div className="shared-chat-page min-h-screen bg-gray-50 flex flex-col text-gray-900 h-screen">
      <V3SiteHeader />

      <div className="v3-unified-page-card is-chat">
        <header className="v3-unified-page-card__header">
          <h1 className="v3-unified-page-card__title">
            <MessageCircle size={28} /> 메시지 관리
          </h1>
        </header>

        {/* 메인 채팅 화면 (왼쪽 목록 + 오른쪽 대화창) */}
        <main className="v3-unified-page-card__body flex-grow flex-row overflow-hidden min-h-0">

          {/* 왼쪽: 채팅방 목록 (모바일은 방을 고르기 전에만 표시) */}
          <aside
            ref={asideRef}
            style={{ '--chat-list-width': `${listWidth}px` }}
            className={`shrink-0 bg-white rounded-2xl border border-gray-200 shadow-sm flex-col overflow-hidden h-full w-full md:w-[var(--chat-list-width)] md:flex ${activeRoom ? 'hidden' : 'flex'
              }`}
          >
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
                filteredRooms.map((room) => {
                  // 구매자 목록은 상품명을, 판매자 목록은 상대 이름을 제목으로 표시
                  const nameContent = (
                    <>
                      <span className={`truncate min-w-0 ${(room.targetWithdrawn || room.iLeft) ? 'text-gray-500' : ''}`}>{room.targetWithdrawn ? withdrawnLabel : room.targetName}</span>
                      {room.iLeft && (
                        <span className="shrink-0 text-[10px] font-medium text-gray-400">
                          나감
                        </span>
                      )}
                    </>
                  );
                  const productContent = (
                    <>
                      <span className={`truncate min-w-0 ${room.productDeleted ? 'line-through text-gray-500' : ''}`}>{room.productName}</span>
                      {room.productDeleted && (
                        <span className="shrink-0 text-[10px] font-medium text-gray-400">
                          삭제됨
                        </span>
                      )}
                    </>
                  );
                  const titleContent = userRole === 'BUYER' ? productContent : nameContent;
                  const subtitleContent = userRole === 'BUYER' ? nameContent : productContent;
                  // 구매자 목록 아이콘은 상품 썸네일 (없으면 기본 아이콘)
                  const showProductThumb = userRole === 'BUYER' && !!room.productImageUrl
                    && !room.productDeleted && !room.targetWithdrawn;

                  return (
                    <div
                      key={room.roomId}
                      onClick={() => setActiveRoom(room)}
                      // 판매자가 나간 방은 목록에서 흐리게 표시 (누르면 재입장)
                      className={`p-4 border-b border-gray-50 flex items-start gap-3 cursor-pointer transition ${room.iLeft ? 'opacity-50 grayscale' : ''
                        } ${activeRoom?.roomId === room.roomId ? 'bg-blue-50/50' : 'hover:bg-gray-50'
                        }`}
                    >
                      <div className={`w-10 h-10 bg-gray-200 flex items-center justify-center text-gray-500 flex-shrink-0 overflow-hidden ${userRole === 'BUYER' ? 'rounded-md' : 'rounded-full'
                        }`}>
                        {showProductThumb ? (
                          <img src={room.productImageUrl} alt="" className="w-full h-full object-cover" />
                        ) : userRole === 'BUYER' ? (
                          <Store size={18} />
                        ) : (
                          <User size={20} />
                        )}
                      </div>
                      <div className="flex flex-col flex-grow min-w-0 overflow-hidden">
                        <div className="flex justify-between items-center mb-0.5">
                          <h4 className="text-sm font-bold text-gray-900 truncate flex items-center gap-1.5 min-w-0">
                            {titleContent}
                          </h4>
                          <span className="text-[10px] text-gray-400 font-medium whitespace-nowrap ml-2 shrink-0">
                            {formatTime(room.lastSentAt)}
                          </span>
                        </div>
                        <p className="text-xs font-medium text-gray-500 truncate mb-1 flex items-center gap-1.5 min-w-0">
                          {subtitleContent}
                        </p>
                        <p className={`text-xs truncate ${room.productDeleted ? 'text-gray-400' : (room.unreadCount > 0 ? 'text-gray-900 font-bold' : 'text-gray-500')
                          }`}>
                          {room.lastMessage || '새 채팅방'}
                        </p>
                      </div>
                      {room.unreadCount > 0 && (
                        <div className="w-5 h-5 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center flex-shrink-0 mt-4">
                          {room.unreadCount}
                        </div>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          </aside>

          {/* 목록과 대화창 사이 구분선 (드래그해서 목록 폭 조절) */}
          <div
            onMouseDown={handleResizeMouseDown}
            className="hidden md:flex items-center justify-center w-4 shrink-0 cursor-col-resize group"
            title="드래그해서 목록 너비 조절"
          >
            <div className="w-1 h-10 rounded-full bg-gray-200 group-hover:bg-blue-400 transition" />
          </div>

          {/* 오른쪽: 대화창 (모바일은 방을 고른 뒤에만 표시) */}
          <section className={`flex-grow min-w-0 bg-white rounded-2xl border border-gray-200 shadow-sm flex-col overflow-hidden h-full md:flex ${activeRoom ? 'flex' : 'hidden'
            }`}>
            {!activeRoom ? (
              <div className="flex-grow flex items-center justify-center text-sm text-gray-400">
                채팅방을 선택해주세요
              </div>
            ) : (
              <>
                {/* 대화창 상단 */}
                <div className="p-4 border-b border-gray-100 flex justify-between items-center bg-white shrink-0">
                  <div className="flex items-center gap-3 overflow-hidden flex-1 min-w-0">
                    {/* 모바일 전용 뒤로가기 (목록으로) */}
                    <button
                      type="button"
                      onClick={() => setActiveRoom(null)}
                      aria-label="채팅방 목록으로"
                      className="md:hidden -ml-2 p-1 text-gray-500 hover:text-gray-800 shrink-0"
                    >
                      <ChevronLeft size={24} />
                    </button>
                    <div className="w-12 h-12 bg-gray-100 rounded-xl flex items-center justify-center text-gray-400 shrink-0 overflow-hidden">
                      {userRole === 'BUYER' && activeRoom.productImageUrl
                        && !activeRoom.productDeleted && !activeRoom.targetWithdrawn ? (
                        <img src={activeRoom.productImageUrl} alt="" className="w-full h-full object-cover" />
                      ) : (
                        <Store size={20} />
                      )}
                    </div>
                    <div className="flex flex-col min-w-0 flex-1">
                      <span className="text-[11px] font-bold text-gray-500 truncate">
                        {activeRoom.targetWithdrawn ? withdrawnLabel : activeRoom.targetName}
                      </span>
                      {/* 삭제된 상품이면 상품명 대신 안내 문구 표시 */}
                      {activeRoom.productDeleted ? (
                        <span className="text-sm font-extrabold text-gray-400 truncate" title="삭제된 공동구매입니다">
                          삭제된 공동구매입니다
                        </span>
                      ) : (
                        <button
                          onClick={handleGoToProduct}
                          title="상품 상세 보기"
                          className="group flex items-center gap-0.5 text-sm font-extrabold text-gray-900 hover:text-blue-600 hover:underline transition min-w-0"
                        >
                          <span className="truncate min-w-0">{activeRoom.productName}</span>
                          <ChevronRight size={14} className="shrink-0 text-gray-300 group-hover:text-blue-600" />
                        </button>
                      )}
                      {/* 공동구매 상태는 서버에서 받은 최신 상태로 표시 */}
                      {!activeRoom.productDeleted && (
                        <span className={`text-xs font-bold mt-0.5 ${!connected ? 'text-gray-400'
                            : activeRoom.productStatus === 'OPEN' ? 'text-blue-600' : 'text-gray-400'
                          }`}>
                          {!connected ? '연결 중...'
                            : activeRoom.productStatus === 'OPEN' ? '공동구매 진행중' : '공동구매 종료'}
                        </span>
                      )}
                    </div>
                  </div>
                  {/* 더보기 메뉴 (신고하기, 채팅방 나가기) */}
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
                          {/* 신고하기를 누르면 상대 메시지를 고르는 모드로 전환 */}
                          <button
                            onClick={handleStartReport}
                            className="w-full flex items-center gap-2 px-4 py-2.5 text-sm font-bold text-gray-700 hover:bg-gray-50 transition"
                          >
                            <Flag size={16} /> 신고하기
                          </button>
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

                {/* 안전거래 안내 배너 (판매자는 경고, 구매자는 주의 안내) */}
                <button
                  onClick={() => setSafetyOpen(true)}
                  className={`mx-4 mt-3 flex items-center gap-2.5 px-4 py-3 rounded-2xl text-xs font-bold shrink-0 transition ${userRole === 'SELLER'
                      ? 'bg-red-50 text-red-600 hover:brightness-95'
                      : 'bg-blue-50 text-blue-700 hover:bg-blue-100'
                    }`}
                >
                  {userRole === 'SELLER'
                    ? <TriangleAlert size={16} className="shrink-0 text-red-500" />
                    : <ShieldCheck size={16} className="shrink-0 text-blue-500" />}
                  <span className="flex-grow text-left">
                    {userRole === 'SELLER'
                      ? '개인 거래·선입금 요청은 제재 대상이에요'
                      : '공동구매 외 개인 거래·선입금 요청은 주의하세요'}
                  </span>
                  <ChevronRight size={16} className={`shrink-0 ${userRole === 'SELLER' ? 'text-red-400' : 'text-blue-400'}`} />
                </button>

                {/* 후기 작성 배너 (정산 완료, 결제 완료, 후기 미작성일 때만) */}
                {userRole === 'BUYER' && reviewEligibility?.eligible && !reviewEligibility?.myReview && (
                  <button
                    onClick={handleGoToReview}
                    className="mx-4 mt-2 flex items-center gap-2.5 px-4 py-3 rounded-2xl bg-amber-50 text-amber-700 text-xs font-bold shrink-0 hover:bg-amber-100 transition"
                  >
                    <Star size={16} className="shrink-0 text-amber-500" />
                    <span className="flex-grow text-left">공동구매가 성공적으로 끝났어요! 판매자에게 거래 후기를 남겨주세요</span>
                    <ChevronRight size={16} className="shrink-0 text-amber-400" />
                  </button>
                )}

                {/* 메시지 목록 */}
                <div className="flex-grow overflow-y-auto p-4 flex flex-col gap-4 bg-gray-50/30">
                  {/* 구매자 화면 상단 판매자 정보 카드 */}
                  {userRole === 'BUYER' && sellerProfile && !activeRoom.targetWithdrawn && (
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

                      // 입장, 퇴장 안내는 가운데 회색 글씨로 표시
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
                            className={`group flex max-w-[75%] ${isMe ? 'self-end flex-row-reverse' : 'self-start'} ${reportMode && isReportable(msg) ? 'cursor-pointer' : ''
                              }`}
                            // 신고 모드에선 말풍선 아무 곳이나 눌러도 선택 (사진 확대보다 먼저)
                            onClickCapture={reportMode && isReportable(msg) ? (e) => {
                              e.stopPropagation();
                              toggleReportSelect(msg.id);
                            } : undefined}
                          >
                            {/* 신고 모드일 때 상대 메시지 옆 선택 동그라미 */}
                            {reportMode && isReportable(msg) && (
                              <span
                                className={`w-5 h-5 mr-2 mt-1.5 rounded-full border-2 flex items-center justify-center shrink-0 transition ${reportSelectedId === msg.id
                                    ? 'bg-red-600 border-[#a04b3c] text-white'
                                    : 'bg-white border-gray-300'
                                  }`}
                              >
                                {reportSelectedId === msg.id && <Check size={12} strokeWidth={3} />}
                              </span>
                            )}
                            {!isMe && (
                              <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 shrink-0 mr-2">
                                <User size={16} />
                              </div>
                            )}
                            <div className={`flex flex-col gap-1 ${isMe ? 'items-end' : 'items-start'}`}>
                              {!isMe && (
                                <span className="text-[11px] font-medium text-gray-500 px-1">
                                  {activeRoom.targetWithdrawn ? withdrawnLabel : msg.senderNickname}
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
                                {/* 삭제된 메시지는 원문 대신 '삭제된 메시지' 표시 */}
                                {msg.deleted ? (
                                  <div className="p-3 rounded-2xl text-sm italic text-gray-400 bg-gray-100 border border-gray-200">
                                    메시지가 삭제되었습니다
                                  </div>
                                ) : msg.type === 'IMAGE' ? (
                                  <img
                                    src={msg.content}
                                    alt="전송된 이미지"
                                    // 사진이 로드되기 전에도 자리를 잡아 두도록 크기 고정
                                    className="w-[220px] h-[220px] rounded-2xl object-cover cursor-pointer bg-gray-100"
                                    onLoad={handleImageLoad}
                                    onClick={() => setLightboxImage(msg.content)}
                                  />
                                ) : (
                                  <div className={`p-3 rounded-2xl text-sm ${isMe
                                      ? 'bg-blue-600 text-white rounded-tr-none'
                                      : 'bg-white border border-gray-200 text-gray-800 rounded-tl-none shadow-sm'
                                    }`}>
                                    {msg.content}
                                  </div>
                                )}
                                {/* 내 메시지에만 마우스를 올리면 삭제 버튼 표시 */}
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

                {/* 메시지 입력창 (신고 모드에선 선택 안내로 바뀜) */}
                <div className="p-4 bg-white border-t border-gray-100 shrink-0">
                  {reportMode ? (
                    <div className="flex items-center gap-2 bg-red-50 p-2 pl-4 rounded-2xl border border-[#a04b3c]/15">
                      <Flag size={16} className="text-red-500 shrink-0" />
                      <span className="flex-grow text-sm font-bold text-red-600">
                        {reportSelectedId ? '메시지를 선택했어요' : '신고할 메시지를 1개 선택해주세요'}
                      </span>
                      <button
                        type="button"
                        onClick={cancelReport}
                        className="px-4 py-2.5 rounded-xl text-sm font-bold text-gray-500 bg-white hover:bg-gray-100 transition shrink-0"
                      >
                        취소
                      </button>
                      <button
                        type="button"
                        onClick={() => setReportModalOpen(true)}
                        disabled={!reportSelectedId}
                        className="px-4 py-2.5 rounded-xl text-sm font-bold text-white bg-red-600 hover:bg-red-700 transition shrink-0 disabled:opacity-40"
                      >
                        다음
                      </button>
                    </div>
                  ) : (
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
                        disabled={!connected || uploadingImage || activeRoom.targetWithdrawn}
                        className="p-2.5 text-gray-400 hover:text-gray-600 transition rounded-xl hover:bg-gray-200 shrink-0 disabled:opacity-50"
                      >
                        <ImageIcon size={20} />
                      </button>
                      <textarea
                        value={message}
                        onChange={(e) => {
                          setMessage(e.target.value);
                          draftsRef.current[activeRoom.roomId] = e.target.value;
                        }}
                        placeholder={
                          activeRoom.targetWithdrawn
                            ? '탈퇴한 상대와는 대화를 할 수 없어요'
                            : (connected ? '메시지를 입력하세요...' : '연결 중...')
                        }
                        disabled={!connected || activeRoom.targetWithdrawn}
                        className="chat-message-input flex-grow outline-none text-sm resize-none py-2.5 max-h-32 min-h-[44px] disabled:opacity-50"
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
                        disabled={!message.trim() || !connected || activeRoom.targetWithdrawn}
                        className={`p-2.5 rounded-xl transition shrink-0 flex items-center justify-center ${message.trim() && connected && !activeRoom.targetWithdrawn
                            ? 'bg-blue-600 text-white shadow-md hover:bg-blue-700'
                            : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                          }`}
                      >
                        <Send size={18} />
                      </button>
                    </form>
                  )}
                </div>
              </>
            )}
          </section>

        </main>
      </div>

      {/* 사진 크게 보기 */}
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

      {/* 안전거래 안내 창 */}
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
            <div className={`w-14 h-14 rounded-full flex items-center justify-center ${userRole === 'SELLER' ? 'bg-red-50 text-red-500' : 'bg-blue-50 text-blue-500'
              }`}>
              {userRole === 'SELLER' ? <TriangleAlert size={28} /> : <ShieldCheck size={28} />}
            </div>
            {userRole === 'SELLER' ? (
              <>
                <h3 className="text-lg font-extrabold text-gray-900 leading-snug">
                  안전한 거래를 위한<br /><span className="text-red-600">판매자 거래 원칙 안내</span>
                </h3>
                <p className="text-sm text-gray-500 leading-relaxed">
                  모든 결제는 구매자의 <b className="font-bold text-gray-700">공동구매 참여하기(N빵 탑승)</b>를 통해서만 받아야 해요.
                  채팅으로 직접 거래를 제안하거나 선입금을 요청하지 마세요.
                </p>
                <p className="text-sm text-gray-500 leading-relaxed">
                  신고가 접수되면 <b className="font-bold text-gray-700">운영진</b>이 직접 확인·조치해요.
                </p>
              </>
            ) : (
              <>
                <h3 className="text-lg font-extrabold text-gray-900 leading-snug">
                  안전한 거래를 위한<br /><span className="text-blue-600">개인 거래 주의 안내</span>
                </h3>
                <p className="text-sm text-gray-500 leading-relaxed">
                  모든 결제는 <b className="font-bold text-gray-700">공동구매 참여하기(N빵 탑승)</b>를 통해 앱 안에서 이루어져요.
                  공동구매를 거치지 않고 직접 거래하거나, 먼저 돈을 보내달라는 요청은 사기일 수 있습니다.
                </p>
                <p className="text-sm text-gray-500 leading-relaxed">
                  모든 판매자는 <b className="font-bold text-gray-700">운영진 승인</b>을 거쳐 등록돼요.
                  의심되는 요청을 받았다면 채팅방 <b className="font-bold text-gray-700">더보기 → 신고하기</b>로 알려주세요.
                  운영진이 직접 확인·조치해요.
                </p>
              </>
            )}
            <div className={`w-full border rounded-2xl p-4 text-xs leading-relaxed ${userRole === 'SELLER'
                ? 'bg-red-50 border-[#a04b3c]/15 text-red-600'
                : 'bg-gray-50 border-gray-100 text-gray-500'
              }`}>
              {/* 판매자는 개인 거래 유도 경고, 구매자는 신고 방법 안내 */}
              {userRole === 'SELLER'
                ? '계좌번호·외부 링크 등으로 개인 거래 유도를 반복하면 이용이 제한될 수 있어요.'
                : '판매자가 계좌번호·외부 링크 등으로 결제를 유도하면 응하지 말고 더보기 → 신고하기로 알려주세요. 단, 반복된 허위 신고는 이용이 제한될 수 있어요.'}
            </div>
          </div>
        </div>
      )}
      {/* 신고 작성 창 */}
      {reportModalOpen && activeRoom && messages.some(m => m.id === reportSelectedId) && (
        <ChatReportModal
          roomId={activeRoom.roomId}
          targetName={activeRoom.targetWithdrawn ? withdrawnLabel : activeRoom.targetName}
          message={messages.find(m => m.id === reportSelectedId)}
          onClose={() => setReportModalOpen(false)}
          onSubmitted={handleReportSubmitted}
        />
      )}
      {/* 브라우저 기본 alert 대신 쓰는 안내 창 */}
      {dialog && (() => {
        const [title, ...details] = dialog.message.split('\n');
        return (
          <div
            className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50"
            onClick={() => closeDialog(false)}
          >
            <div
              className="bg-white rounded-2xl shadow-2xl w-full max-w-xs mx-4 overflow-hidden"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="text-center py-8 px-6">
                <p className="text-base font-bold text-gray-900">{title}</p>
                {details.length > 0 && (
                  <p className="mt-2 text-sm text-gray-500 whitespace-pre-line">{details.join('\n')}</p>
                )}
              </div>
              <div className="flex border-t border-gray-100">
                {dialog.confirm && (
                  <button
                    type="button"
                    onClick={() => closeDialog(false)}
                    className="flex-1 py-3.5 text-sm font-medium text-gray-500 hover:bg-gray-50 transition"
                  >
                    취소
                  </button>
                )}
                <button
                  type="button"
                  autoFocus
                  onClick={() => closeDialog(true)}
                  className={`flex-1 py-3.5 text-sm font-bold text-blue-600 hover:bg-gray-50 transition ${dialog.confirm ? 'border-l border-gray-100' : ''
                    }`}
                >
                  확인
                </button>
              </div>
            </div>
          </div>
        );
      })()}
    </div>
  );
};

export default SharedChatPage;
