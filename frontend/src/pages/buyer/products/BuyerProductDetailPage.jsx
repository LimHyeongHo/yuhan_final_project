import React, { useState, useEffect } from 'react';
//아래 import문 삭제
// import { useParams, Link } from 'react-router-dom';
import { Clock, Users, BookOpen, ChevronLeft, CheckCircle, Share2, AlertCircle, MessageCircle, AlertTriangle, AlertOctagon, X, Copy, Heart } from 'lucide-react';
import Header from '../../../components/layout/Header';
//[추가]
import { useParams, Link, useNavigate } from 'react-router-dom';
const BuyerProductDetailPage = () => {
  // 1. 주소창에서 상품 고유 ID 추출 (예: /buyer/products/1 -> id = "1")
  const { id } = useParams();
  //[추가]
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [verification, setVerification] = useState(null); // [신규] 블록체인 검증 상태
  const [isJoined, setIsJoined] = useState(false); // 테스트용: 공구 참여 상태 토글
  const [showReceiptModal, setShowReceiptModal] = useState(false); // [신규] 스마트 영수증 모달 상태
  const [isScrapped, setIsScrapped] = useState(false); // [신규] 스크랩 상태

  useEffect(() => {
    fetch(`http://localhost:8080/api/products/${id}`)
      .then(res => {
        if (!res.ok) throw new Error("상품을 찾을 수 없습니다.");
        return res.json();
      })
      .then(data => {
        // D-Day 계산 로직
        let dDayText = '기한 없음';
        if (data.deadline) {
          const deadlineDate = new Date(data.deadline);
          const today = new Date();
          const diffTime = deadlineDate - today;
          const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
          dDayText = diffDays > 0 ? `D-${diffDays}` : (diffDays === 0 ? 'D-Day' : '마감');
        }

        setProduct({
          id: String(data.productId),
          title: data.title,
          major: data.type === 'BOOK' ? '전공 도서' : '학과 물품',
          author: data.author || '',
          publisher: data.publisher || '',
          originalPrice: data.originalPrice || data.price,
          price: data.price,
          currentCount: data.currentCount,
          targetCount: data.targetCount,
          deadline: data.deadline ? data.deadline.split('T')[0].replace(/-/g, '.') : '기한 없음',
          dDay: dDayText,
          status: data.status === 'OPEN' ? '모집 중' : '마감됨',
          thumbnail: data.imageUrl || null,
          description: data.description,
          sellerEmail: data.sellerEmail // [신규] 문의하기 버튼에서 채팅방 생성 API 호출용
        });
      })
      .catch(err => {
        console.error("상품 상세 로드 실패:", err);
        alert("상품 정보를 불러오는데 실패했습니다.");
      });

    // [신규] 블록체인 및 알라딘 가격 교차 검증 API 호출
    fetch(`http://localhost:8080/api/products/${id}/verify`)
      .then(res => res.json())
      .then(data => setVerification(data))
      .catch(err => console.error("검증 정보 로드 실패:", err));

    // [신규] 스크랩 상태 호출
    fetch(`http://localhost:8080/api/products/${id}/scrap/status`, { credentials: 'include' })
      .then(res => {
        if (res.ok) return res.json();
        return false;
      })
      .then(data => setIsScrapped(data))
      .catch(err => console.error("스크랩 상태 로드 실패:", err));
  }, [id]);

  // 공구 참여하기 버튼 클릭 핸들러
  const handleJoinToggle = async () => {
    if (!product) return;

    try {
      if (!isJoined) {
        //[추가] 참여 처리는 결제 성공 후 백엔드가 대신 해줌 — 여기선 결제 페이지로 이동만
        //이전에 있던 if문 안 쪽은 전부 삭제함. else문은 그대로
        navigate('/payment', { state: { product } });

      } else {
        // 백엔드 API 연동: 참여 인원 감소 (취소)
        const res = await fetch(`http://localhost:8080/api/products/${product.id}/cancel`, { method: 'POST' });
        if (!res.ok) throw new Error("참여 취소 실패");
        const updatedProduct = await res.json();

        // 서버에서 받은 최신 인원으로 화면 업데이트
        setProduct(prev => ({ ...prev, currentCount: updatedProduct.currentCount }));
        setIsJoined(false);
        alert('공동구매 참여가 취소되었습니다.');
      }
    } catch (error) {
      console.error(error);
      alert("서버와 통신하는 중 문제가 발생했습니다.");
    }
  };

  // [신규] 스크랩 버튼 클릭 핸들러
  const handleScrapToggle = async () => {
    if (!product) return;
    try {
      const res = await fetch(`http://localhost:8080/api/products/${product.id}/scrap`, {
        method: 'POST',
        credentials: 'include'
      });
      if (!res.ok) {
        if (res.status === 401) throw new Error('로그인이 필요합니다.');
        throw new Error('스크랩 처리에 실패했습니다.');
      }
      const newStatus = await res.json();
      setIsScrapped(newStatus);
    } catch (e) {
      alert(e.message);
    }
  };

  // [신규] 문의하기 버튼 클릭 핸들러 — 채팅방 생성 후 채팅방 목록으로 이동
  const handleChatInquiry = async () => {
    if (!localStorage.getItem('user_nickname')) {
      alert('로그인이 필요합니다');
      return;
    }
    try {
      const res = await fetch('http://localhost:8080/api/chat/rooms', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          sellerEmail: product.sellerEmail,
          productId: product.id,
          productName: product.title,
        }),
      });
      if (!res.ok) throw new Error('채팅방 생성에 실패했습니다.');
      navigate('/buyer/chat');
    } catch (e) {
      alert(e.message);
    }
  };

  // [신규] DB 해킹 시뮬레이션 (시연용)
  const handleHackSimulation = async () => {
    if (window.confirm("경고: 진짜로 DB 가격을 조작하시겠습니까? (블록체인 방어 시스템 테스트용)")) {
      try {
        const res = await fetch(`http://localhost:8080/api/products/${product.id}/simulate-hack`, { method: 'POST' });
        if (!res.ok) throw new Error("해킹 시뮬레이션 실패");
        alert("🚨 DB 데이터가 999,999원으로 위조되었습니다!\n새로고침하여 블록체인이 어떻게 막아내는지 확인하세요!");
        window.location.reload();
      } catch (error) {
        console.error(error);
        alert("해킹 시뮬레이션 실패");
      }
    }
  };

  if (!product) return <div className="p-8 text-center font-bold">도서 정보를 불러오는 중입니다...</div>;

  const progressRatio = Math.min(Math.round((product.currentCount / product.targetCount) * 100), 100);
  // [신규] 본인이 등록한 상품이면 구매자용 버튼 대신 판매자용 버튼(판매 현황 / 구매자 문의)을 보여줌
  const isOwnProduct = !!product.sellerEmail && product.sellerEmail === localStorage.getItem('email');
  // [신규] 판매자(본인 상품 아닌 경우) 또는 관리자 계정 — 구매자 행동(참여/문의) 전부 비활성화
  const isRestrictedViewer = ['ROLE_SELLER', 'ROLE_ADMIN'].includes(localStorage.getItem('user_role')) && !isOwnProduct;

  // [신규] 검증 상태에 따른 뱃지 렌더링 함수
  const renderVerificationBadge = () => {
    if (!verification) return null;
    let badgeContent = null;
    switch (verification.status) {
      case 'VALID':
        badgeContent = (
          <div className="flex items-center gap-1.5 px-3 py-1.5 bg-green-50 text-green-700 text-sm font-bold rounded-full border border-green-200 shadow-sm">
            <CheckCircle size={16} /> 블록체인 검증 완료
          </div>
        );
        break;
      case 'GOOD_DEAL':
        badgeContent = (
          <div className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-50 text-blue-700 text-sm font-bold rounded-full border border-blue-200 shadow-sm">
            <CheckCircle size={16} /> 검증 완료 (착한 가격)
          </div>
        );
        break;
      case 'ANCHORING_WARNING':
        badgeContent = (
          <div className="flex items-center gap-1.5 px-3 py-1.5 bg-orange-50 text-orange-700 text-sm font-bold rounded-full border border-orange-200 shadow-sm">
            <AlertCircle size={16} /> 시세 조작 주의
          </div>
        );
        break;
      case 'FORGED':
        badgeContent = (
          <div className="flex items-center gap-1.5 px-3 py-1.5 bg-red-50 text-red-700 text-sm font-bold rounded-full border border-red-200 shadow-sm">
            <AlertCircle size={16} /> 데이터 위변조 감지됨
          </div>
        );
        break;
      default:
        return null;
    }

    return (
      <div
        onClick={() => setShowReceiptModal(true)}
        className="cursor-pointer hover:opacity-80 transition-opacity inline-block"
        title="클릭하여 블록체인 스마트 보증서 확인하기"
      >
        {badgeContent}
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col font-sans text-gray-900">
      <Header />

      <main className="flex-grow max-w-7xl w-full mx-auto p-4 md:p-8 flex flex-col gap-6">

        {/* 뒤로가기 네비게이션 */}
        <div className="flex items-center justify-between">
          <Link to="/buyer/products" className="flex items-center gap-1 text-sm font-bold text-gray-500 hover:text-gray-900 transition">
            <ChevronLeft size={18} />
            목록으로 돌아가기
          </Link>
          <div className="flex gap-2">
            <button
              onClick={handleScrapToggle}
              className={`p-2 border border-gray-200 rounded-xl transition shadow-sm ${isScrapped ? 'text-red-500 bg-red-50 border-red-200' : 'text-gray-400 hover:text-red-500 bg-white'}`}
            >
              <Heart size={16} fill={isScrapped ? 'currentColor' : 'none'} />
            </button>
            <button className="p-2 text-gray-400 hover:text-gray-600 bg-white border border-gray-200 rounded-xl transition shadow-sm">
              <Share2 size={16} />
            </button>
          </div>
        </div>

        {/* 메인 레이아웃 그리드 (PC 2열 분할) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">

          {/* 좌측 컬럼: 이미지 및 도서 상세 설명 */}
          <div className="lg:col-span-7 flex flex-col gap-6">
            {/* 큰 이미지 박스 */}
            <div className="bg-white rounded-[32px] border border-gray-200 p-4 md:p-6 shadow-sm flex items-center justify-center aspect-[4/3] md:aspect-[16/10] overflow-hidden relative">
              <img src={product.thumbnail} alt={product.title} className="w-full h-full object-cover rounded-2xl" />
              <span className="absolute top-8 left-8 bg-red-500 text-white text-xs font-black px-3 py-1.5 rounded-md shadow-md">
                {product.dDay} 마감
              </span>
            </div>

            {/* 도서 소개글 */}
            <div className="bg-white rounded-[32px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col gap-4">
              <h3 className="text-lg font-black text-gray-950 border-b border-gray-100 pb-3">도서 및 공구 소개</h3>
              <p className="text-gray-600 text-sm md:text-base font-medium leading-relaxed whitespace-pre-wrap">
                {product.description}
              </p>
            </div>

            {/* 안전 거래 유의사항 (블록체인 강조) */}
            <div className="bg-blue-50/50 rounded-2xl p-5 border border-blue-100 flex gap-3">
              <AlertCircle className="text-blue-500 shrink-0 mt-0.5" size={20} />
              <div className="flex flex-col gap-1">
                <span className="text-sm font-bold text-blue-900">N방 안전 투명 거래 안내</span>
                <span className="text-xs text-blue-700 font-semibold leading-relaxed">
                  본 플랫폼은 영수증 검증 및 결제 내역을 블록체인 상에 투명하게 기록하여 거래 조작을 방지합니다. 모집 정원이 100% 달성되면 스마트 계약에 의해 안전하게 거래 및 배부가 확정됩니다.
                </span>
              </div>
            </div>
          </div>

          {/* 우측 컬럼: 구매 및 공구 현황 컨트롤 패널 (스티키 고정 효과 추가) */}
          <div className="lg:col-span-5 flex flex-col gap-6 lg:sticky lg:top-24">
            <div className="bg-white rounded-[32px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col gap-6">

              {/* 태그 & 학과 */}
              <div className="flex justify-between items-center">
                <span className="text-xs font-black text-blue-600 bg-blue-50 border border-blue-100 px-2.5 py-1 rounded">
                  {product.major} 전공
                </span>
                <div className="flex items-center gap-1.5 text-xs font-bold text-gray-400">
                  <Clock size={14} />
                  <span>마감일: {product.deadline}</span>
                </div>
              </div>

              {/* 제목 및 저자 정보 */}
              <div className="flex flex-col gap-1.5">
                {renderVerificationBadge()}
                {/* 폰트 수정 하려면 아래 className을 수정하면 됨 */}
                <h1 className="text-xl md:text-2xl font-black text-gray-900 leading-tight mt-2">
                  {product.title.includes('-') ? (
                    <>
                      <span>{product.title.split('-')[0].trim()}</span>
                      <span className="text-sm md:text-sm text-gray-500 font-bold block mt-1">
                        - {product.title.substring(product.title.indexOf('-') + 1).trim()}
                      </span>
                    </>
                  ) : (
                    product.title
                  )}
                </h1>
                <span className="text-sm font-bold text-gray-400">
                  {product.author} | {product.publisher}
                </span>
              </div>

              {/* 가격 정보 (정가 대비 할인가 구조 적용) */}
              <div className="bg-gray-50 rounded-2xl p-4 flex justify-between items-center">
                <div className="flex flex-col">
                  <span className="text-xs text-gray-400 line-through font-bold">정가 {product.originalPrice.toLocaleString()}원</span>
                  {(() => {
                    const diffRatio = Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100);
                    if (diffRatio > 0) {
                      return <span className="text-xs text-emerald-600 font-black mt-0.5">-{diffRatio}% 파괴 할인가</span>;
                    } else if (diffRatio < 0) {
                      return <span className="text-xs text-orange-500 font-black mt-0.5">정가보다 {Math.abs(diffRatio)}% 비쌈</span>;
                    } else {
                      return <span className="text-xs text-gray-500 font-black mt-0.5">정가와 동일</span>;
                    }
                  })()}
                </div>
                <div className="text-right">
                  <span className="text-2xl md:text-3xl font-black text-blue-600">{product.price.toLocaleString()}원</span>
                  <span className="text-xs text-gray-500 font-bold block mt-0.5">(인당 부담금)</span>
                </div>
              </div>

              {/* 공구 진행률 상황판 */}
              <div className="flex flex-col gap-3 pt-2">
                <div className="flex justify-between items-end">
                  <span className="text-sm font-black text-gray-900 flex items-center gap-1.5">
                    <Users size={16} className="text-blue-500" />
                    모집 현황 <span className="text-blue-600">{progressRatio}%</span>
                  </span>
                  <span className="text-sm font-bold text-gray-600">
                    <span className="font-black text-gray-900">{product.currentCount}</span> / {product.targetCount} 명
                  </span>
                </div>

                {/* 진행 게이지 */}
                <div className="w-full h-3 bg-gray-100 rounded-full overflow-hidden shadow-inner">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${isJoined ? 'bg-emerald-500' : 'bg-blue-600'}`}
                    style={{ width: `${progressRatio}%` }}
                  ></div>
                </div>
              </div>

              {isOwnProduct ? (
                <>
                  {/* [신규] 본인 상품: 참여하기 → 판매 현황 확인하기 */}
                  <button
                    onClick={() => navigate('/seller/status')}
                    className="w-full py-4 rounded-2xl font-black text-base md:text-lg transition-all shadow-md flex items-center justify-center gap-2 bg-blue-600 text-white hover:bg-blue-700 hover:shadow-blue-500/20"
                  >
                    판매 현황 확인하기
                  </button>

                  {/* [신규] 본인 상품: 문의하기 → 구매자 문의 확인하기 (이 상품 채팅만 필터링) */}
                  <button
                    onClick={() => navigate(`/seller/chat?productId=${product.id}`)}
                    className="w-full py-3.5 rounded-2xl font-bold text-gray-700 bg-white border-2 border-gray-200 hover:bg-gray-50 hover:border-gray-300 transition-all flex items-center justify-center gap-2"
                  >
                    <MessageCircle size={18} />
                    구매자 문의 확인하기
                  </button>
                </>
              ) : isRestrictedViewer ? (
                <>
                  {/* [신규] 판매자(남의 상품)/관리자 계정 — 전부 비활성화 */}
                  <button
                    disabled
                    title="판매자·관리자 계정은 공동구매에 참여할 수 없습니다"
                    className="w-full py-4 rounded-2xl font-black text-base md:text-lg flex items-center justify-center gap-2 bg-gray-100 text-gray-400 cursor-not-allowed"
                  >
                    공동구매 참여하기 (N빵 탑승)
                  </button>

                  <button
                    disabled
                    title="판매자·관리자 계정은 문의하기를 이용할 수 없습니다"
                    className="w-full py-3.5 rounded-2xl font-bold bg-gray-100 text-gray-400 border-2 border-gray-200 cursor-not-allowed flex items-center justify-center gap-2"
                  >
                    <MessageCircle size={18} />
                    판매자에게 문의하기 (채팅)
                  </button>
                  <p className="text-xs text-center text-gray-400 font-medium -mt-3">
                    판매자·관리자 계정으로는 이용할 수 없는 기능입니다
                  </p>
                </>
              ) : (
                <>
                  {/* 🌟 구매자 최종 액션 버튼 (참여 여부에 따른 조건부 UI) */}
                  <button
                    onClick={handleJoinToggle}
                    className={`w-full py-4 rounded-2xl font-black text-base md:text-lg transition-all shadow-md flex items-center justify-center gap-2 ${isJoined
                      ? 'bg-emerald-50 text-emerald-600 border border-emerald-200 hover:bg-emerald-100'
                      : 'bg-blue-600 text-white hover:bg-blue-700 hover:shadow-blue-500/20'
                      }`}
                  >
                    {isJoined ? (
                      <>
                        <CheckCircle size={20} />
                        공구 탑승 완료 (취소하기)
                      </>
                    ) : (
                      '공동구매 참여하기 (N빵 탑승)'
                    )}
                  </button>

                  {/* 문의하기 (채팅) 버튼 */}
                  <button
                    onClick={handleChatInquiry}
                    className="w-full py-3.5 rounded-2xl font-bold text-gray-700 bg-white border-2 border-gray-200 hover:bg-gray-50 hover:border-gray-300 transition-all flex items-center justify-center gap-2"
                  >
                    <MessageCircle size={18} />
                    판매자에게 문의하기 (채팅)
                  </button>
                </>
              )}

              {/* [신규] DB 해킹 시뮬레이션 버튼 (관리자 전용) */}
              {localStorage.getItem('user_role') === 'ROLE_ADMIN' && (
                <button
                  onClick={handleHackSimulation}
                  className="w-full mt-2 py-3 rounded-2xl font-bold text-red-50 bg-red-600 border border-red-700 hover:bg-red-700 transition-all shadow-md flex items-center justify-center gap-2"
                >
                  <span className="text-xl">☠️</span> DB 해킹 시뮬레이션 (시연용)
                </button>
              )}

            </div>
          </div>

        </div>

      </main>

      {/* 스마트 영수증 모달 렌더링 */}
      {showReceiptModal && (
        <SmartReceiptModal
          verification={verification}
          onClose={() => setShowReceiptModal(false)}
        />
      )}
    </div>
  );
};

// [신규] 스마트 영수증(보증서) 모달 컴포넌트
const SmartReceiptModal = ({ verification, onClose }) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    if (verification?.txHash) {
      navigator.clipboard.writeText(verification.txHash);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const getStatusUI = () => {
    switch (verification?.status) {
      case 'FORGED':
        return {
          icon: <AlertCircle size={32} />,
          headerIcon: <AlertCircle size={20} />,
          colorText: 'text-red-500',
          bgHeader: 'bg-red-100 text-red-600',
          bgBox: 'bg-red-50/50 border-red-200',
          title: '데이터 위변조 감지됨',
          dbHashText: 'text-red-600 font-bold'
        };
      case 'ANCHORING_WARNING':
        return {
          icon: <AlertTriangle size={32} />,
          headerIcon: <AlertTriangle size={20} />,
          colorText: 'text-orange-500',
          bgHeader: 'bg-orange-100 text-orange-600',
          bgBox: 'bg-orange-50/50 border-orange-200',
          title: '무결성 검증 완료 (시세 조작 주의)',
          dbHashText: 'text-gray-800'
        };
      case 'GOOD_DEAL':
        return {
          icon: <CheckCircle size={32} />,
          headerIcon: <CheckCircle size={20} />,
          colorText: 'text-blue-500',
          bgHeader: 'bg-blue-100 text-blue-600',
          bgBox: 'bg-blue-50/50 border-blue-200',
          title: '무결성 검증 완료 (착한 가격)',
          dbHashText: 'text-gray-800'
        };
      case 'VALID':
      default:
        return {
          icon: <CheckCircle size={32} />,
          headerIcon: <CheckCircle size={20} />,
          colorText: 'text-emerald-500',
          bgHeader: 'bg-emerald-100 text-emerald-600',
          bgBox: 'bg-emerald-50/50 border-emerald-200',
          title: '무결성 검증 완료',
          dbHashText: 'text-gray-800'
        };
    }
  };

  const ui = getStatusUI();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* 딤 배경 (Blur 효과) */}
      <div
        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* 모달 컨텐츠 (Glassmorphism 카드) */}
      <div className="relative w-full max-w-md bg-white/90 backdrop-blur-md border border-white/40 shadow-[0_0_40px_rgba(59,130,246,0.15)] rounded-3xl overflow-hidden flex flex-col transform transition-all">

        {/* 헤더 */}
        <div className="px-6 py-5 border-b border-gray-200/60 flex justify-between items-center bg-white/50">
          <div className="flex items-center gap-2">
            <div className={`p-1.5 rounded-full ${ui.bgHeader}`}>
              {ui.headerIcon}
            </div>
            <h2 className="text-lg font-black text-gray-900">블록체인 스마트 보증서</h2>
          </div>
          <button onClick={onClose} className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition">
            <X size={20} />
          </button>
        </div>

        {/* 바디 (상세 데이터) */}
        <div className="p-6 flex flex-col gap-6">

          {/* 상태 배지 영역 */}
          <div className="flex flex-col items-center justify-center gap-2 py-2">
            <div className={`${ui.colorText} font-bold flex flex-col items-center gap-1`}>
              {ui.icon}
              <span className="text-lg text-center">{ui.title}</span>
            </div>
            <p className="text-xs text-gray-500 text-center font-medium">
              이 거래의 가격과 정보는 이더리움 블록체인에 영구적으로 기록되어 절대 임의로 조작할 수 없습니다.
            </p>
          </div>

          <div className="w-full h-px bg-gray-200/50" />

          {/* 트랜잭션 정보 */}
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-500">트랜잭션 해시 (TxHash)</span>
              <div className="flex items-center gap-2 bg-gray-100/80 p-3 rounded-xl border border-gray-200/50">
                <span className="text-xs font-mono text-gray-700 truncate flex-1">
                  {verification?.txHash || 'Pending...'}
                </span>
                <button
                  onClick={handleCopy}
                  className="p-1.5 bg-white rounded-md shadow-sm border border-gray-200 text-gray-500 hover:text-blue-600 transition"
                  title="해시 복사하기"
                >
                  <Copy size={14} />
                </button>
              </div>
              {copied && <span className="text-[10px] text-blue-600 font-bold ml-1">복사되었습니다!</span>}
            </div>

            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-500">원본 해시 조합 (ID_ISBN_Price)</span>
              <div className="bg-gray-100/80 p-3 rounded-xl border border-gray-200/50">
                <span className="text-xs font-mono text-gray-700 break-all">
                  {verification?.targetData || '데이터 없음'}
                </span>
              </div>
            </div>

            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-500">블록체인 저장 해시 vs DB 해시</span>
              <div className={`p-3 rounded-xl border ${ui.bgBox} flex flex-col gap-2`}>
                <div className="flex flex-col">
                  <span className="text-[10px] font-bold text-gray-400">BC Hash (스마트 컨트랙트)</span>
                  <span className="text-xs font-mono text-gray-800 truncate">
                    {verification?.blockchainHash || '조회 실패'}
                  </span>
                </div>
                <div className="w-full h-px bg-gray-200/50" />
                <div className="flex flex-col">
                  <span className="text-[10px] font-bold text-gray-400">DB Hash (현재 데이터)</span>
                  <span className={`text-xs font-mono truncate ${ui.dbHashText}`}>
                    {verification?.dbHash || '계산 실패'}
                  </span>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
};

export default BuyerProductDetailPage;