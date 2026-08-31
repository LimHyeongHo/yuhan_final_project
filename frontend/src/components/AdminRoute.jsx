import React from 'react';
import { useNavigate } from 'react-router-dom';

// 관리자(role=ROLE_ADMIN)가 아니면 admin 페이지 접근을 막는 라우트 가드
const AdminRoute = ({ children }) => {
  const navigate = useNavigate();
  const isAdmin = localStorage.getItem('user_role') === 'ROLE_ADMIN';

  const goHome = () => navigate('/', { replace: true });

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
