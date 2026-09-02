import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { LayoutDashboard, Users, Activity, BarChart3, ShieldAlert, ArrowUpRight, Clock } from 'lucide-react';
import Header from '../../../components/layout/Header'; // 공통 헤더 연결

const AdminDashboardPage = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    newUsersToday: 0,
    activeSellers: 0,
    totalProducts: 0,
    totalUsers: 0
  });
  const [weeklyStats, setWeeklyStats] = useState({
    signupData: [0, 0, 0, 0, 0, 0, 0],
    sellerData: [0, 0, 0, 0, 0, 0, 0]
  });
  const [recentProducts, setRecentProducts] = useState([]);

  useEffect(() => {
    fetch('http://localhost:8080/api/admin/statistics', { credentials: 'include' })
      .then(res => res.json())
      .then(data => setStats(data))
      .catch(err => console.error("통계 데이터 로드 실패:", err));

    fetch('http://localhost:8080/api/admin/weekly-stats', { credentials: 'include' })
      .then(res => res.json())
      .then(data => setWeeklyStats(data))
      .catch(err => console.error("주간 통계 데이터 로드 실패:", err));

    fetch('http://localhost:8080/api/admin/products/recent', { credentials: 'include' })
      .then(res => res.json())
      .then(data => setRecentProducts(data))
      .catch(err => console.error("최근 상품 로드 실패:", err));
  }, []);

  const signupMax = Math.max(...weeklyStats.signupData, 1);
  const sellerMax = Math.max(...weeklyStats.sellerData, 1);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      
      {/* 1. 글로벌 공통 헤더 네비게이션 */}
      <Header />

      {/* 2. 상단 묵직한 다크 배너 섹션 (디자인 아이덴티티 통일) */}
      <section className="bg-slate-900 text-white py-12 px-6 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-col gap-2">
          <span className="bg-blue-500/20 text-blue-400 text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full w-max border border-blue-500/30">
            Operations Hub
          </span>
          <h2 className="text-4xl font-extrabold tracking-tight mt-1">
            통합 대시보드
          </h2>
          <p className="text-slate-400 font-medium text-base max-w-2xl mt-1">
            YU-BOOK 플랫폼의 전체 운영 현황, 회원 가입 추이 및 실시간 시스템 트래픽을 한눈에 모니터링하고 제어합니다.
          </p>
        </div>
      </section>

      {/* 3. 메인 대시보드 콘텐츠 영역 */}
      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col gap-8">
        
        {/* 상단 3종 핵심 요약 통계 메트릭 카드 */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* 카드 1: 일간 신규 가입 */}
          <div 
            onClick={() => navigate('/admin/dashboard/users-stats')}
            className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center cursor-pointer hover:shadow-md hover:border-blue-300 transition-all"
          >
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">일간 신규 가입</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">{stats.newUsersToday.toLocaleString()} 명</h3>
              <span className="text-emerald-600 text-xs font-bold mt-1 flex items-center gap-0.5">
                ▲ +12% <span className="text-gray-400 font-normal ml-1">vs 어제</span>
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center text-blue-600">
              <Users size={22} />
            </div>
          </div>

          {/* 카드 2: 누적 활성 판매자 */}
          <div 
            onClick={() => navigate('/admin/dashboard/sellers-stats')}
            className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center cursor-pointer hover:shadow-md hover:border-blue-300 transition-all"
          >
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">누적 활성 판매자</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">{stats.activeSellers.toLocaleString()} 명</h3>
              <span className="text-emerald-600 text-xs font-bold mt-1 flex items-center gap-0.5">
                ▲ +3% <span className="text-gray-400 font-normal ml-1">vs 지난달</span>
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-600">
              <BarChart3 size={22} />
            </div>
          </div>

          {/* 카드 3: 누적 등록 상품 (공동구매) */}
          <div 
            onClick={() => navigate('/admin/products')}
            className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center cursor-pointer hover:shadow-md hover:border-blue-300 transition-all"
          >
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">누적 등록 상품 (공동구매)</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">{stats.totalProducts.toLocaleString()} 건</h3>
              <span className="text-blue-600 text-xs font-extrabold mt-1 uppercase tracking-tight flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-blue-500 animate-pulse"></span> Live Traffic
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-purple-50 flex items-center justify-center text-purple-600">
              <Activity size={22} />
            </div>
          </div>
        </section>

        {/* 하단 스플릿 그리드 레이아웃 (2/3 메인 차트 판넬 + 1/3 상세 모니터링 판넬) */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* 좌측 패널: 가입 및 등록 추이 시각화 (2/3 영역) */}
          <div className="lg:col-span-2 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col gap-8">
            <div>
              <h3 className="text-xl font-extrabold text-gray-950 tracking-tight">주간 운영 통계 추이</h3>
              <p className="text-xs text-gray-400 mt-0.5">최근 7일간의 신규 유저 유입 현황 및 판매자 활성화 그래프입니다.</p>
            </div>

            {/* 피그마 기획안의 미니 바 차트를 정밀 구현한 레이아웃 */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              
              {/* 가입 통계 세로 막대 그래프 구역 */}
              <div className="flex flex-col gap-4">
                <span className="text-sm font-bold text-gray-700 flex justify-between items-center">
                  일간 신규 가입 트렌드
                  <span className="text-xs text-blue-600 font-extrabold flex items-center">최고치 달성 <ArrowUpRight size={14} /></span>
                </span>
                <div className="h-32 flex items-end gap-2 pt-4 border-b border-gray-100 px-2">
                  {weeklyStats.signupData.map((val, idx) => (
                    <div key={idx} className="flex-grow flex flex-col items-center gap-1.5 h-full justify-end">
                      <div 
                        style={{ height: `${(val / signupMax) * 100}%` }} 
                        className={`w-full rounded-t-md transition-all duration-500 ${idx === 6 ? 'bg-blue-600' : 'bg-gray-200'}`}
                      ></div>
                    </div>
                  ))}
                </div>
                <div className="flex justify-between text-[11px] text-gray-400 font-semibold px-1">
                  <span>D-6</span><span>D-5</span><span>D-4</span><span>D-3</span><span>D-2</span><span>D-1</span><span className="text-blue-600 font-bold">오늘</span>
                </div>
              </div>

              {/* 활성 판매자 세로 막대 그래프 구역 */}
              <div className="flex flex-col gap-4">
                <span className="text-sm font-bold text-gray-700 flex justify-between items-center">
                  누적 활성 판매자 트렌드
                  <span className="text-xs text-red-600 font-extrabold flex items-center">안정적 우상향</span>
                </span>
                <div className="h-32 flex items-end gap-2 pt-4 border-b border-gray-100 px-2">
                  {weeklyStats.sellerData.map((val, idx) => (
                    <div key={idx} className="flex-grow flex flex-col items-center gap-1.5 h-full justify-end">
                      <div 
                        style={{ height: `${(val / sellerMax) * 100}%` }} 
                        className={`w-full rounded-t-md transition-all duration-500 ${idx === 6 ? 'bg-red-700' : 'bg-gray-200'}`}
                      ></div>
                    </div>
                  ))}
                </div>
                <div className="flex justify-between text-[11px] text-gray-400 font-semibold px-1">
                  <span>D-6</span><span>D-5</span><span>D-4</span><span>D-3</span><span>D-2</span><span>D-1</span><span className="text-red-700 font-bold">오늘</span>
                </div>
              </div>

            </div>

            {/* 대시보드 하단 미니 알림/가이드 */}
            <div className="bg-blue-50/50 rounded-2xl p-4 border border-blue-100 flex items-center gap-3 mt-2">
              <Clock size={18} className="text-blue-600 flex-shrink-0" />
              <p className="text-xs text-gray-600 leading-relaxed font-medium">
                현재 플랫폼 트래픽 분산 처리가 정상 작동 중입니다. 매일 자정 시스템 백엔드 해시 원장 정기 검증 작업이 자동으로 스케줄링됩니다.
              </p>
            </div>
          </div>

          {/* 우측 패널: 상세 활동 시스템 모니터링 (1/3 영역) */}
          <div className="lg:col-span-1 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col justify-between">
            <div className="flex flex-col gap-6">
              <div>
                <h3 className="text-xl font-extrabold text-gray-950 tracking-tight">상세 활동 모니터링</h3>
                <p className="text-xs text-gray-400 mt-0.5">엔진의 핵심 성과 지표와 에러율을 실시간 체크합니다.</p>
              </div>

              <div className="flex flex-col gap-6 mt-2">
                {/* 1. 초당 요청량 */}
                <div className="flex flex-col gap-2">
                  <div className="flex justify-between items-end">
                    <span className="text-xs font-bold text-gray-500">초당 API 요청량</span>
                    <span className="text-sm font-black text-gray-900">842 req/s</span>
                  </div>
                  <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden shadow-inner">
                    <div className="h-full bg-red-500 w-[55%] rounded-full transition-all duration-500"></div>
                  </div>
                </div>

                {/* 2. 에러율 */}
                <div className="flex flex-col gap-2">
                  <div className="flex justify-between items-end">
                    <span className="text-xs font-bold text-gray-500">시스템 오류율</span>
                    <span className="text-sm font-black text-red-600">0.02 %</span>
                  </div>
                  <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden shadow-inner">
                    <div className="h-full bg-orange-500 w-[5%] rounded-full transition-all duration-500"></div>
                  </div>
                </div>

                {/* 3. 보안 위협 */}
                <div className="flex flex-col gap-1 border-t border-gray-50 pt-4">
                  <span className="text-xs font-bold text-gray-500 block mb-1">최근 보안 위협 감지</span>
                  <div className="flex items-center gap-2 text-emerald-600 font-extrabold text-base">
                    <ShieldAlert size={18} className="text-emerald-500" />
                    <span>None (위험 요소 없음)</span>
                  </div>
                </div>
              </div>
            </div>

            {/* 대시보드 푸터 상태 표시 */}
            <div className="mt-8 pt-4 border-t border-gray-100 flex items-center justify-between text-xs font-bold text-gray-400">
              <span>서버 동기화 상태</span>
              <span className="text-blue-600 font-extrabold uppercase">Synchronized</span>
            </div>
          </div>
        </section>

        {/* 🚀 최근 등록된 공동구매 리스트 */}
        <section className="bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col mt-2">
          <div className="flex justify-between items-center mb-6">
            <h3 className="text-xl font-extrabold text-gray-950 tracking-tight">최근 등록된 공동구매 리스트</h3>
            <span className="bg-gray-100 text-gray-600 text-xs font-bold px-3 py-1.5 rounded-lg">
              최근 5건
            </span>
          </div>
          
          <div className="overflow-x-auto border-t border-gray-100 mt-2">
            {recentProducts.length === 0 ? (
              <div className="p-8 text-center text-gray-400 font-bold">최근 등록된 상품이 없습니다.</div>
            ) : (
              <table className="w-full text-left whitespace-nowrap">
                <thead>
                  <tr className="bg-gray-50/50 text-gray-500 text-xs uppercase tracking-wider font-bold">
                    <th className="px-6 py-4 border-b border-gray-100 font-bold w-1/2">책 제목</th>
                    <th className="px-6 py-4 border-b border-gray-100 text-right">목표 인원</th>
                    <th className="px-6 py-4 border-b border-gray-100 text-right">가격</th>
                    <th className="px-6 py-4 border-b border-gray-100 text-center">모집 상태</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {recentProducts.map(product => (
                    <tr key={product.id} className="hover:bg-gray-50/50 transition">
                      <td className="px-6 py-4 whitespace-normal">
                        <div className="flex flex-col gap-1">
                          <span className="text-[10px] font-bold text-gray-400 uppercase">{product.category}</span>
                          <span className="font-bold text-gray-900 text-sm leading-tight">
                            {product.title.split('-')[0].trim()}
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-right font-bold text-gray-700 text-sm">
                        {product.targetCount}명
                      </td>
                      <td className="px-6 py-4 text-right font-black text-blue-600 text-sm">
                        {product.price.toLocaleString()}원
                      </td>
                      <td className="px-6 py-4 text-center">
                        <span className={`px-3 py-1 rounded-full text-xs font-bold inline-block ${
                          product.status === 'OPEN' ? 'bg-blue-50 text-blue-600' : 'bg-gray-100 text-gray-500'
                        }`}>
                          {product.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </section>

      </main>
    </div>
  );
};

export default AdminDashboardPage;