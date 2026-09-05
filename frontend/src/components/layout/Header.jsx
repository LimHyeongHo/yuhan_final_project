import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
// [신규] Bell — 헤더 채팅 알림 배지/미리보기 아이콘
import { User, ShieldAlert, LogOut, Bell, X, Menu } from 'lucide-react';
// [신규] 인증서 남은 시간(mm:ss) + +5분/-5분 조정을 위한 Context 훅
import { useCertificateTimer } from '../../contexts/CertificateTimerContext';

// [신규] 초 단위를 "mm:ss" 형식 문자열로 변환
const formatRemaining = (totalSeconds) => {
  const clamped = Math.max(totalSeconds, 0);
  const m = Math.floor(clamped / 60).toString().padStart(2, '0');
  const s = Math.floor(clamped % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
};

// [신규] +버튼으로 늘릴 수 있는 상한 (서버 CertificateSessionService.MAX_VALID_MINUTES와 동일하게 60분)
const MAX_REMAINING_SECONDS = 60 * 60;

const Header = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [nickname, setNickname] = useState(localStorage.getItem('user_nickname') || '로그인 필요');
  const [userRole, setUserRole] = useState(localStorage.getItem('user_role') || 'ROLE_BUYER');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  // [신규] 인증서 타이머 상태 (남은 초 / 조정 함수)
  const { remainingSeconds, extend } = useCertificateTimer();
  // [신규] 헤더 알림 배지 + 미리보기 드롭다운용 상태 (채팅 + 판매자 참여 알림 통합)
  const [chatRooms, setChatRooms] = useState([]);
  const [participationAlerts, setParticipationAlerts] = useState([]);
  const [systemNotifications, setSystemNotifications] = useState([]);
  const [isChatPreviewOpen, setIsChatPreviewOpen] = useState(false);
  // [신규] 참여 알림 "읽음" 기준 시각 (로컬 저장)
  const [lastSeenParticipationAt, setLastSeenParticipationAt] = useState(
    () => localStorage.getItem('notif_participation_seen_at') || new Date(0).toISOString()
  );

  const syncFromStorage = () => {
    const storedNickname = localStorage.getItem('user_nickname');
    const storedRole = localStorage.getItem('user_role');
    if (storedNickname) setNickname(storedNickname);
    else setNickname('로그인 필요');
    if (storedRole) setUserRole(storedRole);
    else setUserRole('ROLE_BUYER');
  };

  useEffect(() => {
    syncFromStorage();
  }, [location]);

  // [신규] 같은 페이지에 머문 채(라우트 이동 없이) 닉네임 등이 바뀐 경우, 새로고침 없이 헤더에 즉시 반영
  useEffect(() => {
    window.addEventListener('user-profile-updated', syncFromStorage);
    return () => window.removeEventListener('user-profile-updated', syncFromStorage);
  }, []);

  // [신규] 읽음 처리 이벤트 받으면 배지 즉시 갱신
  useEffect(() => {
    const handleChatRead = (e) => {
      const { roomId } = e.detail || {};
      if (roomId == null) return;
      setChatRooms(prev => prev.map(r => r.roomId === roomId ? { ...r, unreadCount: 0 } : r));
    };
    window.addEventListener('chat-read', handleChatRead);
    return () => window.removeEventListener('chat-read', handleChatRead);
  }, []);

  // [신규] 채팅방 목록(+ 판매자면 참여 알림)을 20초마다 갱신
  useEffect(() => {
    if (!localStorage.getItem('user_nickname')) {
      setChatRooms([]);
      setParticipationAlerts([]);
      return;
    }

    const loadNotifications = () => {
      fetch(`http://${window.location.hostname}:8080/api/chat/rooms`, { credentials: 'include' })
        .then(res => {
          if (!res.ok) throw new Error('Failed to fetch chat rooms');
          return res.json();
        })
        .then(setChatRooms)
        .catch(err => console.error('헤더 채팅 알림 로드 실패', err));

      // [신규] 참여 알림 및 시스템 알림은 판매자 계정에만 의미 있는 이벤트라 seller일 때만 조회
      if (localStorage.getItem('user_role') === 'ROLE_SELLER') {
        fetch(`http://${window.location.hostname}:8080/api/products/seller/me/participations`, { credentials: 'include' })
          .then(res => {
            if (!res.ok) throw new Error('Failed to fetch participations');
            return res.json();
          })
          .then(setParticipationAlerts)
          .catch(err => console.error('헤더 참여 알림 로드 실패', err));

        fetch(`http://${window.location.hostname}:8080/api/seller/notifications`, { credentials: 'include' })
          .then(res => {
            if (!res.ok) throw new Error('Failed to fetch system notifications');
            return res.json();
          })
          .then(setSystemNotifications)
          .catch(err => console.error('헤더 시스템 알림 로드 실패', err));
      } else {
        setParticipationAlerts([]);
        setSystemNotifications([]);
      }
    };

    loadNotifications();
    const intervalId = setInterval(loadNotifications, 20000);
    return () => clearInterval(intervalId);
  }, [nickname]);

  const handleLogout = async () => {
    // [SEC-RQ-002] 서버 세션도 함께 무효화해야 뒤로가기/재요청으로 보호 API에 재진입할 수 없음.
    // 인증서 폐기는 탈퇴/타이머 만료 때만 해야 하므로 여기서는 로그아웃 API만 호출한다.
    try {
      await fetch(`http://${window.location.hostname}:8080/api/pki/logout`, {
        method: 'POST',
        credentials: 'include',
      });
    } catch (err) {
      console.error('서버 로그아웃 요청 실패', err);
    }

    localStorage.removeItem('user_nickname');
    localStorage.removeItem('user_role');
    // [신규] 채팅 메시지 판별 email 삭제
    localStorage.removeItem('email');
    setNickname('로그인 필요');
    setUserRole('ROLE_BUYER');
    alert('로그아웃 되었습니다.');
    navigate('/login');
  };

  const myPagePath = userRole === 'ROLE_SELLER' ? '/seller/mypage' : userRole === 'ROLE_ADMIN' ? '/admin/dashboard' : '/buyer/mypage';

  // [신규] 헤더 채팅 링크 클릭 시 비로그인이면 이동 막고 alert만 표시 (URL 직접 입력 접근은 미차단, PrivateRoute에서 별도 처리 예정)
  const handleChatClick = (e) => {
    if (!localStorage.getItem('user_nickname')) {
      e.preventDefault();
      alert('로그인이 필요합니다');
    }
  };

  // [신규] 알림 배지/미리보기 — role별 채팅 목록 경로, 채팅+참여 알림을 하나로 합쳐서 최신순 표시
  const chatBasePath = userRole === 'ROLE_SELLER' ? '/seller/chat' : '/buyer/chat';

  const unreadChatItems = chatRooms
    .filter(r => r.unreadCount > 0)
    .map(r => ({
      type: 'CHAT',
      key: `chat-${r.roomId}`,
      roomId: r.roomId,
      title: r.targetName,
      subtitle: r.productName,
      preview: r.lastMessage || '새 메시지',
      time: r.lastSentAt,
      badge: r.unreadCount,
    }));

  const newParticipationItems = participationAlerts
    .filter(p => new Date(p.joinDate) > new Date(lastSeenParticipationAt))
    .map(p => ({
      type: 'PARTICIPATION',
      key: `participation-${p.id}`,
      title: `구매자 '${p.buyerName}'님이 참여했습니다`,
      subtitle: p.product?.title,
      preview: null,
      time: p.joinDate,
    }));

  const newSystemNotificationItems = systemNotifications
    .filter(n => new Date(n.createdAt) > new Date(lastSeenParticipationAt))
    .map(n => ({
      type: 'SYSTEM',
      key: `system-${n.id}`,
      title: '시스템 알림 (상품 상태 변경)',
      subtitle: n.message,
      preview: null,
      time: n.createdAt,
    }));

  const notificationItems = [...unreadChatItems, ...newParticipationItems, ...newSystemNotificationItems]
    .sort((a, b) => new Date(b.time) - new Date(a.time))
    .slice(0, 6);

  const totalUnreadCount = unreadChatItems.reduce((sum, item) => sum + item.badge, 0) + newParticipationItems.length + newSystemNotificationItems.length;

  // [신규] 닫히는 모든 경로(벨 재클릭/항목 클릭/전체보기 클릭)에서 공통으로 호출
  const markNotificationsSeen = () => {
    const now = new Date().toISOString();
    localStorage.setItem('notif_participation_seen_at', now);
    setLastSeenParticipationAt(now);
  };

  const handleToggleNotifications = () => {
    if (isChatPreviewOpen) markNotificationsSeen();
    setIsChatPreviewOpen(!isChatPreviewOpen);
  };

  // [신규] 항목 클릭 시 이동 — 채팅은 해당 방, 참여 알림은 판매 현황, 시스템 알림은 대시보드
  const handleNotificationItemClick = (item) => {
    markNotificationsSeen();
    setIsChatPreviewOpen(false);
    if (item.type === 'CHAT') {
      navigate(`${chatBasePath}?roomId=${item.roomId}`);
    } else if (item.type === 'SYSTEM') {
      navigate('/seller/dashboard');
    } else {
      navigate('/seller/status');
    }
  };

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-50 w-full h-16 flex items-center justify-between">

      <div className="flex items-center shrink-0 pl-[max(1.5rem,calc(50vw-40rem+1.5rem))] md:pl-[max(2rem,calc(50vw-40rem+2rem))]">
        <Link to="/" className="text-xl font-black tracking-tight text-gray-900 hover:text-blue-600 transition">
          YU-BOOK
        </Link>
      </div>

      <div className="flex items-center gap-4 md:gap-8 pr-4 md:pr-8">

        {/* [+] 햄버거 버튼 (모바일 전용) */}
        <button
          className="md:hidden p-2 text-gray-600 hover:text-gray-900"
          onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
        >
          {isMobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>

        {/* [+] 데스크탑 네비게이션 */}
        <nav className="hidden md:flex items-center gap-6 text-sm font-bold text-gray-500">
          {userRole === 'ROLE_ADMIN' && (
            <>
              <Link to="/admin/dashboard" className="hover:text-red-600 transition">관리자 홈</Link>
              <Link to="/admin/authorization" className="hover:text-red-600 transition">회원 관리</Link>
              <Link to="/admin/products" className="hover:text-red-600 transition">상품 관리</Link>
              <Link to="/admin/security" className="hover:text-red-600 transition">보안 로그</Link>
            </>
          )}

          {userRole === 'ROLE_SELLER' && (
            <>
              <Link to="/seller/dashboard" className="hover:text-gray-950 transition">대시보드</Link>
              <Link to="/seller/products" className="hover:text-gray-950 transition">물품 등록</Link>
              <Link to="/seller/status" className="hover:text-gray-950 transition">판매 현황</Link>
              <Link to="/seller/analytics" className="hover:text-gray-950 transition">분석 데이터</Link>
              {/* [수정] onClick 추가 — 비로그인 시 handleChatClick이 이동 막고 alert 표시 */}
              <Link to="/seller/chat" className="hover:text-gray-950 transition" onClick={handleChatClick}>채팅</Link>
            </>
          )}

          {/* [SEC-RQ-003] ROLE_SELLER_PENDING(승인 대기 판매자)은 아직 SELLER가 아니므로 구매자와 같은 탐색 메뉴를 보여준다 */}
          {(userRole === 'ROLE_BUYER' || userRole === 'ROLE_SELLER_PENDING' || !userRole.startsWith('ROLE_')) && (
            <>
              <Link to="/" className="hover:text-gray-950 transition">홈</Link>
              <Link to="/buyer/products" className="hover:text-gray-950 transition">공구 찾기</Link>
              {/* [수정] onClick 추가 — 비로그인 시 handleChatClick이 이동 막고 alert 표시 */}
              <Link to="/buyer/chat" className="hover:text-gray-950 transition" onClick={handleChatClick}>채팅</Link>
            </>
          )}
        </nav>

        <div className="hidden md:block w-px h-4 bg-gray-200"></div>

        {localStorage.getItem('user_nickname') ? (
          <>
            {/* [신규] 알림 배지 + 미리보기 드롭다운 */}
            <div className="relative shrink-0">
              <button
                type="button"
                onClick={handleToggleNotifications}
                className="relative p-2 text-gray-400 hover:text-gray-700 hover:bg-gray-100 rounded-xl transition"
                title="알림"
              >
                <Bell size={20} />
                {totalUnreadCount > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 min-w-[16px] h-4 px-1 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center">
                    {totalUnreadCount > 9 ? '9+' : totalUnreadCount}
                  </span>
                )}
              </button>

              {isChatPreviewOpen && (
                <div className="absolute right-0 top-full mt-2 w-80 bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden z-50">
                  <div className="px-4 py-3 border-b border-gray-100">
                    <span className="text-sm font-bold text-gray-900">알림</span>
                  </div>
                  <div className="max-h-80 overflow-y-auto">
                    {notificationItems.length === 0 ? (
                      <div className="px-4 py-6 text-center text-sm text-gray-400">새 알림이 없습니다</div>
                    ) : (
                      notificationItems.map(item => (
                        <button
                          key={item.key}
                          type="button"
                          onClick={() => handleNotificationItemClick(item)}
                          className="w-full text-left px-4 py-3 border-b border-gray-50 last:border-b-0 hover:bg-gray-50 transition flex flex-col gap-0.5"
                        >
                          <div className="flex justify-between items-center gap-2">
                            <span className="text-sm font-bold text-gray-900 truncate">{item.title}</span>
                            {item.badge > 0 && (
                              <span className="w-5 h-5 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center shrink-0">
                                {item.badge}
                              </span>
                            )}
                          </div>
                          {item.subtitle && (
                            <span className="text-xs text-gray-400 font-medium truncate">{item.subtitle}</span>
                          )}
                          {item.preview && (
                            <span className="text-xs text-gray-600 truncate">{item.preview}</span>
                          )}
                        </button>
                      ))
                    )}
                  </div>
                  <Link
                    to={
                      userRole === 'ROLE_SELLER' ? '/seller/dashboard'
                        : userRole === 'ROLE_ADMIN' ? '/admin/security'
                          : chatBasePath
                    }
                    onClick={() => { markNotificationsSeen(); setIsChatPreviewOpen(false); }}
                    className="block text-center py-2.5 text-xs font-bold text-blue-600 hover:bg-blue-50 transition border-t border-gray-100"
                  >
                    전체 알림 보기
                  </Link>
                </div>
              )}
            </div>

            {/* [신규] 인증서 남은 시간 표시 + +5분/-5분 조정 버튼 (인증서 타이머 세션이 있을 때만 표시) */}
            {remainingSeconds !== null && (
              <div className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-gray-50 border border-gray-200 shrink-0">
                <button
                  type="button"
                  onClick={() => extend(-5)}
                  className="w-6 h-6 flex items-center justify-center text-sm font-bold text-gray-500 hover:text-red-600 hover:bg-gray-100 rounded-full transition"
                  title="인증서 유효시간 5분 감소"
                >
                  -
                </button>
                <span
                  className={`text-xs font-mono font-bold tabular-nums w-10 text-center ${remainingSeconds <= 60 ? 'text-red-600' : 'text-gray-700'
                    }`}
                  title="인증서 남은 유효시간"
                >
                  {formatRemaining(remainingSeconds)}
                </span>
                <button
                  type="button"
                  onClick={() => extend(5)}
                  disabled={remainingSeconds >= MAX_REMAINING_SECONDS}
                  className="w-6 h-6 flex items-center justify-center text-sm font-bold text-gray-500 hover:text-blue-600 hover:bg-gray-100 rounded-full transition disabled:opacity-30 disabled:hover:text-gray-500 disabled:hover:bg-transparent disabled:cursor-not-allowed"
                  title={remainingSeconds >= MAX_REMAINING_SECONDS ? '최대 60분까지 늘릴 수 있습니다.' : '인증서 유효시간 5분 증가'}
                >
                  +
                </button>
              </div>
            )}

            <Link
              to={myPagePath}
              className="flex items-center gap-3 hover:bg-gray-50 px-3 py-1.5 rounded-2xl transition cursor-pointer group shrink-0"
            >
              <div className="flex flex-col text-right">
                <span className="text-sm font-extrabold text-gray-900 group-hover:text-blue-600 transition">
                  {nickname} 님
                </span>
                <span
                  className={`text-[10px] font-black tracking-wider uppercase transition ${userRole === 'ROLE_ADMIN' ? 'text-red-600' : userRole === 'ROLE_SELLER' ? 'text-emerald-600' : 'text-blue-600'
                    }`}
                >
                  {userRole.replace('ROLE_', '')} MODE
                </span>
              </div>

              <div className="w-9 h-9 rounded-full bg-gray-100 border border-gray-200 flex items-center justify-center text-gray-400 group-hover:border-blue-300 transition shadow-inner overflow-hidden">
                {userRole === 'ROLE_ADMIN' ? <ShieldAlert size={18} className="text-red-500" /> : <User size={18} className="group-hover:text-blue-500" />}
              </div>
            </Link>

            <button
              onClick={handleLogout}
              className="p-2 text-gray-400 hover:text-red-500 transition-colors"
              title="로그아웃"
            >
              <LogOut size={20} />
            </button>
          </>
        ) : (
          <div className="flex items-center gap-2">
            <Link
              to="/login"
              className="px-4 py-2 text-sm font-bold text-gray-600 hover:text-blue-600 transition"
            >
              로그인
            </Link>
            <Link
              to="/signup"
              className="px-4 py-2 text-sm font-bold text-white bg-blue-600 rounded-xl hover:bg-blue-700 transition shadow-sm"
            >
              회원가입
            </Link>
          </div>
        )}

      </div>

      {/* [+] 모바일 햄버거 메뉴 드롭다운 */}
      {isMobileMenuOpen && (
        <div className="absolute top-16 left-0 w-full bg-white border-b border-gray-200 shadow-lg flex flex-col p-4 gap-4 md:hidden z-40">
          <nav className="flex flex-col gap-4 text-sm font-bold text-gray-700">
            {userRole === 'ROLE_ADMIN' && (
              <>
                <Link to="/admin/dashboard" onClick={() => setIsMobileMenuOpen(false)}>관리자 홈</Link>
                <Link to="/admin/authorization" onClick={() => setIsMobileMenuOpen(false)}>회원 관리</Link>
                <Link to="/admin/products" onClick={() => setIsMobileMenuOpen(false)}>상품 관리</Link>
                <Link to="/admin/security" onClick={() => setIsMobileMenuOpen(false)}>보안 로그</Link>
              </>
            )}

            {userRole === 'ROLE_SELLER' && (
              <>
                <Link to="/seller/dashboard" onClick={() => setIsMobileMenuOpen(false)}>대시보드</Link>
                <Link to="/seller/products" onClick={() => setIsMobileMenuOpen(false)}>물품 등록</Link>
                <Link to="/seller/status" onClick={() => setIsMobileMenuOpen(false)}>판매 현황</Link>
                <Link to="/seller/analytics" onClick={() => setIsMobileMenuOpen(false)}>분석 데이터</Link>
                <Link to="/seller/chat" onClick={(e) => { handleChatClick(e); setIsMobileMenuOpen(false); }}>채팅</Link>
              </>
            )}

            {/* [SEC-RQ-003] ROLE_SELLER_PENDING(승인 대기 판매자)은 아직 SELLER가 아니므로 구매자와 같은 탐색 메뉴를 보여준다 */}
          {(userRole === 'ROLE_BUYER' || userRole === 'ROLE_SELLER_PENDING' || !userRole.startsWith('ROLE_')) && (
              <>
                <Link to="/" onClick={() => setIsMobileMenuOpen(false)}>홈</Link>
                <Link to="/buyer/products" onClick={() => setIsMobileMenuOpen(false)}>공구 찾기</Link>
                <Link to="/buyer/chat" onClick={(e) => { handleChatClick(e); setIsMobileMenuOpen(false); }}>채팅</Link>
              </>
            )}
          </nav>
        </div>
      )}
    </header>
  );
};

export default Header;
