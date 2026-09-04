import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, Layers, AlertTriangle, CheckCircle, Clock, Filter, Trash2, ShieldAlert, History, Info } from 'lucide-react';
import Header from '../../../components/layout/Header'; // 공통 헤더 연결

const GroupManagementPage = () => {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [activeTab, setActiveTab] = useState('전체 거래');
  const [showOnlyOpen, setShowOnlyOpen] = useState(false);
  const [syncLogs, setSyncLogs] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const filteredProducts = products.filter(item => {
    if (showOnlyOpen && item.status !== 'OPEN') return false;
    
    if (activeTab === '승인 대기' && item.status !== '승인 대기') return false;
    
    // 신고를 받아 이상 상태인 경우 (suspicious = true)
    if (activeTab === '신고/이상 상태') {
      if (!item.suspicious) return false;
    }
    
    // 인원이 부족해서 실패한 경우 (CLOSED_FAIL 이면서 suspicious가 아님)
    if (activeTab === '모집 실패') {
      if (item.status !== 'CLOSED_FAIL' || item.suspicious) return false;
    }
    
    return true;
  });

  const fetchProductsAndLogs = () => {
    fetch('http://localhost:8080/api/admin/products', { credentials: 'include' })
      .then(res => res.json())
      .then(data => setProducts(data))
      .catch(err => console.error("상품 데이터 로드 실패:", err));

    fetch('http://localhost:8080/api/admin/logs?type=SECURITY', { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data)) {
          setSyncLogs(data.slice(0, 5));
        }
      })
      .catch(error => console.error("로그 로딩 실패:", error));
  };

  useEffect(() => {
    fetchProductsAndLogs();
    const interval = setInterval(fetchProductsAndLogs, 3000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    setCurrentPage(1);
  }, [activeTab, showOnlyOpen]);

  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [rejectingProductId, setRejectingProductId] = useState(null);
  const [rejectReason, setRejectReason] = useState("");

  const openRejectModal = (id) => {
    setRejectingProductId(id);
    setRejectReason("");
    setIsRejectModalOpen(true);
  };

  const closeRejectModal = () => {
    setIsRejectModalOpen(false);
    setRejectingProductId(null);
    setRejectReason("");
  };

  const submitReject = () => {
    if (!rejectReason.trim()) {
      alert("거절 사유를 입력해주세요.");
      return;
    }
    
    fetch(`http://localhost:8080/api/admin/products/${rejectingProductId}/reject`, { 
      method: 'POST', 
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ reason: rejectReason }),
      credentials: 'include' 
    })
      .then(res => {
        if (!res.ok) throw new Error('상품 거절/삭제 실패');
        alert('상품이 성공적으로 거절(삭제)되었으며 판매자에게 사유가 전송되었습니다.');
        closeRejectModal();
        fetchProductsAndLogs();
      })
      .catch(err => {
        console.error(err);
        alert(err.message);
      });
  };

  const totalProducts = products.length;
  const activeProducts = products.filter(p => p.status === 'OPEN').length;
  const suspiciousProducts = products.filter(p => p.suspicious).length;

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      
      {/* 1. 글로벌 공통 헤더 네비게이션 */}
      <Header />

      {/* 2. 상단 묵직한 다크 배너 섹션 (SecurityLogPage 디자인 무드 완전 동화) */}
      <section className="bg-slate-900 text-white py-12 px-6 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-col gap-2">
          <span className="bg-blue-500/20 text-blue-400 text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full w-max border border-blue-500/30">
            Operations Hub
          </span>
          <h2 className="text-4xl font-extrabold tracking-tight mt-1">
            공동구매 관리
          </h2>
          <p className="text-slate-400 font-medium text-base max-w-2xl mt-1">
            플랫폼 내에서 개설된 모든 대학 교재 공동구매 프로젝트를 실시간으로 모니터링하고, 허위 매물 및 위변조 의심 거래를 선제적으로 제어합니다.
          </p>
        </div>
      </section>

      {/* 3. 메인 콘텐츠 영역 */}
      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col gap-8">
        
        {/* 상단 3종 대시보드 요약 메트릭 카드 */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* 카드 1: 전체 공동구매 */}
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">전체 공동구매 목록</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">{totalProducts} 건</h3>
              <span className="text-blue-600 text-xs font-bold mt-1 flex items-center gap-1">
                <Layers size={14} /> LIVE PROJECTS
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center text-blue-600">
              <BookOpen size={24} />
            </div>
          </div>

          {/* 카드 2: 진행 중인 거래 */}
          <div className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">현재 체결 진행 건수</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">{activeProducts} 건</h3>
              <span className="text-emerald-600 text-xs font-bold mt-1 flex items-center gap-1">
                <Clock size={14} /> ACTIVE TRADING
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-600">
              <CheckCircle size={24} />
            </div>
          </div>

          {/* 카드 3: 이상 상태 감지 */}
          <div className="bg-white rounded-[24px] p-6 border-2 border-red-100 shadow-sm bg-red-50/10 flex justify-between items-center">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-red-400 uppercase tracking-wider">위변조 및 허위 의심</span>
              <h3 className="text-3xl font-black text-red-600 mt-1">{suspiciousProducts} 건</h3>
              <span className="text-red-500 text-xs font-extrabold mt-1 uppercase tracking-tight flex items-center gap-1 animate-pulse">
                <AlertTriangle size={14} /> HIGH RISK DETECTED
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center text-red-600">
              <ShieldAlert size={24} />
            </div>
          </div>
        </section>

        {/* 하단 서적 관리 제어 레이아웃 그리드 (2/3 카드 뷰 + 1/3 서브 로그 모니터 뷰) */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* 좌측 패널: 공동구매 서적 제어 관리 현황 (2/3 영역) */}
          <div className="lg:col-span-2 flex flex-col gap-4">
            
            {/* 필터 세션 탭 라인 */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-4 rounded-2xl border border-gray-200 shadow-sm mb-2">
              <div className="flex gap-2 bg-gray-100 p-1 rounded-xl">
                {['전체 거래', '승인 대기', '신고/이상 상태', '모집 실패'].map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    className={`px-4 py-2 text-xs font-bold rounded-lg transition ${
                      activeTab === tab 
                        ? 'bg-blue-600 text-white shadow-md' 
                        : 'text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    {tab}
                  </button>
                ))}
              </div>
              <button 
                onClick={() => setShowOnlyOpen(!showOnlyOpen)}
                className={`flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-xl text-xs font-bold transition ${
                  showOnlyOpen ? 'bg-blue-50 text-blue-600' : 'text-gray-600 bg-white hover:bg-gray-50'
                }`}
              >
                <Filter size={14} /> 진행중인 상품
              </button>
            </div>

            {/* 메인 도서 등록 리스트 피드 */}
            <div className="flex flex-col gap-4">
              {(() => {
                const indexOfLastItem = currentPage * itemsPerPage;
                const indexOfFirstItem = indexOfLastItem - itemsPerPage;
                const currentProducts = filteredProducts.slice(indexOfFirstItem, indexOfLastItem);
                
                return currentProducts.map((item) => (
                  <div 
                    key={item.id} 
                  className={`bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex flex-col sm:flex-row items-start sm:items-center gap-6 transition-all hover:shadow-md ${
                    item.suspicious ? 'border-red-200 ring-2 ring-red-50/50' : ''
                  }`}
                >
                  {/* 더미 책 커버 레이아웃 */}
                  <div className="w-16 h-24 bg-gradient-to-br from-gray-100 to-gray-200 rounded-xl shadow-inner border border-gray-200 flex items-center justify-center text-gray-400 flex-shrink-0">
                    <BookOpen size={24} />
                  </div>
                  
                  {/* 정보 설명 상세 정보 피드 */}
                  <div className="flex-grow flex flex-col gap-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-[10px] font-mono font-bold text-gray-400">ID: #{item.id}</span>
                      <span className={`text-[10px] font-black px-2.5 py-0.5 rounded-md uppercase ${
                        item.status === 'CLOSED_FAIL' ? 'bg-red-50 text-red-700 border border-red-100' :
                        item.status === 'CLOSED_SUCCESS' ? 'bg-green-50 text-green-700 border border-green-100' : 
                        'bg-blue-50 text-blue-700 border border-blue-100'
                      }`}>
                        {item.status}
                      </span>
                      {item.suspicious && (
                        <span className="bg-red-50 text-red-600 text-[10px] font-black px-2.5 py-0.5 rounded-md flex items-center gap-1 border border-red-100">
                          <ShieldAlert size={12} /> 위험 노출
                        </span>
                      )}
                    </div>
                    <h4 className="text-base font-extrabold text-gray-950 mt-1 tracking-tight">{item.title.split('-')[0].trim()}</h4>
                    <p className="text-xs text-gray-500 font-mono mt-0.5">
                      SELLER: <span className="font-semibold text-gray-700">{item.seller}</span> | PRICE: <span className="font-semibold text-gray-900">₩{item.price.toLocaleString()}</span>
                    </p>
                    
                    {/* 달성도 프로그레스 바 레이아웃 */}
                    <div className="flex items-center gap-3 w-full max-w-md mt-3">
                      <div className="flex-grow h-1.5 bg-gray-100 rounded-full overflow-hidden border border-gray-200/50">
                        <div 
                          className={`h-full rounded-full transition-all duration-500 ${item.suspicious ? 'bg-red-500' : 'bg-blue-600'}`}
                          style={{ width: `${item.ratio}%` }}
                        ></div>
                      </div>
                      <span className="text-xs font-bold text-gray-700 w-8 text-right">{item.ratio}%</span>
                    </div>
                  </div>

                  {/* 액션 제어 버튼 패널 영역 */}
                  <div className="flex sm:flex-col items-end gap-3 justify-between w-full sm:w-auto self-stretch pt-4 sm:pt-0 border-t sm:border-t-0 border-gray-100">
                    <div className="flex items-center gap-2">
                      <button onClick={() => navigate(`/buyer/products/${item.id}`)} className="p-2 text-gray-400 hover:text-blue-500 bg-gray-50 rounded-lg hover:bg-gray-100 transition" title="상품 상세페이지 이동"><Info size={16} /></button>
                    </div>
                    <div className="flex gap-2">
                      <button onClick={() => openRejectModal(item.id)} className="p-2 px-3 bg-red-50 text-red-600 text-xs font-bold rounded-xl hover:bg-red-100 transition flex items-center gap-1">
                        <Trash2 size={14} /> 거절
                      </button>
                      <button className="p-2 px-3 bg-blue-600 text-white text-xs font-bold rounded-xl hover:bg-blue-700 shadow-md shadow-blue-100 transition">
                        거래 승인
                      </button>
                    </div>
                  </div>

                </div>
                ));
              })()}
              
              {/* 페이지네이션 컨트롤 */}
              {Math.ceil(filteredProducts.length / itemsPerPage) > 1 && (
                <div className="flex justify-center items-center gap-2 mt-4 pb-2">
                  <button 
                    onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                    disabled={currentPage === 1}
                    className="px-3 py-1 border border-gray-200 rounded-md text-sm disabled:opacity-50 hover:bg-gray-50 bg-white"
                  >
                    이전
                  </button>
                  <span className="text-sm text-gray-600 font-medium px-2">
                    {currentPage} / {Math.ceil(filteredProducts.length / itemsPerPage)}
                  </span>
                  <button 
                    onClick={() => setCurrentPage(prev => Math.min(prev + 1, Math.ceil(filteredProducts.length / itemsPerPage)))}
                    disabled={currentPage === Math.ceil(filteredProducts.length / itemsPerPage)}
                    className="px-3 py-1 border border-gray-200 rounded-md text-sm disabled:opacity-50 hover:bg-gray-50 bg-white"
                  >
                    다음
                  </button>
                </div>
              )}
            </div>
          </div>

          {/* 우측 패널: 실시간 시스템 검증 로그 모니터 (1/3 영역) */}
          <div className="lg:col-span-1 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col justify-between">
            <div className="flex flex-col gap-5">
              <div>
                <h3 className="text-xl font-extrabold text-gray-950 tracking-tight">검증 동기화 로그</h3>
                <p className="text-xs text-gray-400 mt-0.5">플랫폼 서적 거래의 데이터 무결성 체크 히스토리입니다.</p>
              </div>

              <div className="flex flex-col gap-4 border-t border-gray-100 pt-4">
                {syncLogs.length === 0 ? (
                  <div className="text-center text-xs text-gray-400 py-4">동기화 로그가 없습니다.</div>
                ) : (
                  syncLogs.map((log) => (
                    <div key={log.id} className="flex items-start gap-3 p-2 hover:bg-gray-50 rounded-xl transition">
                      <div className={`mt-0.5 ${log.status === 'SUCCESS' || log.status === 'VALID' ? 'text-blue-600' : 'text-red-600 animate-pulse'}`}>
                        {log.status === 'SUCCESS' || log.status === 'VALID' ? <CheckCircle size={16} /> : <AlertTriangle size={16} />}
                      </div>
                      <div className="flex flex-col gap-0.5">
                        <span className="text-[10px] font-bold text-gray-400 flex items-center gap-1 font-mono">
                          <Clock size={10} /> {log.timestamp.split(' ')[1] || log.timestamp}
                        </span>
                        <p className={`text-xs font-medium leading-relaxed ${log.status === 'SUCCESS' || log.status === 'VALID' ? 'text-gray-700' : 'text-red-600'}`}>
                          {log.displayId} 검증 {(log.status === 'SUCCESS' || log.status === 'VALID') ? '통과 및 동기화 완료' : '불일치 (위변조 감지)'}
                        </p>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="mt-8 pt-4 border-t border-gray-100 flex items-center justify-between text-xs font-bold text-gray-400">
              <span>업데이트 주기</span>
              <span className="text-blue-600 font-extrabold font-mono">3s (LIVE)</span>
            </div>
          </div>

        </section>
      </main>

      {/* 거절 사유 입력 모달 */}
      {isRejectModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/50 backdrop-blur-sm">
          <div className="bg-white rounded-[24px] p-6 max-w-md w-full shadow-xl flex flex-col gap-4">
            <div className="flex items-center gap-2 text-red-600 border-b border-gray-100 pb-4">
              <ShieldAlert size={20} />
              <h3 className="text-lg font-bold">상품 강제 거절(삭제)</h3>
            </div>
            <div className="flex flex-col gap-2">
              <label className="text-sm font-bold text-gray-700">거절 사유 입력 (판매자에게 전송됩니다)</label>
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="허위 매물, 가품 의심, 규정 위반 등 명확한 사유를 기재해 주세요."
                className="w-full h-32 p-3 text-sm border border-gray-200 rounded-xl bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none transition"
              ></textarea>
            </div>
            <div className="flex justify-end gap-2 mt-2 pt-4 border-t border-gray-100">
              <button 
                onClick={closeRejectModal}
                className="px-4 py-2 text-sm font-bold text-gray-600 bg-gray-100 rounded-xl hover:bg-gray-200 transition"
              >
                취소
              </button>
              <button 
                onClick={submitReject}
                className="px-4 py-2 text-sm font-bold text-white bg-red-600 rounded-xl hover:bg-red-700 shadow-md shadow-red-200 transition flex items-center gap-1.5"
              >
                <Trash2 size={16} /> 거절 확인 및 사유 전송
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default GroupManagementPage;