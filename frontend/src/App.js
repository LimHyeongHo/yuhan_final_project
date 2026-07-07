import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import './App.css';

import LoginPage from './pages/login/LoginPage';
import SignupPage from './pages/signup/SignupPage';
import HomePage_v2 from './pages/home/HomePage_v2';
/// [*] 관리자용 페이지 import
import SecurityLogPage from './pages/admin/security/SecurityLogPage';
import UserAuthorization from './pages/admin/authorization/UserAuthorization';
import AdminDashboardPage from './pages/admin/dashboard/AdminDashboardPage';
import ProductsManagementPage from './pages/admin/products/ProductsManagementPage';

/// [*] 판매자용 페이지 import
import SellerDashboardPage from './pages/seller/dashboard/SellerDashboardPage';
import ProductsRegisterPage from './pages/seller/products/ProductsRegisterPage';
import SellerProductsPage from './pages/seller/products/SellerProductsPage';
import SellerAnalyticsPage from './pages/seller/analytics/SellerAnalyticsPage';

/// [*] 구매자용 페이지 import
import BuyerProductsPage from './pages/buyer/products/BuyerProductsPage';

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

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 메인 주소(localhost:3000/)로 접속했을 때 보여줄 화면 */}
        {/* <Route path="/" element={<Home />} /> */}

        {/* 로그인 주소(localhost:3000/login)로 접속했을 때 보여줄 화면 */}
        <Route path="/login" element={<LoginPage />} />
        
        {/* ✨ 회원가입 주소 등록 (http://localhost:3000/signup) */}
        <Route path="/signup" element={<SignupPage />} />

        {/* 홈 화면으로 접속했을 때 보여줄 화면 */}
        <Route path="/" element={<HomePage_v2 />} />

        { /* ----------------관리자 페이지-----------------*/}
        { /* 보안 로그 화면으로 접속했을 때 보여줄 화면 */ }
        <Route path="/admin/security" element={<SecurityLogPage />} />
        { /* 회원 관리 페이지 접속 화면 */}
        <Route path="/admin/authorization" element={<UserAuthorization />} />
        { /* 대시보드 페이지 접속 화면 */}
        <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
        { /* 상품관리 페이지 접속 화면 */}
        <Route path="/admin/products" element={<ProductsManagementPage />} />
        { /* ---------------------------------------------*/}

        { /* ----------------판매자 페이지-----------------*/}
        { /* 대시보드 페이지 접속 화면 */}
        <Route path="/seller/dashboard" element={<SellerDashboardPage />} />
        { /* 상품관리 페이지 접속 화면 */}
        <Route path="/seller/products" element={<ProductsRegisterPage />} />
        { /* 상품관리 페이지 접속 화면 */}
        <Route path="/seller/status" element={<SellerProductsPage />} />
        { /* 분석데이터 페이지 접속 화면 */}
        <Route path="/seller/analytics" element={<SellerAnalyticsPage />} />  
        { /* ---------------------------------------------*/}

        { /* ----------------구매자 페이지-----------------*/}
        { /* 대시보드 페이지 접속 화면 */}
        <Route path="/buyer/products" element={<BuyerProductsPage />} />
        { /* ---------------------------------------------*/}

        { /* ----------------결제 페이지-----------------*/}
        <Route path="/payment" element={<PaymentPage />} />
        <Route path="/payment/success" element={<PaymentSuccessPage />} />
        <Route path="/payment/fail" element={<PaymentFailPage />} />
        { /* ---------------------------------------------*/}

        { /* ----------------공용 페이지(채팅 등)-----------------*/}
        { /* 판매자, 구매자 영역 채팅 페이지 */}
        <Route path="/seller/chat" element={<SharedChatPage userRole="SELLER" />} />
        <Route path="/buyer/chat" element={<SharedChatPage userRole="BUYER" />} />
        
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
          <Route path="settlement" element={<MyPageSettlement userRole="SELLER" />} />
          <Route path="settings" element={<MyPageSettings userRole="SELLER" />} />
        </Route>
        { /* ---------------------------------------------*/}

                {/* 나중에 전공책 상세페이지 같은 걸 추가하면 이렇게 씁니다 */}
                {/* <Route path="/book/:id" element={<BookDetail />} /> */} 
      </Routes>
    </BrowserRouter>
  );
}

export default App;