import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, useLocation } from 'react-router-dom';
import './App.css';
import Header from './components/Header';
import Home from './components/Home';
import SignUp from './components/SignUp';
import Login from './components/Login';
import BuyerHome from './components/BuyerHome';
import SellerHome from './components/SellerHome';
import AdminHome from './components/AdminHome';
import Footer from './components/Footer';

// 헤더와 푸터를 조건부로 렌더링하기 위한 래퍼 컴포넌트
const Layout = ({ children }) => {
  const location = useLocation();
  // 역할별 페이지(/buyer, /seller, /admin)에서는 전역 헤더/푸터를 숨깁니다.
  const isRolePage = ['/buyer', '/seller', '/admin'].includes(location.pathname);

  return (
    <div className="bg-background text-on-background selection:bg-primary selection:text-on-primary min-h-screen font-body">
      {!isRolePage && <Header />}
      {children}
      {!isRolePage && <Footer />}
    </div>
  );
};

function App() {
  useEffect(() => {
    const script = document.createElement('script');
    script.src = "https://unpkg.com/@splinetool/viewer@1.0.91/build/spline-viewer.js";
    script.type = "module";
    document.head.appendChild(script);

    return () => {
      document.head.removeChild(script);
    };
  }, []);

  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/signup" element={<SignUp />} />
          <Route path="/login" element={<Login />} />
          <Route path="/buyer" element={<BuyerHome />} />
          <Route path="/seller" element={<SellerHome />} />
          <Route path="/admin" element={<AdminHome />} />
        </Routes>
      </Layout>
    </Router>
  );
}

export default App;
