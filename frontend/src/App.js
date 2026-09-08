import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import './App.css';

/// [신규] 인증서 만료 타이머(헤더 +5분/-5분, 00:00 만료 로그아웃) 전역 상태 Provider
import { CertificateTimerProvider } from './contexts/CertificateTimerContext';
// [feature/chat-fixes] 채팅 안읽음 배지 전역 실시간 공유 Provider (Header/Dashboard 20초 폴링 대체)
import { ChatNotificationProvider } from './contexts/ChatNotificationContext';
/// [신규] 비로그인 시 URL 직접 접근 막는 라우트 가드
import PrivateRoute from './components/PrivateRoute';
import AdminRoute from './components/AdminRoute';

import LoginPage from './pages/login/LoginPage';
import SignupPage from './pages/signup/SignupPage';
import HomePage_v2 from './pages/home/HomePage_v2';
// [UI-RQ-006][feature/ui-fixes] 이용 가이드 페이지 (로그인 불필요, 공개 라우트)
import GuidePage from './pages/guide/GuidePage';
/// [*] 관리자용 페이지 import
import SecurityLogPage from './pages/admin/security/SecurityLogPage';
import AdminSimulatorPage from './pages/admin/security/AdminSimulatorPage';
import UserAuthorization from './pages/admin/authorization/UserAuthorization';
import AdminDashboardPage from './pages/admin/dashboard/AdminDashboardPage';
import ProductsManagementPage from './pages/admin/products/ProductsManagementPage';
// ===== 26.08.31 건우 추가내용 시작 =====
import AdminSettlementPage from './pages/admin/settlement/AdminSettlementPage';
// ===== 26.08.31 건우 추가내용 끝 =====

/// [*] 판매자용 페이지 import
import SellerDashboardPage from './pages/seller/dashboard/SellerDashboardPage';
import ProductsRegisterPage from './pages/seller/products/ProductsRegisterPage';
import ProductsEditPage from './pages/seller/products/ProductsEditPage';
import SellerProductsPage from './pages/seller/products/SellerProductsPage';
import SellerAnalyticsPage from './pages/seller/analytics/SellerAnalyticsPage';
import SellerOrdersPage from './pages/seller/management/SellerOrdersPage';
import SellerProfilePage from './pages/shared/seller/SellerProfilePage';

/// [*] 구매자용 페이지 import
import BuyerProductsPage from './pages/buyer/products/BuyerProductsPage';
import BuyerProductDetailPage from './pages/buyer/products/BuyerProductDetailPage';

/// [*] 결제 페이지 import
import PaymentPage from './pages/payment/PaymentPage';
import PaymentSuccessPage from './pages/payment/PaymentSuccessPage';
import PaymentFailPage from './pages/payment/PaymentFailPage';

/// [*] 공용 페이지 import
import SharedChatPage from './pages/shared/chat/SharedChatPage';
import SharedMyPageLayout from './pages/shared/mypage/SharedMyPageLayout';
import MyPageOverview from './pages/shared/mypage/MyPageOverview';

/// [*] 구매자 마이페이지 내 세부 페이지 import
import MyPageOrder from './pages/shared/mypage/MyPageOrder';
import MyPageScrap from './pages/shared/mypage/MyPageScrap';
import MyPageSettings from './pages/shared/mypage/MyPageSettings';

/// [*] 판매자 마이페이지 내 세부 페이지 import
import MyPageProjects from './pages/shared/mypage/MyPageProjects';
import MyPageSettlement from './pages/shared/mypage/MyPageSettlement';
// [UI-RQ-003][feature/ui-fixes] 판매자 "내가 받은 후기" 진입점
import MyPageMyReviews from './pages/shared/mypage/MyPageMyReviews';



// 로컬 개발 서버(npm start)는 루트("/")에서 그대로 열리지만, GitHub Pages 배포본은
// "/yuhan_final_project" 하위 경로에서 열리기 때문에 basename이 필요하다.
// 개발 모드에서까지 이 접두사를 강제하면 localhost:3000/ 이 라우터와 안 맞아 백지 화면이 된다
// (결제 완료 후 백엔드 리다이렉트도 이 접두사 없이 오기 때문에 동일하게 백지화됨).
const routerBasename = process.env.NODE_ENV === 'production' ? process.env.PUBLIC_URL : '';

function App() {
  return (
    <BrowserRouter basename={routerBasename}>
      {/* [신규] 인증서 타이머 Provider로 전체 라우트를 감싸 어느 페이지에서도 남은 시간을 공유 */}
      <CertificateTimerProvider>
       {/* [feature/chat-fixes] 채팅 안읽음 배지를 어느 페이지에서도 실시간으로 공유 */}
       <ChatNotificationProvider>
        <Routes>
          {/* 메인 주소(localhost:3000/)로 접속했을 때 보여줄 화면 */}
          {/* <Route path="/" element={<Home />} /> */}

          {/* 로그인 주소(localhost:3000/login)로 접속했을 때 보여줄 화면 */}
          <Route path="/login" element={<LoginPage />} />

          {/* ✨ 회원가입 주소 등록 (http://localhost:3000/signup) */}
          <Route path="/signup" element={<SignupPage />} />

          {/* 홈 화면으로 접속했을 때 보여줄 화면 */}
          <Route path="/" element={<HomePage_v2 />} />
          {/* [UI-RQ-006][feature/ui-fixes] 이용 가이드 - 로그인 여부 무관 공개 라우트 */}
          <Route path="/guide" element={<GuidePage />} />

          { /* ----------------관리자 페이지-----------------*/}
          { /* 보안 로그 화면으로 접속했을 때 보여줄 화면 */}
          <Route path="/admin/security" element={<AdminRoute><SecurityLogPage /></AdminRoute>} />
          { /* 보안 검증 시뮬레이터 화면 */}
          <Route path="/admin/simulator" element={<AdminRoute><AdminSimulatorPage /></AdminRoute>} />
          { /* 회원 관리 페이지 접속 화면 */}
          <Route path="/admin/authorization" element={<AdminRoute><UserAuthorization /></AdminRoute>} />
          { /* 대시보드 페이지 접속 화면 */}
          <Route path="/admin/dashboard" element={<AdminRoute><AdminDashboardPage /></AdminRoute>} />
          { /* 상품관리 페이지 접속 화면 */}
          <Route path="/admin/products" element={<AdminRoute><ProductsManagementPage /></AdminRoute>} />
          {/* ===== 26.08.31 건우 추가내용 시작 ===== */}
          { /* 출금 신청 승인 관리 화면 접속 화면 */}
          <Route path="/admin/settlements" element={<AdminRoute><AdminSettlementPage /></AdminRoute>} />
          {/* ===== 26.08.31 건우 추가내용 끝 ===== */}
          { /* ---------------------------------------------*/}

          { /* ----------------판매자 페이지-----------------*/}
          { /* 대시보드 페이지 접속 화면 */}
          <Route path="/seller/dashboard" element={<SellerDashboardPage />} />
          { /* 상품관리 페이지 접속 화면 - 네이버 검색 api 문제로 인해 상품 검색만 임시 차단*/}
          <Route path="/seller/products" element={<ProductsRegisterPage />} />
          { /* 상품 수정 페이지 접속 화면 */}
          <Route path="/seller/products/edit/:id" element={<ProductsEditPage />} />
          { /* 상품관리 페이지 접속 화면 */}
          <Route path="/seller/status" element={<SellerProductsPage />} />
          { /* 분석데이터 페이지 접속 화면 */}
          <Route path="/seller/analytics" element={<SellerAnalyticsPage />} />
          { /* 판매자 주문/구매자 관리 페이지 */}
          <Route path="/seller/orders" element={<SellerOrdersPage />} />
          { /* 판매자 프로필(거래 후기) 페이지 */}
          <Route path="/sellers/:email" element={<SellerProfilePage />} />
          { /* ---------------------------------------------*/}

          { /* ----------------구매자 페이지-----------------*/}
          { /* 대시보드 페이지 접속 화면 */}
          <Route path="/buyer/products" element={<BuyerProductsPage />} />
          { /* 구매자 상품 상세 페이지 접속 화면 */}
          <Route path="/buyer/products/:id" element={<BuyerProductDetailPage />} />
          { /* ---------------------------------------------*/}

          { /* ----------------결제 페이지-----------------*/}
          <Route path="/payment" element={<PaymentPage />} />
          <Route path="/payment/success" element={<PaymentSuccessPage />} />
          <Route path="/payment/fail" element={<PaymentFailPage />} />
          { /* ---------------------------------------------*/}

          { /* ----------------공용 페이지(채팅 등)-----------------*/}
          { /* 판매자, 구매자 영역 채팅 페이지 */}
          { /* [수정] 비로그인 URL 직접 접근 차단을 위해 PrivateRoute로 감쌈 */}
          <Route path="/seller/chat" element={<PrivateRoute><SharedChatPage userRole="SELLER" /></PrivateRoute>} />
          <Route path="/buyer/chat" element={<PrivateRoute><SharedChatPage userRole="BUYER" /></PrivateRoute>} />

          {/* ---------------- 구매자 마이페이지 ---------------- */}
          <Route path="/buyer/mypage" element={<SharedMyPageLayout userRole="BUYER" />}>
            <Route index element={<Navigate to="overview" replace />} />
            <Route path="overview" element={<MyPageOverview userRole="BUYER" />} />
            <Route path="orders" element={<MyPageOrder userRole="BUYER" />} />
            <Route path="scrap" element={<MyPageScrap userRole="BUYER" />} />
            <Route path="settings" element={<MyPageSettings userRole="BUYER" />} />
          </Route>
          {/* ---------------- 판매자 마이페이지 ---------------- */}
          <Route path="/seller/mypage" element={<SharedMyPageLayout userRole="SELLER" />}>
            <Route index element={<Navigate to="overview" replace />} />
            <Route path="overview" element={<MyPageOverview userRole="SELLER" />} />
            <Route path="projects" element={<MyPageProjects userRole="SELLER" />} />
            {/* [UI-RQ-003][feature/ui-fixes] 내가 받은 후기 */}
            <Route path="reviews" element={<MyPageMyReviews />} />
            <Route path="settlement" element={<MyPageSettlement userRole="SELLER" />} />
            <Route path="settings" element={<MyPageSettings userRole="SELLER" />} />
          </Route>
          { /* ---------------------------------------------*/}

          {/* 나중에 전공책 상세페이지 같은 걸 추가하면 이렇게 씁니다 */}
          {/* <Route path="/book/:id" element={<BookDetail />} /> */}
        </Routes>
       </ChatNotificationProvider>
      </CertificateTimerProvider>
    </BrowserRouter>
  );
}

export default App;
