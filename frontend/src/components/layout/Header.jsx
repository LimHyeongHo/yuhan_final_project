import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { User, ShieldAlert, LogOut } from 'lucide-react';
// [신규] 인증서 남은 시간(mm:ss) + +5분/-5분 조정을 위한 Context 훅
import { useCertificateTimer } from '../../contexts/CertificateTimerContext';

// [신규] 초 단위를 "mm:ss" 형식 문자열로 변환
const formatRemaining = (totalSeconds) => {
  const clamped = Math.max(totalSeconds, 0);
  const m = Math.floor(clamped / 60).toString().padStart(2, '0');
  const s = Math.floor(clamped % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
};

const Header = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [nickname, setNickname] = useState(localStorage.getItem('user_nickname') || '로그인 필요');
  const [userRole, setUserRole] = useState(localStorage.getItem('user_role') || 'ROLE_BUYER');
  // [신규] 인증서 타이머 상태 (남은 초 / 조정 함수)
  const { remainingSeconds, extend } = useCertificateTimer();

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

  const handleLogout = () => {
    // 로그아웃은 세션/로컬 정보만 정리하고 기기 인증서는 폐기하지 않음.
    // (예전엔 여기서 /api/member/certificate/revoke를 호출해 매 로그아웃마다
    //  인증서를 폐기시켰는데, 그러면 다음 로그인 때마다 재발급이 강제되는 문제가 있었음)
    localStorage.removeItem('user_nickname');
    localStorage.removeItem('user_role');
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

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-50 w-full h-16 flex items-center justify-between">

      <div className="flex items-center shrink-0 pl-[max(1.5rem,calc(50vw-40rem+1.5rem))] md:pl-[max(2rem,calc(50vw-40rem+2rem))]">
        <Link to="/" className="text-xl font-black tracking-tight text-gray-900 hover:text-blue-600 transition">
          YU-BOOK
        </Link>
      </div>

      <div className="flex items-center gap-8 pr-6 md:pr-8">

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

          {(userRole === 'ROLE_BUYER' || !userRole.startsWith('ROLE_')) && (
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
                  className={`text-xs font-mono font-bold tabular-nums w-10 text-center ${
                    remainingSeconds <= 60 ? 'text-red-600' : 'text-gray-700'
                  }`}
                  title="인증서 남은 유효시간"
                >
                  {formatRemaining(remainingSeconds)}
                </span>
                <button
                  type="button"
                  onClick={() => extend(5)}
                  className="w-6 h-6 flex items-center justify-center text-sm font-bold text-gray-500 hover:text-blue-600 hover:bg-gray-100 rounded-full transition"
                  title="인증서 유효시간 5분 증가"
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
                  className={`text-[10px] font-black tracking-wider uppercase transition ${
                    userRole === 'ROLE_ADMIN' ? 'text-red-600' : userRole === 'ROLE_SELLER' ? 'text-emerald-600' : 'text-blue-600'
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
    </header>
  );
};

export default Header;
