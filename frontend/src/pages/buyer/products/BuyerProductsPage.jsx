import React, { useState } from 'react';
import { Link } from 'react-router-dom';
// 🛠️ LayoutGrid, List 아이콘이 추가되었습니다.
import { Search, SlidersHorizontal, BookOpen, Users, ChevronDown, Filter, Clock, Image as ImageIcon, LayoutGrid, List } from 'lucide-react';
import Header from '../../../components/layout/Header';

const BuyerProductsPage = () => {
  const [isAdvancedOpen, setIsAdvancedOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchTarget, setSearchTarget] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  
  // 🛠️ 뷰 모드 상태 관리 (GRID: 바둑판형, LIST: 목록형)
  const [viewMode, setViewMode] = useState('GRID');

  const [productList, setProductList] = useState([]);

  React.useEffect(() => {
    fetch('http://localhost:8080/api/products')
      .then(res => res.json())
      .then(data => {
        const formattedData = data.map(item => ({
          id: item.productId,
          title: item.title,
          type: item.type, // 'BOOK' or 'ITEM'
          category: item.category || '', // 백엔드에서 추가된 category 값
          major: item.type === 'BOOK' ? '전공 도서' : '학과 물품', 
          author: item.author || item.publisher || '정보 없음',
          current: item.currentCount,
          target: item.targetCount,
          price: item.price.toLocaleString() + '원',
          status: item.status === 'OPEN' ? '모집 중' : '마감됨',
          deadline: item.deadline ? item.deadline.split('T')[0] : '기한 없음',
          thumbnail: item.imageUrl || null,
          description: item.description || ''
        }));
        setProductList(formattedData);
      })
      .catch(err => console.error("상품 목록 로드 실패:", err));
  }, []);

  // 🌟 프론트엔드 검색 및 필터링 로직
  const filteredList = React.useMemo(() => {
    return productList.filter(item => {
      // 1. 상품 종류 필터 (전체, 전공도서, 학과물품)
      if (typeFilter !== 'ALL' && item.type !== typeFilter) {
        return false;
      }

      // 1-1. 세부 카테고리 필터 적용
      if (categoryFilter !== 'ALL') {
        if (!item.category.includes(categoryFilter)) {
          return false;
        }
      }

      // 2. 검색어 필터
      if (searchQuery.trim() !== '') {
        const query = searchQuery.toLowerCase();
        const matchTitle = item.title.toLowerCase().includes(query) || item.author.toLowerCase().includes(query);
        const matchContent = item.description.toLowerCase().includes(query);

        if (searchTarget === 'TITLE' && !matchTitle) return false;
        if (searchTarget === 'CONTENT' && !matchContent) return false;
        if (searchTarget === 'ALL' && !(matchTitle || matchContent)) return false;
      }
      
      return true;
    });
  }, [productList, searchQuery, searchTarget, typeFilter, categoryFilter]);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <Header />

      <div className="bg-gradient-to-r from-blue-600 to-indigo-600 py-16 px-6">
        <div className="max-w-7xl mx-auto flex flex-col gap-4">
          <span className="bg-white/20 text-white text-[10px] font-black tracking-widest px-3 py-1.5 rounded-full w-max uppercase">
            Yuhan University Joint Purchase
          </span>
          <h2 className="text-3xl md:text-4xl font-extrabold tracking-tight text-white mt-1">
            전공책, 이제 친구들과 모여서 <span className="text-yellow-400">N빵</span> 하세요!
          </h2>
          <p className="text-blue-100 text-sm md:text-base font-medium max-w-2xl leading-relaxed mt-1">
            학과 인원이 모일수록 가격은 파괴됩니다. 안전한 블록체인 기반 거래로 투명하고 저렴하게 전공 서적을 구입해 보세요.
          </p>
        </div>
      </div>

      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col gap-6">
        
        {/* 검색 박스 영역 */}
        <section className="bg-white rounded-[32px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col gap-4 transition-all">
          <div className="flex items-center gap-3">
            <div className="relative flex-grow">
              <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                <Search className="text-gray-400" size={20} />
              </div>
              <input 
                type="text" 
                placeholder="찾으시는 도서명, 저자, 출판사를 입력하세요..." 
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-11 pr-4 py-3.5 bg-gray-50 border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 rounded-2xl text-sm font-semibold outline-none transition"
              />
            </div>
            <button className="px-6 py-3.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold rounded-2xl transition shadow-sm whitespace-nowrap">
              검색
            </button>
            <button 
              onClick={() => setIsAdvancedOpen(!isAdvancedOpen)}
              className={`px-4 py-3.5 border text-sm font-bold rounded-2xl transition flex items-center gap-2 whitespace-nowrap ${
                isAdvancedOpen ? 'bg-blue-50 border-blue-200 text-blue-600' : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'
              }`}
            >
              <SlidersHorizontal size={18} />
              <span className="hidden sm:inline">세부 검색</span>
            </button>
          </div>

          {/* 세부 검색 토글 영역 */}
          {isAdvancedOpen && (
            <div className="pt-6 mt-2 border-t border-gray-100 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-in slide-in-from-top-2 fade-in duration-200">
              <div className="flex flex-col gap-2">
                <label className="text-xs font-bold text-gray-500 flex items-center gap-1.5"><Filter size={14} /> 검색 대상</label>
                <div className="flex bg-gray-50 p-1 rounded-xl border border-gray-200">
                  {['ALL', 'TITLE', 'CONTENT'].map((type) => (
                    <button key={type} onClick={() => setSearchTarget(type)} className={`flex-1 py-2 text-xs font-bold rounded-lg transition ${searchTarget === type ? 'bg-white text-blue-600 shadow-sm' : 'text-gray-500 hover:text-gray-900'}`}>
                      {type === 'ALL' ? '제목 + 내용' : type === 'TITLE' ? '제목만' : '내용만'}
                    </button>
                  ))}
                </div>
              </div>
              <div className="flex flex-col gap-2">
                <label className="text-xs font-bold text-gray-500 flex items-center gap-1.5"><BookOpen size={14} /> 상품 종류</label>
                <div className="flex bg-gray-50 p-1 rounded-xl border border-gray-200">
                  {['ALL', 'BOOK', 'ITEM'].map((type) => (
                    <button 
                      key={type} 
                      onClick={() => {
                        setTypeFilter(type);
                        if (type !== 'BOOK') setCategoryFilter('ALL');
                      }} 
                      className={`flex-1 py-2 text-xs font-bold rounded-lg transition ${typeFilter === type ? 'bg-white text-blue-600 shadow-sm' : 'text-gray-500 hover:text-gray-900'}`}
                    >
                      {type === 'ALL' ? '전체 상품' : type === 'BOOK' ? '전공도서' : '학과물품'}
                    </button>
                  ))}
                </div>
              </div>
              
              <div className="flex flex-col gap-2">
                <label className="text-xs font-bold text-gray-500 flex items-center gap-1.5"><Filter size={14} /> 세부 카테고리</label>
                <div className="relative">
                  <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)} className="w-full appearance-none px-4 py-2.5 bg-gray-50 border border-gray-200 focus:border-blue-500 rounded-xl text-sm font-semibold text-gray-700 outline-none cursor-pointer">
                    <option value="ALL">전체 카테고리</option>
                    <option value="대학교재">대학교재</option>
                    <option value="전문서적">전문서적</option>
                    <option value="컴퓨터">컴퓨터/IT</option>
                    <option value="모바일">모바일</option>
                    <option value="수험서">수험서/자격증</option>
                    <option value="자격증">자격증</option>
                    <option value="과학">과학</option>
                    <option value="공학">공학</option>
                    <option value="인문">인문학</option>
                    <option value="사회">사회과학</option>
                    <option value="어학">어학</option>
                    <option value="외국어">외국어사전</option>
                  </select>
                  <ChevronDown className="absolute right-3 top-3 text-gray-400 pointer-events-none" size={16} />
                </div>
              </div>
              <div className="flex flex-col justify-end pb-1">
                <label className="flex items-center gap-2 cursor-pointer group">
                  <input type="checkbox" className="w-4 h-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500 cursor-pointer" />
                  <span className="text-sm font-semibold text-gray-600 group-hover:text-gray-900 transition">모집 중인 공구만 보기</span>
                </label>
              </div>
            </div>
          )}
        </section>

        {/* 검색 결과 리스트 */}
        <section className="flex flex-col gap-4">
          <div className="flex items-center justify-between px-2">
            <span className="text-sm font-bold text-gray-500">총 <span className="text-blue-600">{filteredList.length}</span>건의 공구가 있습니다.</span>
            
            <div className="flex items-center gap-4">
              {/* 🌟 뷰 모드 전환 토글 버튼 */}
              <div className="hidden sm:flex items-center gap-1 bg-gray-100 p-1 rounded-lg">
                <button 
                  onClick={() => setViewMode('GRID')}
                  className={`p-1.5 rounded-md transition ${viewMode === 'GRID' ? 'bg-white text-blue-600 shadow-sm' : 'text-gray-400 hover:text-gray-700'}`}
                  title="그리드 뷰"
                >
                  <LayoutGrid size={16} />
                </button>
                <button 
                  onClick={() => setViewMode('LIST')}
                  className={`p-1.5 rounded-md transition ${viewMode === 'LIST' ? 'bg-white text-blue-600 shadow-sm' : 'text-gray-400 hover:text-gray-700'}`}
                  title="리스트 뷰"
                >
                  <List size={16} />
                </button>
              </div>

              <select className="text-sm font-bold text-gray-600 bg-transparent outline-none cursor-pointer hover:text-gray-900 transition">
                <option>최신 등록순</option>
                <option>마감 임박순</option>
                <option>낮은 가격순</option>
              </select>
            </div>
          </div>

          {/* 🌟 선택된 뷰 모드에 따라 레이아웃 동적 변경 */}
          <div className={viewMode === 'GRID' ? "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6" : "flex flex-col gap-4"}>
            {filteredList.map((item) => (
              <Link 
                key={item.id} 
                to={`/buyer/products/${item.id}`}
                className={`bg-white rounded-[24px] border border-gray-200 shadow-sm flex hover:border-blue-300 hover:shadow-md transition-all cursor-pointer group overflow-hidden ${
                  viewMode === 'GRID' ? 'flex-col' : 'flex-row items-stretch'
                }`}
              >
                
                {/* 썸네일 영역 */}
                <div className={`bg-gray-100 relative overflow-hidden flex items-center justify-center shrink-0 ${
                  viewMode === 'GRID' ? 'w-full h-48' : 'w-32 md:w-48'
                }`}>
                  {item.thumbnail ? (
                    <img src={item.thumbnail} alt={item.title} className="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                  ) : (
                    <div className="flex flex-col items-center gap-2 text-gray-400">
                      <ImageIcon size={32} />
                      <span className="text-xs font-bold">이미지 없음</span>
                    </div>
                  )}
                  <span className={`absolute top-3 left-3 text-[11px] font-black px-2.5 py-1 rounded-md shadow-sm z-10 ${
                    item.status === '모집 중' ? 'bg-blue-500 text-white' : 'bg-orange-500 text-white'
                  }`}>
                    {item.status}
                  </span>
                </div>
                
                {/* 정보 영역 */}
                <div className={`p-5 flex flex-col flex-grow ${viewMode === 'GRID' ? 'gap-4' : 'gap-3 justify-center'}`}>
                  
                  <div className="flex justify-between items-center">
                    <span className="text-[10px] font-extrabold text-gray-500 bg-gray-50 px-2 py-1 rounded border border-gray-100">
                      {item.major}
                    </span>
                    <div className="flex items-center gap-1 text-[11px] font-bold text-red-500 bg-red-50 px-2 py-1 rounded-md">
                      <Clock size={12} />
                      <span>{item.deadline} 마감</span>
                    </div>
                  </div>
                  
                  <div className="flex flex-col gap-1">
                    <h4 className={`font-extrabold text-gray-900 group-hover:text-blue-600 transition leading-snug ${
                      viewMode === 'GRID' ? 'text-base line-clamp-2' : 'text-lg line-clamp-1'
                    }`}>
                      {item.title}
                    </h4>
                    <span className="text-xs text-gray-400 font-semibold">{item.author}</span>
                  </div>

                  {/* 하단 진행 바 영역 (리스트 모드일 땐 너비를 적절히 조절) */}
                  <div className={`flex flex-col gap-2 pt-4 border-t border-gray-50 ${viewMode === 'GRID' ? 'mt-auto' : 'mt-2'}`}>
                    <div className="flex justify-between items-end">
                      <span className="text-lg font-black text-gray-900">{item.price}</span>
                      <div className="flex items-center gap-1 text-[11px] font-bold text-gray-500">
                        <Users size={12} />
                        <span className={item.current >= item.target ? 'text-emerald-500' : 'text-blue-500'}>{item.current}</span>
                        <span>/ {item.target}명</span>
                      </div>
                    </div>
                    <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                      <div 
                        className={`h-full rounded-full transition-all duration-500 ${item.current >= item.target ? 'bg-emerald-400' : 'bg-blue-500'}`}
                        style={{ width: `${(item.current / item.target) * 100}%` }}
                      ></div>
                    </div>
                  </div>

                </div>
              </Link>
            ))}
          </div>
          
          {/* 데이터가 많을 때만 더보기 버튼 표시되도록 동적 처리 (현재는 10개 초과 시 표시) */}
          {filteredList.length > 10 && (
            <div className="flex justify-center mt-6">
              <button className="px-6 py-2.5 bg-white border border-gray-200 text-gray-600 hover:bg-gray-50 text-sm font-bold rounded-full transition shadow-sm">
                더보기 (1 / {Math.ceil(filteredList.length / 10)})
              </button>
            </div>
          )}
        </section>

      </main>
    </div>
  );
};

export default BuyerProductsPage;