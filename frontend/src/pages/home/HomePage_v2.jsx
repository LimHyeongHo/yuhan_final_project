import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Flame, TrendingUp, Clock, Users } from 'lucide-react';
import Header from '../../components/layout/Header';

const HomePage = () => {
  const navigate = useNavigate();

  const handleProductClick = (product) => {
    navigate('/payment', { state: { product } });
  };
  const productList = [
    { id: 1, title: '명품 Java Programming (개정 4판)', major: '컴퓨터소프트웨어', author: '황기태, 김효수', current: 8, target: 10, originalPrice: '33,000', price: '24,000원', dDay: 'D-5', progress: 80, thumbnail: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=600&auto=format&fit=crop' },
    { id: 2, title: '쉽게 배우는 스프링 부트 3 웹 프로그래밍', major: '컴퓨터소프트웨어', author: '구멍가게 코딩단', current: 4, target: 5, originalPrice: '29,000', price: '21,000원', dDay: 'D-5', progress: 80, thumbnail: 'https://images.unsplash.com/photo-1555662800-82a8747209e7?q=80&w=600&auto=format&fit=crop' },
    { id: 3, title: 'Do it! 리액트 모던 웹 개발', major: '컴퓨터소프트웨어', author: '박성호', current: 12, target: 20, originalPrice: '35,000', price: '26,000원', dDay: 'D-1', progress: 60, thumbnail: 'https://images.unsplash.com/photo-1633356122544-f134324a6cee?q=80&w=600&auto=format&fit=crop' },
    { id: 4, title: '알고리즘 문제 해결 전략 (인사이트)', major: '정보통신', author: '구종만', current: 2, target: 10, originalPrice: '50,000', price: '38,000원', dDay: 'D-7', progress: 20, thumbnail: 'https://images.unsplash.com/photo-1516116216624-53e697fedbea?q=80&w=600&auto=format&fit=crop' },
  ];

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
            placeholder="이번 학기 필요한 전공책, 저자, 출판사를 검색해보세요!" 
            className="flex-grow pl-3 md:pl-4 pr-4 py-3 md:py-4 bg-transparent border-none text-sm md:text-base font-semibold outline-none w-full text-gray-900 placeholder-gray-400"
          />
          <button className="px-6 md:px-10 py-3 md:py-4 bg-blue-600 hover:bg-blue-700 text-white text-sm md:text-base font-bold rounded-xl md:rounded-[18px] transition whitespace-nowrap">
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
            {productList.map((item) => (
              <div key={item.id} onClick={() => handleProductClick(item)} className="bg-white rounded-[24px] border border-gray-200 shadow-sm flex flex-col hover:border-blue-300 hover:shadow-xl transition-all cursor-pointer group overflow-hidden">
                <div className="w-full h-48 bg-gray-100 relative overflow-hidden flex items-center justify-center">
                  <img src={item.thumbnail} alt={item.title} className="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                  <span className="absolute top-3 left-3 text-[11px] font-black px-2.5 py-1 rounded-md shadow-sm z-10 bg-red-500 text-white">
                    {item.dDay}
                  </span>
                  <span className="absolute top-3 right-3 text-[11px] font-black px-2.5 py-1 rounded-md shadow-sm z-10 bg-gray-900/70 text-white backdrop-blur-sm">
                    {item.major}
                  </span>
                </div>
                <div className="p-5 flex flex-col gap-4 flex-grow">
                  <div className="flex flex-col gap-1">
                    <h4 className="text-base font-extrabold text-gray-900 group-hover:text-blue-600 transition line-clamp-2 leading-snug">{item.title}</h4>
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
              </div>
            ))}
          </div>
        </section>

        {/* ✨ 섹션 2: 인기 공구 큐레이션 */}
        <section className="flex flex-col gap-6">
          <div className="flex items-center gap-2">
            <TrendingUp className="text-blue-500" size={28} />
            <h3 className="text-2xl font-extrabold text-gray-900 tracking-tight">실시간 인기 탑승 공구</h3>
          </div>
          
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
             {/* 현재는 위의 productList를 그대로 재사용하거나, 나중에 다른 API 데이터를 연결하시면 됩니다. */}
             {productList.map((item) => (
              <div key={`popular-${item.id}`} onClick={() => handleProductClick(item)} className="bg-white rounded-[24px] border border-gray-200 shadow-sm flex flex-col hover:border-blue-300 hover:shadow-xl transition-all cursor-pointer group overflow-hidden">
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
                    <h4 className="text-base font-extrabold text-gray-900 group-hover:text-blue-600 transition line-clamp-2 leading-snug">{item.title}</h4>
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
              </div>
            ))}
          </div>
        </section>

      </main>
    </div>
  );
};

export default HomePage;