// [UI-RQ-003] "내가 받은 후기" 탭 — 마이페이지 레이아웃 안에서 SellerProfileContent 렌더 (다른 탭과 스타일 통일)
import React, { useEffect, useState } from 'react';
import { Loader2, AlertTriangle } from 'lucide-react';
import SellerProfileContent from '../seller/SellerProfileContent';

const API_BASE = 'http://localhost:8080/api';

const MyPageMyReviews = () => {
  const [email, setEmail] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch(`${API_BASE}/member/info`, { credentials: 'include' })
      .then((res) => {
        if (!res.ok) throw new Error('로그인 정보를 확인할 수 없습니다.');
        return res.json();
      })
      .then((data) => {
        if (!data.email) throw new Error('이메일 정보를 찾을 수 없습니다.');
        setEmail(data.email);
      })
      .catch((err) => setError(err.message));
  }, []);

  if (error) {
    return (
      <div className="bg-white rounded-[32px] p-8 border border-gray-200 shadow-sm flex flex-col items-center justify-center gap-3 py-20 text-center">
        <AlertTriangle size={24} className="text-red-400" />
        <span className="text-sm font-bold text-gray-500">{error}</span>
      </div>
    );
  }

  if (!email) {
    return (
      <div className="bg-white rounded-[32px] p-8 border border-gray-200 shadow-sm flex flex-col items-center justify-center gap-3 py-20 text-center">
        <Loader2 size={24} className="text-gray-300 animate-spin" />
        <span className="text-sm font-bold text-gray-400">받은 후기 정보를 불러오는 중...</span>
      </div>
    );
  }

  // [UI-RQ-003] 진행한 공동구매 섹션은 "개설한 공동구매 관리" 탭과 중복이라 제외
  return <SellerProfileContent email={email} showPurchases={false} />;
};

export default MyPageMyReviews;
