import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useSession } from '../contexts/SessionContext';

// 로그인하지 않고 URL로 들어오면 안내 창을 띄우고, "확인"을 누르면 로그인 페이지로 보냄
// (alert는 브라우저가 막는 경우가 있어서 화면에 직접 그림)
// [UI-RQ-001] 로그인 여부를 localStorage(위변조 가능)가 아니라 서버 세션으로 확인한다.
// 서버 확인 실패/오류는 "비로그인"으로 간주(fail-closed)한다.
const PrivateRoute = ({ children, allowedRoles }) => {
  const navigate = useNavigate();
  const { session, loading } = useSession();

  const goToLogin = () => navigate('/login', { replace: true });
  const goHome = () => navigate('/', { replace: true });

  if (loading) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-white/60">
        <p className="text-sm font-medium text-gray-500">확인 중...</p>
      </div>
    );
  }

  if (!session?.authenticated) {
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
