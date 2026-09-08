// [UI-RQ-004][feature/ui-fixes] 메인 검색창 → 공구 찾기 목록으로 검색어 전달 (Enter/버튼 동일 동작)
import React, { useState, useEffect } from 'react';
import { Search, Flame, TrendingUp, Clock, Users } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import Header from '../../components/layout/Header';

const HomePage = () => {
  const navigate = useNavigate();

  // 팀원이 수정한 버그 픽스 적용 (handleProductClick 제거됨)
  const [productList, setProductList] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');

  const handleSearch = () => {
    const trimmed = searchTerm.trim();
    navigate(trimmed ? `/buyer/products?q=${encodeURIComponent(trimmed)}` : '/buyer/products');
  };

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/products');
        if (!response.ok) {
          throw new Error('Failed to fetch products');
        }
        const data = await response.json();

        // [신규] 마감된 게시글(기간이 지났거나 상태가 OPEN이 아닌 경우) 제외 필터링
        const activeData = data.filter(item => {
          const deadlineDate = new Date(item.deadline);
          const now = new Date();
          return item.status === 'OPEN' && (deadlineDate - now) >= 0;
        });

        // 데이터 매핑
        const mappedData = activeData.map(item => {
          // 진행률 계산
          const target = item.targetCount || 1; // 0으로 나누기 방지
          const progress = Math.min((item.currentCount / target) * 100, 100);

          // D-Day 계산
          const deadlineDate = new Date(item.deadline);
          const now = new Date();
          const diffTime = deadlineDate - now;
          const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
          const dDayStr = diffDays > 0 ? `D-${diffDays}` : (diffDays === 0 ? 'D-Day' : '마감');

          return {
            id: item.productId,
            title: item.title,
            major: item.type === 'BOOK' ? '전공서적' : '기타',
            author: item.author || '저자 미상',
            current: item.currentCount,
            target: item.targetCount,
            originalPrice: item.originalPrice ? Number(item.originalPrice).toLocaleString() : Number(item.price).toLocaleString(),
            price: `${Number(item.price).toLocaleString()}원`,
            dDay: dDayStr,
            diffDays: diffDays,
            progress: progress,
            thumbnail: item.imageUrl || null
          };
        });

        setProductList(mappedData);
      } catch (error) {
        console.error('Error fetching products:', error);
      }
    };

    fetchProducts();
  }, []);

  const urgentProducts = productList.filter(item => item.diffDays >= 0 && item.diffDays <= 5);
  const popularProducts = productList.filter(item => item.current >= item.target * 0.5);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col font-sans">
      <Header />

      {/* 🚀 1. 히어로 배너 영역 (높이 및 패딩 재조정) */}
      <div className="bg-gradient-to-r from-blue-600 to-indigo-600 pt-16 pb-32 px-6 md:px-8 w-full">
        <div className="max-w-7xl mx-auto flex flex-col items-center text-center gap-4">
          <span className="bg-white/20 text-white text-[10px] font-black tracking-widest px-3 py-1.5 rounded-full w-max uppercase">
            Yuhan University Joint Purchase
          </span>
          {/* 타이틀 크기를 반응형으로 조정하여 줄바꿈이 예쁘게 떨어지도록 함 */}
          <h2 className="text-3xl md:text-4xl lg:text-5xl font-extrabold tracking-tight text-white mt-2 leading-tight">
            전공책, 이제 친구들과 모여서 <span className="text-yellow-400">N빵</span> 하세요!
          </h2>
          <p className="text-blue-100 text-sm md:text-base font-medium max-w-4xl leading-relaxed mt-2">
            학과 인원이 모일수록 가격은 파괴됩니다. 안전한 블록체인 기반 거래로 투명하고 저렴하게 전공 서적을 구입해 보세요.
          </p>
        </div>
      </div>

      {/* 🚀 2. 본문 영역 (마진을 음수로 주어 배너 위로 겹치게 배치) */}
      <main className="flex-grow max-w-7xl w-full mx-auto px-6 md:px-8 pb-20 flex flex-col gap-16 -mt-10 relative z-10">

        {/* 🔍 플로팅 통합 검색창 (배너와 본문 사이에 반쯤 걸쳐지는 입체적인 디자인) */}
        <div className="w-full max-w-4xl mx-auto bg-white p-2 rounded-2xl md:rounded-[24px] shadow-xl shadow-blue-900/5 border border-gray-100 flex items-center transition-all hover:shadow-2xl hover:shadow-blue-900/10">
          <div className="pl-4 md:pl-6 pointer-events-none">
            <Search className="text-gray-400" size={24} />
          </div>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') handleSearch(); }}
            placeholder="이번 학기 필요한 전공책, 저자, 출판사를 검색해보세요!"
            className="flex-grow pl-3 md:pl-4 pr-4 py-3 md:py-4 bg-transparent border-none text-sm md:text-base font-semibold outline-none w-full text-gray-900 placeholder-gray-400"
          />
          <button
            type="button"
            onClick={handleSearch}
            className="px-6 md:px-10 py-3 md:py-4 bg-blue-600 hover:bg-blue-700 text-white text-sm md:text-base font-bold rounded-xl md:rounded-[18px] transition whitespace-nowrap"
          >
            검색
          </button>
        </div>

        {/* 🔥 섹션 1: 마감 임박 큐레이션 */}
        <section className="flex flex-col gap-6 mt-4">
          <div className="flex items-center gap-2">
            <Flame className="text-red-500" size={28} />
            <h3 className="text-2xl font-extrabold text-gray-900 tracking-tight">곧 출발해요! 마감 임박 공구</h3>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {urgentProducts.length > 0 ? (
              urgentProducts.map((item) => (
                <Link key={item.id} to={`/buyer/products/${item.id}`} className="bg-white rounded-[24px] border border-gray-200 shadow-sm flex flex-col hover:border-blue-300 hover:shadow-xl transition-all cursor-pointer group overflow-hidden">
                  <div className="w-full h-48 bg-gray-100 relative overflow-hidden flex items-center justify-center">
                    {item.thumbnail ? (
                      <img src={item.thumbnail} alt={item.title} className="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                    ) : (
                      <span className="text-gray-400 text-sm font-semibold">이미지 없음</span>
                    )}
                    <span className="absolute top-3 left-3 text-[11px] font-black px-2.5 py-1 rounded-md shadow-sm z-10 bg-red-500 text-white">
                      {item.dDay}
                    </span>
                    <span className="absolute top-3 right-3 text-[11px] font-black px-2.5 py-1 rounded-md shadow-sm z-10 bg-gray-900/70 text-white backdrop-blur-sm">
                      {item.major}
                    </span>
                  </div>
                  <div className="p-5 flex flex-col gap-4 flex-grow">
                    <div className="flex flex-col gap-1">
                      <h4 className="text-base font-extrabold text-gray-900 group-hover:text-blue-600 transition line-clamp-2 leading-snug">
                        {item.title.includes('-') ? (
                          <span>{item.title.split('-')[0].trim()}</span>
                        ) : (
                          item.title
                        )}
                      </h4>
                      <span className="text-xs text-gray-400 font-semibold">{item.author}</span>
                    </div>
                    <div className="flex flex-col gap-2 mt-auto pt-4 border-t border-gray-50">
                      <div className="flex justify-between items-end">
                        <div className="flex flex-col">
                          <span className="text-xs text-gray-400 line-through font-semibold">{item.originalPrice}원</span>
                          <span className="text-lg font-black text-blue-600">{item.price}</span>
                        </div>
                        <div className="flex items-center gap-1 text-[11px] font-bold text-gray-500 mb-1">
                          <Users size={12} />
                          <span className="text-blue-500">{item.current}</span><span>/ {item.target}명</span>
                        </div>
                      </div>
                      <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                        <div className="h-full bg-blue-500 rounded-full transition-all duration-500" style={{ width: `${item.progress}%` }}></div>
                      </div>
                    </div>
                  </div>
                </Link>
              ))
            ) : (
              <div className="col-span-full py-10 text-center text-gray-500 font-semibold bg-white rounded-[24px] border border-gray-200 border-dashed">
                마감 임박인 프로젝트가 없습니다.
              </div>
            )}
          </div>
        </section>

        {/* ✨ 섹션 2: 인기 공구 큐레이션 */}
        <section className="flex flex-col gap-6">
          <div className="flex items-center gap-2">
            <TrendingUp className="text-blue-500" size={28} />
            <h3 className="text-2xl font-extrabold text-gray-900 tracking-tight">실시간 인기 탑승 공구</h3>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {popularProducts.length > 0 ? (
              popularProducts.map((item) => (
                <Link key={`popular-${item.id}`} to={`/buyer/products/${item.id}`} className="bg-white rounded-[24px] border border-gray-200 shadow-sm flex flex-col hover:border-blue-300 hover:shadow-xl transition-all cursor-pointer group overflow-hidden">
                  <div className="w-full h-48 bg-gray-100 relative overflow-hidden flex items-center justify-center">
                    <img src={item.thumbnail} alt={item.title} className="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                    {/* 인기 공구는 빨간 D-Day 대신 파란색 '모집 중' 뱃지로 변경 가능 */}
                    <span className="absolute top-3 left-3 text-[11px] font-black px-2.5 py-1 rounded-md shadow-sm z-10 bg-blue-500 text-white">
                      모집 중
                    </span>
                    <span className="absolute top-3 right-3 text-[11px] font-black px-2.5 py-1 rounded-md shadow-sm z-10 bg-gray-900/70 text-white backdrop-blur-sm">
                      {item.major}
                    </span>
                  </div>
                  <div className="p-5 flex flex-col gap-4 flex-grow">
                    <div className="flex flex-col gap-1">
                      <h4 className="text-base font-extrabold text-gray-900 group-hover:text-blue-600 transition line-clamp-2 leading-snug">
                        {item.title.includes('-') ? (
                          <span>{item.title.split('-')[0].trim()}</span>
                        ) : (
                          item.title
                        )}
                      </h4>
                      <span className="text-xs text-gray-400 font-semibold">{item.author}</span>
                    </div>
                    <div className="flex flex-col gap-2 mt-auto pt-4 border-t border-gray-50">
                      <div className="flex justify-between items-end">
                        <div className="flex flex-col">
                          <span className="text-xs text-gray-400 line-through font-semibold">{item.originalPrice}원</span>
                          <span className="text-lg font-black text-blue-600">{item.price}</span>
                        </div>
                        <div className="flex items-center gap-1 text-[11px] font-bold text-gray-500 mb-1">
                          <Users size={12} />
                          <span className="text-blue-500">{item.current}</span><span>/ {item.target}명</span>
                        </div>
                      </div>
                      <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                        <div className="h-full bg-blue-500 rounded-full transition-all duration-500" style={{ width: `${item.progress}%` }}></div>
                      </div>
                    </div>
                  </div>
                </Link>
              ))
            ) : (
              <div className="col-span-full py-10 text-center text-gray-500 font-semibold bg-white rounded-[24px] border border-gray-200 border-dashed">
                진행률 50% 이상인 인기 프로젝트가 없습니다.
              </div>
            )}
          </div>
        </section>

      </main>
    </div>
  );
};

export default HomePage;