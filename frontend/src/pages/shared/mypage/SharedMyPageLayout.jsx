// [UI-RQ-003][feature/ui-fixes] 판매자 사이드바에 "내가 받은 후기" 진입점 추가
import React from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { User, ShoppingBag, Bookmark, Settings, Store, CreditCard, ChevronRight, Star } from 'lucide-react';
import Header from '../../../components/layout/Header';

const SharedMyPageLayout = ({ userRole = 'BUYER' }) => {
  const basePath = userRole === 'SELLER' ? '/seller/mypage' : '/buyer/mypage';

  const buyerMenuItems = [
    { path: 'overview', label: '회원 정보 개요', icon: <User size={18} /> },
    { path: 'orders', label: '참여 중인 공동구매', icon: <ShoppingBag size={18} /> },
    { path: 'scrap', label: '관심 도서 (스크랩)', icon: <Bookmark size={18} /> },
    { path: 'settings', label: '프로필/비밀번호 수정', icon: <Settings size={18} /> },
  ];

  const sellerMenuItems = [
    { path: 'overview', label: '회원 정보 개요', icon: <User size={18} /> },
    { path: 'projects', label: '개설한 공동구매 관리', icon: <Store size={18} /> },
    { path: 'reviews', label: '내가 받은 후기', icon: <Star size={18} /> },
    { path: 'settlement', label: '정산 및 계좌 관리', icon: <CreditCard size={18} /> },
    { path: 'settings', label: '프로필/비밀번호 수정', icon: <Settings size={18} /> },
  ];

  const menuItems = userRole === 'SELLER' ? sellerMenuItems : buyerMenuItems;
  const location = useLocation();

  // 현재 활성화된 탭 확인 (기본값으로 '회원 정보 개요'를 잡도록 안전장치 추가)
  const activeMenu = menuItems.find(item => location.pathname.includes(item.path)) || menuItems[0];

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <Header />

      {/* 🚨 기존에 있던 상단 배너 영역 완전 삭제됨 */}

      {/* 메인 영역: mt-8 (위쪽 여백)을 살짝 주어 헤더와 떨어지게 만듭니다. */}
      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col md:flex-row gap-8 mt-4">
        
        {/* 좌측 사이드바 */}
        <aside className="w-full md:w-64 flex-shrink-0 flex flex-col gap-2">
          
          {/* 배너 대신 사이드바 상단에 소속(타이틀)을 알려주는 영역 추가 */}
          <div className="px-4 pb-4 mb-2 border-b border-gray-200">
            <h2 className="text-xl font-extrabold text-gray-900">마이페이지</h2>
            <span className="text-xs font-bold text-gray-400">
              {userRole === 'SELLER' ? '판매자 계정 관리' : '구매자 계정 관리'}
            </span>
          </div>

          {menuItems.map((item) => (
            <NavLink
              key={item.path}
              to={`${basePath}/${item.path}`}
              className={({ isActive }) => `flex items-center justify-between w-full p-4 rounded-2xl font-bold transition-all duration-200 ${
                isActive 
                  ? 'bg-blue-50 text-blue-600 shadow-sm ring-1 ring-blue-100/50' 
                  : 'text-gray-500 hover:bg-gray-100/70 hover:text-gray-900'
              }`}
            >
              {({ isActive }) => (
                <>
                  <div className="flex items-center gap-3">
                    {item.icon}
                    <span className="text-sm">{item.label}</span>
                  </div>
                  {isActive && <ChevronRight size={16} />}
                </>
              )}
            </NavLink>
          ))}
        </aside>

        {/* 우측 메인 콘텐츠 */}
        <section className="flex-grow flex flex-col gap-6">
          <h3 className="text-2xl font-extrabold text-gray-900 tracking-tight mb-2">
            {activeMenu?.label}
          </h3>
          <Outlet /> 
        </section>
      </main>

      <footer className="bg-white border-t border-gray-200 py-10 mt-auto">
        {/* 푸터 내용 유지 */}
      </footer>
    </div>
  );
};

export default SharedMyPageLayout;