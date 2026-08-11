import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Store, BookOpen, Package, Search, Filter, Edit2, Trash2, CheckCircle, Clock, ExternalLink } from 'lucide-react';
import Header from '../../../components/layout/Header';

const SellerProductsPage = () => {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);

  const fetchProduct = () => {
    // [수정] sellerId=1 고정 하드코딩 → 로그인한 본인 상품만 조회
    fetch('http://localhost:8080/api/products/seller/me', { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        const formattedData = data.map(item => ({
          id: item.productId,
          type: item.type,
          title: item.title,
          price: item.price,
          currentCount: item.currentCount,
          targetCount: item.targetCount,
          status: item.status === 'OPEN' ? (item.currentCount >= item.targetCount ? '목표달성' : '진행중') : '마감됨',
          date: item.createdAt ? item.createdAt.split('T')[0].replace(/-/g, '.') : '알 수 없음'
        }));
        setProducts(formattedData);
      })
      .catch(err => console.error("상품 목록 로드 실패:", err));
  };

  React.useEffect(() => {
    fetchProducts();
  }, []);

  const handleDelete = async (id, currentCount) => {
    if (currentCount > 0) {
      const confirmFirst = window.confirm(`현재 참여자가 ${currentCount}명 있습니다. 정말 삭제하시겠습니까?`);
      if (!confirmFirst) return;
    }

    const confirmDelete = window.confirm("프로젝트를 삭제하면 복구할 수 없으며 참여 내역도 함께 삭제됩니다. 계속하시겠습니까?");
    if (!confirmDelete) return;

    try {
      const response = await fetch(`http://localhost:8080/api/products/${id}`, {
        method: 'DELETE'
      });
      if (response.ok) {
        alert("성공적으로 삭제되었습니다.");
        fetchProducts(); // 리스트 갱신
      } else {
        alert("삭제에 실패했습니다.");
      }
    } catch (error) {
      console.error("삭제 중 오류 발생:", error);
      alert("삭제 중 오류가 발생했습니다.");
    }
  };

  // 1. 상태 관리 (탭, 필터, 필터창 열림/닫힘)
  const [activeTab, setActiveTab] = useState('ALL');
  const [filterStatus, setFilterStatus] = useState('ALL'); // 'ALL', '진행중', '목표달성'
  const [isFilterOpen, setIsFilterOpen] = useState(false); // 필터 드롭다운 메뉴 상태

  // 2. 다중 필터링 로직 (탭과 상태를 동시에 검사!)
  const filteredProducts = products.filter(item => {
    const matchType = activeTab === 'ALL' || item.type === activeTab;
    const matchStatus = filterStatus === 'ALL' || item.status === filterStatus;
    return matchType && matchStatus; // 두 조건을 모두 만족하는 데이터만 통과
  });

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <Header />

      <section className="bg-slate-900 text-white py-12 px-6 shadow-md">
        {/* ... (상단 배너 코드는 기존과 동일) ... */}
        <div className="max-w-7xl mx-auto flex flex-col gap-2">
          <span className="bg-emerald-500/20 text-emerald-400 text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full w-max border border-emerald-500/30 flex items-center gap-1.5">
            <Store size={14} /> Seller Hub
          </span>
          <h2 className="text-4xl font-extrabold tracking-tight mt-1">판매 현황</h2>
          <p className="text-slate-400 font-medium text-base max-w-2xl mt-1">내가 개설한 전공 도서 및 학과 물품의 공동구매 진행률을 확인하고 게시글을 관리하세요.</p>
        </div>
      </section>

      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col gap-8">

        {/* ... (상단 통계 카드 3종도 기존과 동일) ... */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1"><span className="text-xs font-bold text-gray-400 uppercase tracking-wider">총 등록 프로젝트</span><h3 className="text-3xl font-black text-gray-950 mt-1">{products.length} 건</h3></div>
            <div className="w-12 h-12 rounded-full bg-gray-50 flex items-center justify-center text-gray-500"><Package size={24} /></div>
          </div>
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1"><span className="text-xs font-bold text-gray-400 uppercase tracking-wider">모집 진행 중</span><h3 className="text-3xl font-black text-gray-950 mt-1">{products.filter(p => p.status === '진행중').length} 건</h3></div>
            <div className="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center text-blue-600"><Clock size={24} /></div>
          </div>
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1"><span className="text-xs font-bold text-gray-400 uppercase tracking-wider">목표 달성 완료</span><h3 className="text-3xl font-black text-emerald-600 mt-1">{products.filter(p => p.status === '목표달성').length} 건</h3></div>
            <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-600"><CheckCircle size={24} /></div>
          </div>
        </section>

        <section className="flex flex-col gap-4">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-4 rounded-2xl border border-gray-200 shadow-sm">

            {/* 탭 버튼 영역 */}
            <div className="flex gap-2 bg-gray-100 p-1 rounded-xl">
              {[{ id: 'ALL', label: '전체 보기' }, { id: 'BOOK', label: '전공 도서' }, { id: 'ITEM', label: '학과 물품' }].map((tab) => (
                <button
                  key={tab.id} onClick={() => setActiveTab(tab.id)}
                  className={`px-4 py-2 text-xs font-bold rounded-lg transition ${activeTab === tab.id ? 'bg-white text-blue-600 shadow-sm ring-1 ring-gray-900/5' : 'text-gray-600 hover:bg-gray-200/50'}`}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            <div className="flex items-center gap-2 w-full sm:w-auto">
              <div className="relative flex-grow sm:flex-grow-0">
                <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                <input type="text" placeholder="프로젝트 검색..." className="w-full sm:w-64 pl-9 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-blue-500 transition" />
              </div>

              {/* 3. ✨ 상태 필터 드롭다운 메뉴 영역 ✨ */}
              <div className="relative">
                <button
                  onClick={() => setIsFilterOpen(!isFilterOpen)}
                  className={`flex items-center gap-2 px-3 py-2 border rounded-xl transition shrink-0 text-sm font-bold ${isFilterOpen || filterStatus !== 'ALL' ? 'border-blue-300 bg-blue-50 text-blue-600' : 'border-gray-200 text-gray-600 bg-white hover:bg-gray-50'
                    }`}
                >
                  <Filter size={18} />
                  {/* 선택된 필터가 있으면 버튼에 글자를 보여줍니다 */}
                  {filterStatus !== 'ALL' && <span className="hidden sm:inline">{filterStatus}</span>}
                </button>

                {/* 드롭다운 메뉴 (열림 상태일 때만 보임) */}
                {isFilterOpen && (
                  <div className="absolute right-0 top-full mt-2 w-36 bg-white rounded-xl shadow-xl border border-gray-100 overflow-hidden z-10 flex flex-col">
                    {[
                      { id: 'ALL', label: '전체 상태' },
                      { id: '진행중', label: '모집 진행중' },
                      { id: '목표달성', label: '목표 달성' }
                    ].map(status => (
                      <button
                        key={status.id}
                        onClick={() => {
                          setFilterStatus(status.id);
                          setIsFilterOpen(false); // 선택하면 메뉴 닫기
                        }}
                        className={`text-left px-4 py-3 text-sm font-bold transition hover:bg-gray-50 ${filterStatus === status.id ? 'text-blue-600 bg-blue-50/50' : 'text-gray-700'
                          }`}
                      >
                        {status.label}
                      </button>
                    ))}
                  </div>
                )}
              </div>

            </div>
          </div>

          {/* 메인 리스트 그리드 (기존 3열 Grid 시스템 유지) */}
          <div className="flex flex-col gap-4">
            {filteredProducts.map((item) => {
              const ratio = Math.min(Math.round((item.currentCount / item.targetCount) * 100), 100);
              const isCompleted = item.status === '목표달성';

              return (
                <div key={item.id} className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm grid grid-cols-1 md:grid-cols-12 gap-6 items-center hover:shadow-md transition">
                  <div className="md:col-span-6 flex items-center gap-5">
                    <div className={`w-14 h-14 rounded-2xl flex items-center justify-center flex-shrink-0 border ${item.type === 'BOOK' ? 'bg-blue-50 border-blue-100 text-blue-600' : 'bg-purple-50 border-purple-100 text-purple-600'}`}>
                      {item.type === 'BOOK' ? <BookOpen size={24} /> : <Package size={24} />}
                    </div>
                    <div className="flex flex-col gap-1 w-full overflow-hidden">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className={`text-[10px] font-black px-2 py-0.5 rounded uppercase border ${isCompleted ? 'bg-emerald-50 text-emerald-600 border-emerald-100' : 'bg-gray-100 text-gray-600 border-gray-200'}`}>
                          {item.status}
                        </span>
                        <span className="text-[10px] font-mono font-bold text-gray-400">#{item.id}</span>
                        <span className="text-[10px] text-gray-400 font-medium ml-1">{item.date} 개설</span>
                      </div>
                      <h4 className="text-lg font-extrabold text-gray-900 tracking-tight leading-tight mt-0.5 truncate">{item.title}</h4>
                      <p className="text-sm font-bold text-gray-600 mt-1">₩{item.price.toLocaleString()}</p>
                    </div>
                  </div>
                  <div className="md:col-span-4 w-full flex flex-col gap-1.5">
                    <div className="flex justify-between text-xs font-bold text-gray-600">
                      <span>진행률 <span className={isCompleted ? 'text-emerald-600' : 'text-blue-600'}>{ratio}%</span></span>
                      <span>{item.currentCount} / {item.targetCount} 명</span>
                    </div>
                    <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                      <div className={`h-full rounded-full transition-all duration-500 ${isCompleted ? 'bg-emerald-500' : 'bg-blue-600'}`} style={{ width: `${ratio}%` }}></div>
                    </div>
                  </div>
                  <div className="md:col-span-2 flex items-center gap-2 justify-start md:justify-end border-t md:border-t-0 pt-4 md:pt-0 border-gray-100">
                    <button onClick={() => navigate(`/buyer/products/${item.id}`)} className="flex items-center justify-center p-2.5 text-gray-400 hover:text-blue-600 bg-gray-50 rounded-xl hover:bg-blue-50 transition"><ExternalLink size={18} /></button>
                    <button onClick={() => navigate(`/seller/products/edit/${item.id}`)} className="flex items-center justify-center p-2.5 text-gray-400 hover:text-gray-700 bg-gray-50 rounded-xl hover:bg-gray-200 transition"><Edit2 size={18} /></button>
                    <button onClick={() => handleDelete(item.id, item.currentCount)} className="flex items-center justify-center p-2.5 text-red-400 hover:text-red-600 bg-red-50/50 rounded-xl hover:bg-red-50 transition"><Trash2 size={18} /></button>
                  </div>
                </div>
              );
            })}

            {filteredProducts.length === 0 && (
              <div className="bg-white rounded-[24px] p-12 border border-gray-200 flex flex-col items-center justify-center text-gray-400 gap-3">
                <Package size={40} className="opacity-20" />
                <p className="text-sm font-bold">조건에 맞는 프로젝트가 없습니다.</p>
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  );
};

export default SellerProductsPage;