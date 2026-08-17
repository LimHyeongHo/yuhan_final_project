import React, { useState, useEffect } from 'react';
import { ArrowLeft, BarChart3, TrendingUp } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Header from '../../../components/layout/Header';

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
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <Header />
      
      <section className="bg-slate-900 text-white py-8 px-6 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-col gap-2 relative">
          <button 
            onClick={() => navigate('/admin/dashboard')}
            className="absolute -top-4 left-0 flex items-center gap-1 text-sm text-slate-400 hover:text-white transition"
          >
            <ArrowLeft size={16} /> 대시보드로 돌아가기
          </button>
          <div className="flex items-center gap-3 mt-6">
            <div className="w-10 h-10 rounded-full bg-emerald-500/20 flex items-center justify-center text-emerald-400">
              <BarChart3 size={20} />
            </div>
            <h2 className="text-3xl font-extrabold tracking-tight">판매자 활동 통계</h2>
          </div>
          <p className="text-slate-400 font-medium mt-1">활성 판매자들의 상품 등록 건수 및 총 누적 수익을 모니터링합니다.</p>
        </div>
      </section>

      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col gap-6">
        
        <div className="flex justify-between items-end">
          <h3 className="text-xl font-bold tracking-tight">전체 판매자 수익 데이터</h3>
          <span className="text-xs font-bold text-gray-400 bg-gray-100 px-3 py-1 rounded-full">
            총 {sellers.length}명
          </span>
        </div>

        <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
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
  );
};

export default AdminSellersStatsPage;
