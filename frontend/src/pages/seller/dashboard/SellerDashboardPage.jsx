import React, { useState } from 'react';
import { Store, TrendingUp, Package, MessageSquare, PlusCircle, ArrowRight, BookOpen, Clock, Bell } from 'lucide-react';
import { Link } from 'react-router-dom';
import Header from '../../../components/layout/Header'; // 공통 헤더

const SellerDashboardPage = () => {
  const [listings, setListings] = useState([]);
  const [participations, setParticipations] = useState([]);
  const [unreadChatCount, setUnreadChatCount] = useState(0);

  // [수정] 하드코딩된 "+₩77,000 이번 주" → 최근 7일 이내 참여 건의 실제 가격 합산
  const weeklyRevenue = participations.reduce((sum, part) => {
    const joinedAt = part.joinDate ? new Date(part.joinDate) : null;
    const isWithinWeek = joinedAt && (Date.now() - joinedAt.getTime()) <= 7 * 24 * 60 * 60 * 1000;
    return isWithinWeek ? sum + (part.product?.price || 0) : sum;
  }, 0);

  React.useEffect(() => {
    // [수정] 하드코딩된 "12건" → 실제 채팅방 안읽음 메시지 합산, 새로고침 없이도 20초마다 갱신
    const loadUnreadChatCount = () => {
      fetch(`http://${window.location.hostname}:8080/api/chat/rooms`, { credentials: 'include' })
        .then(res => {
          if (!res.ok) throw new Error('Failed to fetch chat rooms');
          return res.json();
        })
        .then(rooms => {
          setUnreadChatCount(rooms.reduce((sum, r) => sum + (r.unreadCount || 0), 0));
        })
        .catch(err => console.error(err));
    };

    loadUnreadChatCount();
    const intervalId = setInterval(loadUnreadChatCount, 20000);
    return () => clearInterval(intervalId);
  }, []);

  React.useEffect(() => {
    // [수정] sellerId=1 고정 하드코딩 → 로그인한 본인 상품만 조회
    // 1. 내 판매 현황 목록 불러오기
    fetch('http://localhost:8080/api/products/seller/me', { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error('Failed to fetch products');
        return res.json();
      })
      .then(data => {
        const formattedData = data.map(item => ({
          id: item.productId,
          title: item.title,
          price: item.price,
          current: item.currentCount,
          target: item.targetCount,
          status: item.status === 'OPEN' ? (item.currentCount >= item.targetCount ? '목표달성' : '진행중') : '마감됨',
        }));
        setListings(formattedData);
      })
      .catch(err => console.error(err));

    // 2. 누군가 참여한 최근 내역 불러오기 (최근 알림용)
    fetch('http://localhost:8080/api/products/seller/me/participations', { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error('Failed to fetch participations');
        return res.json();
      })
      .then(data => {
        setParticipations(data);
      })
      .catch(err => console.error(err));
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">

      {/* 1. 글로벌 헤더 */}
      <Header />

      {/* 2. 판매자 전용 다크 배너 */}
      <section className="bg-slate-900 text-white py-12 px-6 shadow-md">
        <div className="max-w-7xl mx-auto flex flex-col gap-2">
          <span className="bg-emerald-500/20 text-emerald-400 text-xs font-bold uppercase tracking-widest px-3 py-1 rounded-full w-max border border-emerald-500/30 flex items-center gap-1.5">
            <Store size={14} /> Seller Hub
          </span>
          <h2 className="text-4xl font-extrabold tracking-tight mt-1">
            판매자 대시보드
          </h2>
          <p className="text-slate-400 font-medium text-base max-w-2xl mt-1">
            내가 개설한 전공책 공동구매 현황과 누적 판매 수익, 그리고 구매자들의 최근 문의를 한눈에 관리하세요.
          </p>
        </div>
      </section>

      {/* 3. 메인 콘텐츠 영역 */}
      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col gap-8">

        {/* 상단 3종 요약 통계 카드 */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* 카드 1: 진행 중인 공동구매 */}
          <Link to="/seller/status" className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center transition hover:shadow-md">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">진행 중인 공동구매</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">{listings.filter(l => l.status === '진행중').length} 건</h3>
              <span className="text-blue-600 text-xs font-bold mt-1 flex items-center gap-1">
                <Package size={14} /> ACTIVE LISTINGS
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center text-blue-600">
              <BookOpen size={24} />
            </div>
          </Link>

          {/* 카드 2: 누적 판매 예상 수익 */}
          <Link to="/seller/analytics" className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center transition hover:shadow-md">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">누적 판매 (예상 수익)</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">₩{listings.reduce((acc, curr) => acc + (curr.price * curr.current), 0).toLocaleString()}</h3>
              <span className="text-emerald-600 text-xs font-bold mt-1 flex items-center gap-1">
                <TrendingUp size={14} /> +₩{weeklyRevenue.toLocaleString()} 이번 주
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-600">
              <Store size={24} />
            </div>
          </Link>

          {/* 카드 3: 신규 문의 내역 */}
          <Link to="/seller/chat" className="bg-white rounded-[24px] p-6 border border-gray-200 shadow-sm flex justify-between items-center transition hover:shadow-md">
            <div className="flex flex-col gap-1">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">신규 채팅/문의</span>
              <h3 className="text-3xl font-black text-gray-950 mt-1">{unreadChatCount} 건</h3>
              <span className="text-orange-500 text-xs font-bold mt-1 flex items-center gap-1">
                {unreadChatCount > 0 ? '미확인 메시지가 있습니다' : '새 메시지가 없습니다'}
              </span>
            </div>
            <div className="w-12 h-12 rounded-full bg-orange-50 flex items-center justify-center text-orange-500">
              <MessageSquare size={24} />
            </div>
          </Link>
        </section>

        {/* 하단 스플릿 레이아웃 (2/3 게시글 관리 + 1/3 빠른 알림) */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">

          {/* 좌측 패널: 내 판매 게시글 현황 (2/3) */}
          <div className="lg:col-span-2 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col gap-6">
            <div className="flex justify-between items-center border-b border-gray-100 pb-4">
              <div>
                <h3 className="text-xl font-extrabold text-gray-950 tracking-tight">내 판매 현황</h3>
                <p className="text-xs text-gray-400 mt-1">진행 중인 공동구매의 목표 달성률을 확인하세요.</p>
              </div>
              <Link to="/seller/products" className="flex items-center gap-2 px-4 py-2 bg-slate-900 text-white rounded-xl text-sm font-bold hover:bg-slate-800 transition shadow-md shadow-slate-200">
                <PlusCircle size={16} /> 새 물품 등록
              </Link>
            </div>

            <div className="flex flex-col gap-4">
              {listings.slice(0, 3).map((item) => {
                const ratio = Math.min(Math.round((item.current / item.target) * 100), 100);
                const isCompleted = item.status === '목표달성';

                return (
                  <div key={item.id} className="p-5 border border-gray-100 rounded-2xl hover:bg-gray-50 transition group">
                    <div className="flex justify-between items-start mb-3">
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-2">
                          <span className={`text-[10px] font-black px-2 py-0.5 rounded uppercase ${isCompleted ? 'bg-blue-50 text-blue-600' : 'bg-emerald-50 text-emerald-600'
                            }`}>
                            {item.status}
                          </span>
                          <span className="text-[10px] font-mono text-gray-400">#{item.id}</span>
                        </div>
                        <h4 className="text-base font-bold text-gray-900">{item.title}</h4>
                      </div>
                      <span className="font-bold text-gray-900">₩{item.price.toLocaleString()}</span>
                    </div>

                    {/* 진행률 바 */}
                    <div className="flex flex-col gap-1.5 mt-2">
                      <div className="flex justify-between text-xs font-bold text-gray-500">
                        <span>진행률 {ratio}%</span>
                        <span>{item.current} / {item.target} 명</span>
                      </div>
                      <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full transition-all duration-500 ${isCompleted ? 'bg-blue-600' : 'bg-emerald-500'}`}
                          style={{ width: `${ratio}%` }}
                        ></div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="flex flex-col md:flex-row gap-2 mt-4 border-t border-gray-100 pt-4">
              <Link to="/seller/status" className="flex-1 text-sm font-bold text-gray-600 bg-gray-50 hover:bg-gray-100 py-3 rounded-xl flex items-center justify-center gap-2 transition">
                전체 게시글 보기 <ArrowRight size={16} />
              </Link>
              <Link to="/seller/orders" className="flex-1 text-sm font-bold text-blue-600 bg-blue-50 hover:bg-blue-100 py-3 rounded-xl flex items-center justify-center gap-2 transition">
                주문 / 구매자 명단 확인 <ArrowRight size={16} />
              </Link>
            </div>
          </div>

          {/* 우측 패널: 알림 및 퀵 액션 (1/3) */}
          <div className="lg:col-span-1 bg-white rounded-[28px] p-6 md:p-8 border border-gray-200 shadow-sm flex flex-col justify-between">
            <div className="flex flex-col gap-6">
              <div className="flex items-center gap-2 border-b border-gray-100 pb-4">
                <Bell size={20} className="text-gray-900" />
                <h3 className="text-lg font-extrabold text-gray-950 tracking-tight">최근 알림</h3>
              </div>

              <div className="flex flex-col gap-4">

                {participations.length > 0 ? (
                  participations.slice(0, 3).map((part, index) => {
                    const timeDiffStr = "방금 전"; // 실제로는 part.joinDate 시간 계산 로직 필요
                    return (
                      <div key={part.id || index} className="flex items-start gap-3">
                        <div className="w-2 h-2 rounded-full bg-orange-500 mt-1.5 flex-shrink-0"></div>
                        <div className="flex flex-col gap-0.5">
                          <p className="text-sm font-bold text-gray-800 leading-tight">
                            구매자 '{part.buyerName}'님이 '{part.product?.title || '상품'}'에 탑승했습니다!
                          </p>
                          <span className="text-xs text-gray-400 font-medium"><Clock size={10} className="inline mr-1" />{timeDiffStr}</span>
                        </div>
                      </div>
                    );
                  })
                ) : (
                  <div className="text-sm text-gray-400 text-center py-4 font-bold">새로운 알림이 없습니다.</div>
                )}

              </div>
            </div>

            <div className="mt-8 pt-4 border-t border-gray-100">
              <div className="bg-slate-50 rounded-xl p-4 border border-slate-100 flex flex-col gap-2">
                <span className="text-xs font-bold text-slate-500">SELLER TIP</span>
                <p className="text-xs text-slate-700 font-medium leading-relaxed">
                  물품의 상세한 사진과 책의 상태(필기 흔적 등)를 정확하게 기재하면 거래 체결률이 평균 45% 상승합니다.
                </p>
              </div>
            </div>
          </div>

        </section>
      </main>
    </div>
  );
};

export default SellerDashboardPage;