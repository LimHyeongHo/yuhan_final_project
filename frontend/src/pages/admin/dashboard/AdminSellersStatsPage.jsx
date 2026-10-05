import React, { useState, useEffect } from 'react';
import { ArrowLeft, BarChart3, TrendingUp } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import AdminHeader from '../../../components/admin/AdminHeader';

const AdminSellersStatsPage = () => {
  const navigate = useNavigate();
  const [sellers, setSellers] = useState([]);

  useEffect(() => {
    fetch('http://localhost:8080/api/admin/sellers/stats-list', { credentials: 'include' })
      .then(res => res.json())
      .then(data => setSellers(data))
      .catch(err => console.error("데이터 로드 실패:", err));
  }, []);

  return (
    <div className="v3-page admin-v3-page min-h-screen flex flex-col text-gray-900">
      <AdminHeader />
      
      <div className="v3-unified-page-card">
        <header className="v3-unified-page-card__header">
          <h1 className="v3-unified-page-card__title"><BarChart3 size={28} />판매자 활동 통계</h1>
          <button 
            onClick={() => navigate('/admin/dashboard')}
            className="flex items-center gap-1 text-sm font-bold text-gray-600 transition hover:text-gray-900"
          >
            <ArrowLeft size={16} /> 대시보드로 돌아가기
          </button>
        </header>

        <main className="v3-unified-page-card__body admin-v3-main flex-grow">
        
        <div className="flex justify-between items-end">
          <h3 className="text-xl font-bold tracking-tight">전체 판매자 수익 데이터</h3>
          <span className="text-xs font-bold text-gray-400 bg-gray-100 px-3 py-1 rounded-full">
            총 {sellers.length}명
          </span>
        </div>

        <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200 text-gray-500 text-sm">
                <th className="py-4 px-6 font-bold">아이디 (이메일)</th>
                <th className="py-4 px-6 font-bold">닉네임</th>
                <th className="py-4 px-6 font-bold text-right">누적 등록 상품 수</th>
                <th className="py-4 px-6 font-bold text-right">총 수익 (원)</th>
              </tr>
            </thead>
            <tbody>
              {sellers.length === 0 ? (
                <tr>
                  <td colSpan="4" className="py-8 text-center text-gray-400 font-bold">활성 판매자가 없습니다.</td>
                </tr>
              ) : (
                sellers.map((seller, idx) => (
                  <tr key={idx} className="border-b border-gray-50 hover:bg-gray-50/50 transition">
                    <td className="py-4 px-6 text-gray-900 font-semibold">{seller.email}</td>
                    <td className="py-4 px-6 text-gray-600">{seller.nickname}</td>
                    <td className="py-4 px-6 text-gray-700 text-right font-bold">{seller.productCount} 건</td>
                    <td className="py-4 px-6 text-emerald-600 text-right font-black flex justify-end items-center gap-1">
                      {seller.totalRevenue > 0 && <TrendingUp size={14} className="text-emerald-500" />}
                      {seller.totalRevenue.toLocaleString()} 원
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        </main>
      </div>
    </div>
  );
};

export default AdminSellersStatsPage;
