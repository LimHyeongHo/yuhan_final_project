import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const API_BASE = `http://${window.location.hostname}:8080`;

// [신규] 비로그인 상태로 보호된 라우트에 URL 직접 접근 시 안내 모달을 보여주고, "확인" 클릭 시 로그인 페이지로 이동
// (alert는 새로고침 없는 내비게이션에서 브라우저가 조용히 막는 경우가 있어 신뢰할 수 없어 화면에 직접 렌더링하는 방식으로 대체)
// [UI-RQ-001] 로그인 여부를 localStorage(위변조 가능)가 아니라 서버 세션으로 확인한다.
// 서버 확인 실패/오류는 "비로그인"으로 간주(fail-closed)한다.
const PrivateRoute = ({ children, allowedRoles }) => {
  const navigate = useNavigate();
  const [session, setSession] = useState(null);

  useEffect(() => {
    let cancelled = false;
    fetch(`${API_BASE}/api/member/session`, { credentials: 'include' })
      .then((res) => (res.ok ? res.json() : { authenticated: false }))
      .then((data) => { if (!cancelled) setSession(data); })
      .catch(() => { if (!cancelled) setSession({ authenticated: false }); });
    return () => { cancelled = true; };
  }, []);

  const goToLogin = () => navigate('/login', { replace: true });
  const goHome = () => navigate('/', { replace: true });

  if (session === null) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-white/60">
        <p className="text-sm font-medium text-gray-500">확인 중...</p>
      </div>
    );
  }

  if (!session.authenticated) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-xs mx-4 overflow-hidden">
          <p className="text-base font-bold text-gray-900 text-center py-8 px-6">
            로그인이 필요한 서비스입니다
          </p>
          <div className="flex border-t border-gray-100">
            <button
              type="button"
              onClick={goHome}
              className="flex-1 py-3.5 text-sm font-medium text-gray-500 hover:bg-gray-50 transition"
            >
              취소
            </button>
            <button
              type="button"
              onClick={goToLogin}
              className="flex-1 py-3.5 text-sm font-bold text-blue-600 border-l border-gray-100 hover:bg-gray-50 transition"
            >
              확인
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (allowedRoles && !allowedRoles.includes(session.role)) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-xs mx-4 overflow-hidden">
          <p className="text-base font-bold text-gray-900 text-center py-8 px-6">
            접근 권한이 없는 페이지입니다
          </p>
          <div className="flex border-t border-gray-100">
            <button
              type="button"
              onClick={goHome}
              className="flex-1 py-3.5 text-sm font-bold text-blue-600 hover:bg-gray-50 transition"
            >
              확인
            </button>
          </div>
        </div>
      </div>
    );
  }

  return children;
};

export default PrivateRoute;
