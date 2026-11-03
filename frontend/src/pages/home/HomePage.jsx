import React from 'react';
import Header from '../../components/layout/Header';
/// [*] 헤더 컴포넌트 연결
import Header from '../../components/layout/Header';

const Home = () => {
  return (
    <div className="flex bg-gray-50 min-h-screen">
      {/* 1. 사이드바 조립 */}
      <Sidebar />

      {/* 우측 콘텐츠 영역 */}
      <div className="flex-grow ml-64 flex flex-col">
        {/* 2. 헤더 조립 */}
        <Header />

        {/* 3. 메인 스페이스 */}
        <main className="p-8 flex flex-col gap-10">
          
          {/* 상단 섹션: 스포트라이트 카드 & 모집 현황 */}
          <section className="grid grid-cols-12 gap-6">
            {/* 메인 배너 카드 (7/12) */}
            <div className="col-span-8 bg-gray-900 rounded-[32px] p-10 h-[320px] relative overflow-hidden group shadow-2xl shadow-blue-900/20">
              <div className="relative z-10 h-full flex flex-col justify-between text-white">
                <div>
                  <span className="text-blue-400 font-bold text-xs uppercase tracking-widest">Premium Spotlight</span>
                  <h2 className="text-4xl font-extrabold mt-2 leading-tight">고전의 재해석 <br/> <span className="text-blue-400">X1 LIMITED EDITION</span></h2>
                </div>
                <div className="text-3xl font-black text-blue-400">₩ 158,000</div>
              </div>
              {/* 배경 장식용 이미지 (피그마 느낌) */}
              <div className="absolute right-[-10%] top-0 w-1/2 h-full bg-gradient-to-l from-blue-600/20 to-transparent"></div>
            </div>

            {/* 실시간 모집 정보 카드 (4/12) */}
            <div className="col-span-4 bg-white rounded-[32px] p-8 border border-gray-100 shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex justify-between items-center mb-6">
                  <span className="text-sm font-bold text-gray-900">실시간 모집 정보</span>
                  <span className="text-xs text-gray-400">14시간 22분 05초 남음</span>
                </div>
                <p className="text-sm text-gray-500 leading-relaxed">최고급 본질 교재 시리즈 최저가 파격 커뮤니티. 모집 인원이 충족되어 소장 가치가 한층 더 높습니다.</p>
              </div>
              <div className="mt-6">
                <div className="flex justify-between text-xs font-bold mb-2">
                  <span>참여 진행률</span>
                  <span className="text-blue-600">85%</span>
                </div>
                <div className="w-full h-3 bg-gray-100 rounded-full overflow-hidden">
                  <div className="h-full bg-blue-600 w-[85%] rounded-full transition-all"></div>
                </div>
              </div>
              <button className="w-full bg-blue-600 text-white rounded-2xl py-4 font-bold mt-6 hover:bg-blue-700 transition shadow-lg shadow-blue-100">공동 구매 참여하기</button>
            </div>
          </section>

          {/* 중간 섹션: 중고 장터 탐색 */}
          <section>
            <div className="flex justify-between items-end mb-6">
              <h3 className="text-2xl font-extrabold text-gray-950 tracking-tight">중고 장터 탐색</h3>
              <button className="text-sm font-semibold text-gray-400 hover:text-blue-600 transition">전체 보기 &gt;</button>
            </div>
            <div className="grid grid-cols-3 gap-6">
              {[
                { title: '바우하우스 디자인 총서', price: '89,000', stock: '12/20 판매', img: 'https://images.unsplash.com/photo-1589998059171-988d887df646?q=80&w=600' },
                { title: '컴퓨터의 역사 (영문판)', price: '125,000', stock: '최근 입고', img: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=600' },
                { title: '가죽 법정 세계 문학 전집', price: '210,000', stock: '5/100 모집', img: 'https://images.unsplash.com/photo-1512820790803-83ca734da794?q=80&w=600' },
              ].map((item, i) => (
                <div key={i} className="bg-white p-4 rounded-[28px] border border-gray-100 hover:shadow-xl transition-all group cursor-pointer">
                  <div className="aspect-[4/3] rounded-2xl bg-gray-100 mb-4 overflow-hidden">
                    <img src={item.img} className="w-full h-full object-cover group-hover:scale-110 transition-transform" />
                  </div>
                  <h4 className="font-bold text-gray-900 mb-1">{item.title}</h4>
                  <div className="flex justify-between items-center">
                    <span className="text-lg font-black text-gray-950">₩ {item.price}</span>
                    <span className="text-xs font-bold text-blue-500 bg-blue-50 px-2 py-1 rounded-md">{item.stock}</span>
                  </div>
                </div>
              ))}
            </div>
          </section>

          {/* 하단 섹션: 나의 참여 정보 테이블 */}
          <section className="bg-white rounded-[32px] p-8 border border-gray-100 shadow-sm">
            <h3 className="text-xl font-extrabold text-gray-950 mb-6">나의 참여 정보</h3>
            <table className="w-full">
              <thead>
                <tr className="text-left text-xs font-bold text-gray-400 uppercase tracking-widest border-b border-gray-50">
                  <th className="pb-4 px-2">상품 정보</th>
                  <th className="pb-4 px-2 text-center">참여 인원</th>
                  <th className="pb-4 px-2 text-center">진행 단계</th>
                  <th className="pb-4 px-2 text-right">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {[1, 2].map((_, i) => (
                  <tr key={i} className="group">
                    <td className="py-6 px-2">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-xl bg-gray-100 overflow-hidden flex-shrink-0">
                          <img src="https://images.unsplash.com/photo-1589998059171-988d887df646?q=80&w=100" />
                        </div>
                        <div>
                          <p className="font-bold text-sm text-gray-900">바우하우스 디자인 총서</p>
                          <p className="text-xs text-gray-400">ID: ABK-88271</p>
                        </div>
                      </div>
                    </td>
                    <td className="py-6 px-2 text-center">
                      <span className="text-sm font-bold text-gray-700">12 / 20</span>
                    </td>
                    <td className="py-6 px-2 text-center">
                      <span className="bg-blue-100 text-blue-600 text-[10px] font-black px-2 py-1 rounded-full uppercase">Recruiting</span>
                    </td>
                    <td className="py-6 px-2 text-right">
                      <button className="text-xs font-bold text-gray-400 hover:text-blue-600 transition">상세 보기</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>

        </main>
      </div>
    </div>
  );
};

export default Home;