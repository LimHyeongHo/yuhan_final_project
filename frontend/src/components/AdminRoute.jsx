import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const API_BASE = `http://${window.location.hostname}:8080`;

// 관리자(role=ROLE_ADMIN)가 아니면 admin 페이지 접근을 막는 라우트 가드
// [UI-RQ-001] 역할을 localStorage(위변조 가능)가 아니라 서버 세션에서 조회해 판단한다.
// 서버 확인 실패/오류는 "권한 없음"으로 간주(fail-closed)한다.
const AdminRoute = ({ children }) => {
  const navigate = useNavigate();
  // null = 확인 중, true/false = 서버 세션 확인 결과
  const [isAdmin, setIsAdmin] = useState(null);

  useEffect(() => {
    let cancelled = false;
    fetch(`${API_BASE}/api/member/session`, { credentials: 'include' })
      .then((res) => (res.ok ? res.json() : { role: null }))
      .then((data) => { if (!cancelled) setIsAdmin(data.role === 'ROLE_ADMIN'); })
      .catch(() => { if (!cancelled) setIsAdmin(false); });
    return () => { cancelled = true; };
  }, []);

  const goHome = () => navigate('/', { replace: true });

  if (isAdmin === null) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-white/60">
        <p className="text-sm font-medium text-gray-500">확인 중...</p>
      </div>
    );
  }

  if (!isAdmin) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-xs mx-4 overflow-hidden">
          <p className="text-base font-bold text-gray-900 text-center py-8 px-6">
            관리자만 접근할 수 있는 페이지입니다
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

export default AdminRoute;
