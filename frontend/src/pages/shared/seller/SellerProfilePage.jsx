// [UI-RQ-002][UI-RQ-003] 프로필+거래후기 렌더링을 SellerProfileContent로 분리 (마이페이지 탭과 공용)
import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import Header from '../../../components/layout/Header';
import SellerProfileContent from './SellerProfileContent';

const SellerProfilePage = () => {
  const { email } = useParams();
  const navigate = useNavigate();
  const sellerEmail = decodeURIComponent(email || '');

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <Header />

      <main className="flex-grow max-w-4xl w-full mx-auto p-6 md:p-8 flex flex-col gap-6 mt-4">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-sm font-bold text-gray-400 hover:text-gray-700 transition w-max"
        >
          <ArrowLeft size={16} /> 뒤로
        </button>

        <SellerProfileContent email={sellerEmail} />
      </main>
    </div>
  );
};

export default SellerProfilePage;
