import React, { useState, useEffect } from 'react';
import { Store, BarChart3, TrendingUp, Users, Eye, ArrowUpRight, DollarSign, Calendar, Wallet } from 'lucide-react';
import Header from '../../../components/layout/Header';

const SellerAnalyticsPage = () => {
  const [timeRange, setTimeRange] = useState('7D'); 
  const [analyticsData, setAnalyticsData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const userId = localStorage.getItem('user_id') || '1'; // 임시 기본값 지원
    // [SEC-RQ-001] 세션 쿠키를 안 보내면 로그인 요구 정책에 걸려 401이 난다.
    fetch(`http://localhost:8080/api/seller/${userId}/analytics`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        setAnalyticsData(data);
        setLoading(false);
      })
      .catch(err => {
        console.error('Failed to fetch analytics', err);
        setLoading(false);
      });
  }, []);

  if (loading || !analyticsData) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col">
        <Header />
        <div className="flex-grow flex items-center justify-center">
          <span className="text-gray-500 font-bold">분석 데이터를 불러오는 중입니다...</span>
        </div>
      </div>
    );
  }

  const currentData = analyticsData.filterData[timeRange];
  const totalCumulativeRevenue = analyticsData.totalCumulativeRevenue;
  const maxRevenue = Math.max(...currentData.chart.map(d => d.revenue));
  const mockTopProducts = analyticsData.topProducts;

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      
      {/* 1. 글로벌 헤더 */}
      <Header />

      {/* 2. 상단 다크 배너 (Seller Hub 패밀리 룩) */}
      <section className="bg-slate-900 text-white py-12 px-6 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-col gap-2">
          <span className="bg-emerald-500/20 text-emerald-400 text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full w-max border border-emerald-500/30 flex items-center gap-1.5">
            <BarChart3 size={14} /> Analytics Hub
          </span>
          <h2 className="text-4xl font-extrabold tracking-tight mt-1">
            분석 데이터
          </h2>
          <p className="text-slate-400 font-medium text-base max-w-2xl mt-1">
            내 프로젝트의 방문자 유입 추이와 매출 데이터를 상세하게 분석하여 다음 공동구매 전략을 세워보세요.
          </p>
        </div>
      </section>

      {/* 3. 메인 콘텐츠 영역 */}
      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col gap-8">
        
        {/* ✨ 컨트롤 패널 (기간 설정 필터 버튼) */}
        <div className="flex justify-between items-center bg-white p-4 rounded-2xl border border-gray-200 shadow-sm">
          <div className="flex items-center gap-2">
            <Calendar size={18} className="text-gray-400" />
            <span className="text-sm font-bold text-gray-700">조회 기간 설정</span>
          </div>
          <div className="flex gap-2 bg-gray-100 p-1 rounded-xl">
            {['7D', '1M', '3M'].map((tabId) => (
              <button
                key={tabId}
                onClick={() => setTimeRange(tabId)}
                className={`px-4 py-2 text-xs font-bold rounded-lg transition ${
                  timeRange === tabId 
                    ? 'bg-white text-blue-600 shadow-sm ring-1 ring-gray-900/5' 
                    : 'text-gray-600 hover:bg-gray-200/50'
                }`}
              >
                {analyticsData.filterData[tabId].label}
              </button>
            ))}
          </div>
        </div>

        {/* ✨ 상단 3종 요약 통계 카드 */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* 카드 1: 선택 기간 누적 조회수 (동적) */}
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center transition-all duration-300">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">{currentData.label} 조회수</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">{currentData.summary.views} 회</h3>
              <span className="text-emerald-600 text-xs font-bold mt-1 flex items-center gap-0.5">
                <ArrowUpRight size={14} /> 안정적 유입 유지
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center text-blue-600">
              <Eye size={24} />
            </div>
          </div>

          {/* 카드 2: 선택 기간 총 결제액 (동적) */}
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center transition-all duration-300 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-50 rounded-full -mr-6 -mt-6 opacity-50 pointer-events-none"></div>
            <div className="flex flex-col gap-1 relative z-10">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">{currentData.label} 결제액</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">₩{currentData.summary.revenue}</h3>
              <span className="text-emerald-600 text-xs font-bold mt-1 flex items-center gap-0.5">
                <ArrowUpRight size={14} /> +34.2% 성장
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-600 relative z-10">
              <DollarSign size={24} />
            </div>
          </div>

          {/* 🚀 카드 3: 총 누적 판매 수익 (고정 데이터) */}
          <div className="bg-white rounded-[24px] p-6 shadow-md flex justify-between items-center relative overflow-hidden group">
            {/* 배경 장식 효과 */}
            <div className="absolute -right-4 -bottom-4 w-32 h-32 bg-indigo-500/20 rounded-full blur-2xl group-hover:bg-indigo-500/30 transition-all"></div>
            
            <div className="flex flex-col gap-1 relative z-10">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">총 누적 판매 수익</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">₩{totalCumulativeRevenue}</h3>
              <span className="text-slate-400 text-xs font-bold mt-1 flex items-center gap-1">
                서비스 가입 이후 전체 수익
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-indigo-500/20 flex items-center justify-center text-indigo-400 relative z-10">
              <Wallet size={24} />
            </div>
          </div>
        </section>

        {/* 하단 스플릿 레이아웃 (차트 영역 2/3 + 랭킹 영역 1/3) */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* ✨ 좌측: 트렌드 차트 (필터 연동형 동적 렌더링) */}
          <div className="lg:col-span-2 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col gap-6">
            <div className="flex justify-between items-center border-b border-gray-100 pb-4">
              <div>
                <h3 className="text-xl font-extrabold text-gray-950 tracking-tight">{currentData.label} 매출 트렌드</h3>
                <p className="text-xs text-gray-400 mt-1">선택한 기간 동안의 수익 변화 추이를 확인하세요.</p>
              </div>
              <span className="bg-blue-50 text-blue-600 text-xs font-bold px-3 py-1.5 rounded-lg flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-blue-500"></span> 매출액 (₩)
              </span>
            </div>

            <div className="h-64 flex items-end gap-4 pt-4 border-b border-gray-100 px-2 mt-4 relative">
              {/* Y축 보조선 (가이드라인) */}
              <div className="absolute inset-0 flex flex-col justify-between pointer-events-none pb-8">
                <div className="border-t border-gray-100 border-dashed w-full"></div>
                <div className="border-t border-gray-100 border-dashed w-full"></div>
                <div className="border-t border-gray-100 border-dashed w-full"></div>
              </div>

              {/* 막대 그래프 렌더링 */}
              {currentData.chart.map((data, idx) => {
                // 데이터 비율 계산 (최대 95%까지만 올라가도록 UI 조정)
                const heightPercentage = Math.max((data.revenue / maxRevenue) * 95, 5); 
                
                // 오늘/이번달 등 가장 최근 데이터를 시각적으로 강조
                const isHighlight = idx === currentData.chart.length - 1 || (timeRange === '7D' && idx === 4); 

                return (
                  <div key={`${timeRange}-${idx}`} className="flex-grow flex flex-col items-center gap-2 h-full justify-end relative z-10 group">
                    {/* 툴팁 (마우스 오버 시 표시) */}
                    <div className="absolute -top-10 bg-gray-900 text-white text-[10px] font-bold px-2.5 py-1 rounded-md opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
                      ₩{data.revenue.toLocaleString()}
                    </div>
                    {/* 차트 막대 */}
                    <div 
                      style={{ height: `${heightPercentage}%` }} 
                      className={`w-full max-w-[48px] rounded-t-xl transition-all duration-700 ease-out hover:opacity-80 animate-fade-in-up ${
                        isHighlight ? 'bg-blue-600 shadow-lg shadow-blue-200' : 'bg-blue-100'
                      }`}
                    ></div>
                    {/* X축 라벨 */}
                    <span className={`text-[11px] font-bold mt-2 whitespace-nowrap ${isHighlight ? 'text-blue-600' : 'text-gray-400'}`}>
                      {data.day}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          {/* 우측: 인기 게시글 TOP 3 랭킹 */}
          <div className="lg:col-span-1 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col">
            <div className="border-b border-gray-100 pb-4 mb-4">
              <h3 className="text-xl font-extrabold text-gray-950 tracking-tight">인기 프로젝트 TOP 3</h3>
              <p className="text-xs text-gray-400 mt-1">조회수 대비 구매율이 가장 높은 항목입니다.</p>
            </div>

            <div className="flex flex-col gap-5 mt-2">
              {mockTopProducts.map((product, index) => (
                <div key={product.id} className="flex flex-col gap-2 p-3 hover:bg-gray-50 rounded-xl transition">
                  <div className="flex items-start gap-3">
                    <span className={`text-sm font-black mt-0.5 ${index === 0 ? 'text-blue-600' : 'text-gray-400'}`}>
                      {index + 1}
                    </span>
                    <div className="flex flex-col gap-0.5 w-full">
                      <h4 className="text-sm font-bold text-gray-900 truncate leading-tight w-48">
                        {product.title}
                      </h4>
                      <div className="flex justify-between items-center mt-1">
                        <span className="text-[11px] text-gray-500 font-medium flex items-center gap-1">
                          <Eye size={12} /> {product.views} 조회
                        </span>
                        <span className="text-[11px] font-bold text-emerald-600 bg-emerald-50 px-1.5 py-0.5 rounded">
                          전환 {product.conversion}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-auto pt-6 border-t border-gray-100">
              <div className="bg-slate-50 rounded-xl p-4 border border-slate-100 flex flex-col gap-2">
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">💡 Analytics Tip</span>
                <p className="text-xs text-slate-700 font-medium leading-relaxed">
                  조회수는 높으나 구매 전환율이 낮은 상품은 가격을 소폭 하향 조정하거나, 상세 설명을 보완하는 것을 추천합니다.
                </p>
              </div>
            </div>
          </div>

        </section>
      </main>

      {/* 차트 애니메이션용 CSS */}
      <style dangerouslySetInnerHTML={{__html: `
        @keyframes fadeInUp {
          from { opacity: 0; transform: translateY(10px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .animate-fade-in-up {
          animation: fadeInUp 0.5s ease-out forwards;
        }
      `}} />
    </div>
  );
};

export default SellerAnalyticsPage;