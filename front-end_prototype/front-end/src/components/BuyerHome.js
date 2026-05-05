import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const BuyerHome = () => {
  const navigate = useNavigate();
  const [isChatOpen, setIsChatOpen] = useState(false);
  const handleLogout = () => navigate('/login');

  return (
    <div className="bg-surface text-on-surface overflow-x-hidden selection:bg-primary selection:text-on-primary font-body">
      {/* TopNavBar */}
      <nav className="fixed top-0 w-full z-50 flex justify-between items-center px-8 h-16 bg-white/40 backdrop-blur-xl border-b border-outline-variant/10 shadow-[0_4px_30px_rgba(0,0,0,0.02)]">
        <div className="flex items-center gap-8">
          <span className="text-2xl font-black italic text-primary drop-shadow-[0_0_8px_rgba(0,106,113,0.1)] font-headline tracking-tight uppercase">대학생 공동주문 마켓</span>
          <div className="hidden md:flex items-center gap-6 font-headline tracking-tight text-sm">
            <a className="text-primary border-b-2 border-primary pb-1" href="#">대시보드</a>
            <a className="text-on-surface-variant hover:text-primary transition-colors" href="#">중고 장터</a>
            <a className="text-on-surface-variant hover:text-primary transition-colors" href="#">주문 내역</a>
            <a className="text-on-surface-variant hover:text-primary transition-colors" href="#">커뮤니티</a>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <div className="relative hidden lg:block">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-sm">search</span>
            <input className="bg-surface-container border-none rounded-full py-1.5 pl-10 pr-4 text-sm w-64 focus:ring-1 focus:ring-primary outline-none" placeholder="희귀 도서 검색..." type="text"/>
          </div>
          <button className="text-on-surface-variant hover:text-primary transition-transform active:scale-90"><span className="material-symbols-outlined">notifications</span></button>
          <button className="text-on-surface-variant hover:text-primary transition-transform active:scale-90"><span className="material-symbols-outlined">account_circle</span></button>
        </div>
      </nav>

      {/* Main Content */}
      <main className="pt-24 pb-32 px-4 md:px-8 md:ml-64 max-w-7xl mx-auto">
        {/* Hero: Featured Drop */}
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-8 mb-20 items-center">
          <div className="lg:col-span-7 relative group">
            <div className="absolute -inset-4 bg-primary/5 blur-3xl rounded-full opacity-50 group-hover:opacity-100 transition-opacity"></div>
            <div className="relative overflow-hidden rounded-3xl bg-surface-container border border-outline-variant/15 aspect-[4/3] flex items-center justify-center">
              <img alt="희귀 초판본" className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuCI51b9758o5sr7b5_icIDr5Yj8x9gmdFERqo1F06lDyARd4eXWvrvFBdgRBwXqYFB1mkXcanvTVKMPthN8M4pTerDXGHz73hG5DfGZ4tThUJYUdxWBZA_wylVGqHZV7OkjZ1jxHT1ftyd5tnfqdcqSETMJ0sMgZFhB2PYXqvAKxrk4VPaeyqPLwFG5v5rRF5FAjWNwYnFKabQ8UxWrg08mbVYh5fpEIJ8gIuvXl9-Ls0yi6W7ehzz8UPZljKmRvyuQXYX99VgyvfCu"/>
              <div className="absolute bottom-6 left-6 right-6 p-6 backdrop-blur-2xl bg-surface-container-highest/60 rounded-2xl border border-white/5 flex justify-between items-end">
                <div>
                  <span className="label-md uppercase tracking-[0.2em] text-primary mb-2 block font-headline font-bold">리미티드 에디션</span>
                  <h1 className="text-4xl md:text-5xl font-black headline-font text-white leading-none tracking-tighter">고전의 재해석 X1</h1>
                </div>
                <div className="text-right">
                  <span className="text-xs text-on-surface-variant block mb-1">현재 입찰가</span>
                  <span className="text-2xl font-bold headline-font text-primary uppercase">₩158,000</span>
                </div>
              </div>
            </div>
          </div>
          <div className="lg:col-span-5 space-y-8">
            <div>
              <div className="flex items-center gap-3 mb-4">
                <span className="bg-error/10 text-error px-3 py-1 rounded-full text-xs font-bold uppercase tracking-widest border border-error/20">모집 중</span>
                <div className="flex items-center gap-2 text-on-surface-variant text-sm">
                  <span className="material-symbols-outlined text-sm">schedule</span>
                  <span>종료까지: 14시간 22분 05초</span>
                </div>
              </div>
              <p className="text-on-surface-variant leading-relaxed text-sm">최상급 보존 상태의 1920년대 초판본 하드커버입니다. 공동 구매에 참여하여 소장 가치가 높은 이 도서를 가장 합리적인 가격에 확보하세요. 현재 5슬롯만 남았습니다.</p>
            </div>
            <div className="space-y-3">
              <div className="flex justify-between text-sm font-headline"><span className="text-white">배치 달성률</span><span className="text-primary font-bold">85%</span></div>
              <div className="h-4 bg-surface-container-highest rounded-full overflow-hidden p-0.5">
                <div className="h-full bg-gradient-to-r from-primary to-secondary rounded-full" style={{width: '85%'}}></div>
              </div>
              <div className="flex justify-between text-[10px] uppercase tracking-widest text-on-surface-variant font-bold"><span>42명 참여</span><span>목표: 50명</span></div>
            </div>
            <div className="flex flex-col gap-4">
              <button 
                onClick={() => navigate('/login')}
                className="bg-primary text-on-primary font-bold py-4 rounded-xl shadow-[0_0_15px_rgba(143,245,255,0.3)] hover:scale-[1.02] active:scale-95 transition-all flex items-center justify-center gap-2 group uppercase"
              >
                공동 구매 참여하기 <span className="material-symbols-outlined group-hover:translate-x-1 transition-transform">auto_stories</span>
              </button>
              <button 
                onClick={() => navigate('/login')}
                className="bg-surface-container-highest/50 border border-outline-variant/20 text-white font-bold py-4 rounded-xl hover:bg-surface-container-highest transition-colors uppercase"
              >
                상세 감정서 보기
              </button>
            </div>
          </div>
        </section>

        {/* Market Explorer */}
        <section className="mb-20">
          <div className="flex justify-between items-end mb-10">
            <div><h2 className="text-3xl font-black headline-font mb-2">중고 장터 탐색</h2><p className="text-on-surface-variant font-medium">네트워크에서 진행 중인 프리미엄 도서 모집 현황입니다.</p></div>
            <button className="text-primary text-sm font-bold flex items-center gap-1 group uppercase tracking-widest">전체 보기 <span className="material-symbols-outlined text-base group-hover:translate-x-1 transition-transform">arrow_forward</span></button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {/* Card 1 */}
            <div className="bg-surface-container-low rounded-3xl p-4 group hover:bg-surface-container transition-all duration-300 hover:scale-[1.02] border border-transparent hover:border-primary/10 shadow-sm hover:shadow-md">
              <div className="relative aspect-square rounded-2xl overflow-hidden mb-5">
                <img alt="빈티지 아트북" className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBeVFhYn3qic9kzmfIw9Fd1jMz5Ws6xv-3RpUPFBLtVe3b1Z1Bs4B3NBq8_H2cWC9DQFJyw9AGpfkWBvUVcUgQttaEp6WVs3WoYPzKnMUhVgZHgN6on-oLljA3--z74_08UD3Fkk6vcuqzFVVeT_nwclNtgrX9L6FrcUdiWvTV63SL3pWDyd4eSWevnlAfnyhqZY3viiDOpR02lWe60Ep2F73ofnFeV3Hd-KPcXgWz_vBQuiUeQw-tjNo9_3kCDqcHNQFEugLuo4_6T"/>
                <div className="absolute top-4 left-4 bg-white/60 backdrop-blur-md px-3 py-1 rounded-full text-[10px] font-bold text-primary border border-outline-variant/10 uppercase tracking-widest">인기 품목</div>
              </div>
              <div className="px-2">
                <h3 className="text-lg font-bold headline-font mb-1 uppercase tracking-tight text-on-surface">바우하우스 디자인 총서</h3>
                <p className="text-sm text-on-surface-variant mb-4 font-bold uppercase">4차 배치 모집 중</p>
                <div className="flex justify-between items-center mb-2"><span className="text-xl font-black text-on-surface headline-font uppercase">₩89,000</span><span className="text-xs text-primary font-bold uppercase tracking-widest">12/20 참여</span></div>
                <div className="h-1.5 bg-surface-container rounded-full"><div className="h-full bg-primary rounded-full shadow-[0_0_8px_rgba(0,106,113,0.1)]" style={{width: '60%'}}></div></div>
              </div>
            </div>
            {/* Card 2 */}
            <div className="bg-surface-container-low rounded-3xl p-4 group hover:bg-surface-container transition-all duration-300 hover:scale-[1.02] border border-transparent hover:border-primary/10 shadow-sm hover:shadow-md">
              <div className="relative aspect-square rounded-2xl overflow-hidden mb-5">
                <img alt="기술 서적" className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuCbKDKvhi3OzX-bdk6OFO61YkTBh-dbNsQC74C78w5SR8ZoqTFs1_f9y3mffgeo_jqSY7t8sleAk0coz8_0LCegXcSkRYt33vrY16ohGCNq9DJ5nYOGxCou7WpfxMGkenrR7xiZMU0ghWrokIUSceDtY2xs8JsFrObGs0dx14ToQymFEi81JSEiKPXaxe5XT9Q_A8NPXeNCLZAsB3ctRh-pAub38QVMiYqNt_NK8v6pl1ZEEdQ8DHUMJMHTDHp4vx9Lgq7dj6G_FhgQ"/>
              </div>
              <div className="px-2">
                <h3 className="text-lg font-bold headline-font mb-1 uppercase tracking-tight text-on-surface">컴퓨팅의 역사 (절판본)</h3>
                <p className="text-sm text-on-surface-variant mb-4 font-bold uppercase tracking-widest">1차 배치 모집 중</p>
                <div className="flex justify-between items-center mb-2"><span className="text-xl font-black text-on-surface headline-font uppercase">₩125,000</span><span className="text-xs text-secondary font-bold uppercase tracking-widest">45/50 참여</span></div>
                <div className="h-1.5 bg-surface-container rounded-full"><div className="h-full bg-secondary rounded-full shadow-[0_0_8px_rgba(131,0,180,0.1)]" style={{width: '90%'}}></div></div>
              </div>
            </div>
            {/* Card 3 */}
            <div className="bg-surface-container-low rounded-3xl p-4 group hover:bg-surface-container transition-all duration-300 hover:scale-[1.02] border border-transparent hover:border-primary/10 shadow-sm hover:shadow-md">
              <div className="relative aspect-square rounded-2xl overflow-hidden mb-5">
                <img alt="가죽 장정 도서" className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuDMiGjD4to27tgVeB68lo2QmvLbmp1OBoyVKK0i3HfnDo-V8GurbjXZs7fldC54ln--XhAcqC0mqgsAX2V7jkXm-kFuRF_HNk2cKmhJzpUwr1OTypDLHhc2jXfgvg1oUsf4jAa08NdtFZEMK9OmfwNcZuTEym_eQzkrHDM-bZDo9Yb_OQjAN8Qw9dmGnfi5qGQZbx9uHM0F7DtwOg5by9qEoAdDFyCF2Zhjw17TPgf9lVDQZYldcsX9gkDgzJNGCIYG1i9V8I0FcTAB"/>
              </div>
              <div className="px-2">
                <h3 className="text-lg font-bold headline-font mb-1 uppercase tracking-tight text-on-surface">가죽 양장 세계 문학 전집</h3>
                <p className="text-sm text-on-surface-variant mb-4 font-bold uppercase tracking-widest">예약 대기 열림</p>
                <div className="flex justify-between items-center mb-2"><span className="text-xl font-black text-on-surface headline-font uppercase">₩210,000</span><span className="text-xs text-tertiary font-bold uppercase tracking-widest">5/100 참여</span></div>
                <div className="h-1.5 bg-surface-container rounded-full"><div className="h-full bg-tertiary rounded-full shadow-[0_0_8px_rgba(0,97,164,0.1)]" style={{width: '5%'}}></div></div>
              </div>
            </div>
          </div>
        </section>

        {/* Participation Tracker */}
        <section className="bg-surface-container rounded-[2.5rem] p-8 md:p-12 border border-outline-variant/10 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-64 h-64 bg-primary/5 blur-3xl rounded-full -mr-32 -mt-32"></div>
          <div className="relative z-10">
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-12">
              <div><h2 className="text-3xl font-black headline-font mb-2">나의 참여 정보</h2><p className="text-on-surface-variant font-medium">현재 참여 중인 공동 구매 현황을 확인하세요.</p></div>
              <div className="flex gap-2 p-1 bg-surface-container-low rounded-xl">
                <button className="bg-primary text-on-primary px-4 py-2 rounded-lg text-xs font-bold uppercase tracking-wider transition-all">전체</button>
                <button className="text-on-surface-variant px-4 py-2 rounded-lg text-xs font-bold uppercase tracking-wider hover:text-white transition-colors">진행 중</button>
                <button className="text-on-surface-variant px-4 py-2 rounded-lg text-xs font-bold uppercase tracking-wider hover:text-white transition-colors">완료</button>
              </div>
            </div>
            <div className="space-y-4">
              {/* Order 1 */}
              <div className="flex flex-col md:flex-row md:items-center gap-6 p-6 rounded-2xl bg-surface-container-lowest border border-white/5 group hover:border-primary/20 transition-all">
                <div className="w-16 h-16 rounded-xl overflow-hidden flex-shrink-0 bg-surface-container-highest">
                  <img alt="아트북 썸네일" className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuDwR7f5PLTFZP43cZTwg18G-T-VlsrIvIWDGfCoe-hqNxVUhkEETG4OsakRL3d7CtSblvTyFYWKynzbJc1kv5csZ1wM2txekP2bOhemCxMidnsxMr7oktVkns2mYKkXGO-0pwruYWtqWhJ7F2IxKBMKdoz7CkRUdpRLCiJ-mVLX9Rjdi798yfuRv_iZoiOYOLGtKiJD41UOrc3jGZMgYy4hucxppF-bXNL36iJq8C5mTGDg81_OZpQrFS1tQmKgVaOz6Eok-bD2OOtR"/>
                </div>
                <div className="flex-grow"><h4 className="font-bold headline-font text-white uppercase tracking-tighter">바우하우스 디자인 총서</h4><p className="text-xs text-on-surface-variant font-bold uppercase tracking-widest mt-1">ID: #BK-98211</p></div>
                <div className="flex flex-col gap-1"><span className="text-[10px] text-on-surface-variant uppercase tracking-[0.2em] font-bold">상태</span><div className="flex items-center gap-2"><span className="w-2 h-2 rounded-full bg-error animate-pulse shadow-[0_0_8px_#ff716c]"></span><span className="text-sm font-bold text-white uppercase tracking-widest">모집 중</span></div></div>
                <div className="flex flex-col gap-1 min-w-[120px]"><span className="text-[10px] text-on-surface-variant uppercase tracking-[0.2em] font-bold">진행도</span><span className="text-sm font-bold text-primary">12 / 20 권</span></div>
                <button className="px-6 py-2.5 rounded-xl bg-surface-container-highest text-white text-xs font-bold uppercase tracking-widest hover:bg-primary hover:text-on-primary transition-all">상세 보기</button>
              </div>
              {/* Order 2 */}
              <div className="flex flex-col md:flex-row md:items-center gap-6 p-6 rounded-2xl bg-surface-container-lowest border border-white/5 group hover:border-primary/20 transition-all opacity-80">
                <div className="w-16 h-16 rounded-xl overflow-hidden flex-shrink-0 grayscale">
                  <img alt="기술 서적 썸네일" className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBQtgSw4DqrAUHXUEh2gnpXqY5-uSB5Qxeppwyj07y7qXSy62vCbIiLoKDRqRy3qGQ-VMJdX5Fz15hZu6-tLCmU8pPfNiKPbm_tqhQgRuIUw2ABtVO3Etxpv1hCiF2LWj5J3RzEkdvhrKhVqTG8egUHfi-JWG5n9rE6PctJG0DQh8xCiPrVMK08w9Nqz8hkR0ISUhcAVGmURBGXmIemiAS_Wg3BRg0I0tcYwAz80A0zlBv9_t7oDBRmuoaO4v8Z3qTP6xbFkTXu5rpS"/>
                </div>
                <div className="flex-grow"><h4 className="font-bold headline-font text-white uppercase tracking-tighter">레트로 컴퓨팅 가이드</h4><p className="text-xs text-on-surface-variant font-bold uppercase tracking-widest mt-1">ID: #BK-87422</p></div>
                <div className="flex flex-col gap-1"><span className="text-[10px] text-on-surface-variant uppercase tracking-[0.2em] font-bold">상태</span><div className="flex items-center gap-2"><span className="w-2 h-2 rounded-full bg-tertiary shadow-[0_0_8px_#65afff]"></span><span className="text-sm font-bold text-white uppercase tracking-widest">주문 처리 중</span></div></div>
                <div className="flex flex-col gap-1 min-w-[120px]"><span className="text-[10px] text-on-surface-variant uppercase tracking-[0.2em] font-bold">진행도</span><span className="text-sm font-bold text-tertiary">배치 최종 조율</span></div>
                <button className="px-6 py-2.5 rounded-xl bg-surface-container-highest text-white text-xs font-bold uppercase tracking-widest hover:bg-primary hover:text-on-primary transition-all">배송 추적</button>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* SideNavBar (Desktop Context) */}
      <aside className="hidden md:flex fixed left-0 top-16 h-[calc(100vh-64px)] w-64 bg-white border-r border-outline-variant/10 flex-col shadow-[10px_0_30px_rgba(0,0,0,0.02)] z-40 transition-all duration-300">
        <div className="p-6 flex flex-col gap-4">
          <div className="bg-surface-container-low rounded-2xl p-4 border border-outline-variant/10">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center border border-primary/30"><span className="material-symbols-outlined text-primary">person</span></div>
              <div><p className="text-xs font-bold headline-font text-on-surface uppercase">김 대학생</p><p className="text-[10px] text-on-surface-variant uppercase tracking-widest font-bold">VIP COLLECTOR</p></div>
            </div>
          </div>
        </div>
        <div className="flex-grow flex flex-col font-headline text-sm uppercase tracking-widest">
          <a className="bg-gradient-to-r from-primary/10 to-transparent text-primary border-l-4 border-primary px-6 py-4 flex items-center gap-3" href="#"><span className="material-symbols-outlined">dashboard</span> 개요</a>
          <a className="text-on-surface-variant px-6 py-4 flex items-center gap-3 hover:translate-x-2 transition-transform hover:bg-surface-container" href="#"><span className="material-symbols-outlined">shopping_bag</span> 중고 장터</a>
          <a className="text-on-surface-variant px-6 py-4 flex items-center gap-3 hover:translate-x-2 transition-transform hover:bg-surface-container" href="#"><span className="material-symbols-outlined">inventory_2</span> 주문 내역</a>
          <a className="text-on-surface-variant px-6 py-4 flex items-center gap-3 hover:translate-x-2 transition-transform hover:bg-surface-container" href="#"><span className="material-symbols-outlined">group</span> 커뮤니티</a>
        </div>
        <div className="p-6 border-t border-outline-variant/10 space-y-2">
          <button onClick={handleLogout} className="text-error/70 px-2 py-2 flex items-center gap-3 text-[10px] hover:text-error transition-colors font-black uppercase tracking-[0.2em]"><span className="material-symbols-outlined text-sm">logout</span> 로그아웃</button>
        </div>
      </aside>

      {/* Floating Action Button (Chat) */}
      <div className="fixed bottom-24 right-8 z-[60] group md:bottom-8">
        {isChatOpen && (
          <div className="absolute bottom-full right-0 mb-4 w-64 transition-all duration-300">
            <div className="bg-surface-container-highest/90 backdrop-blur-xl p-4 rounded-2xl border border-primary/20 shadow-[0_10px_40px_rgba(0,0,0,0.5)]">
              <div className="flex items-center gap-3 mb-3 border-b border-white/5 pb-2">
                <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center border border-primary/30">
                  <span className="material-symbols-outlined text-primary text-sm" style={{fontVariationSettings: "'FILL' 1"}}>person</span>
                </div>
                <div>
                  <p className="text-xs font-bold text-white font-headline">도서 큐레이터</p>
                  <p className="text-[8px] text-primary uppercase tracking-widest font-bold">상담 가능</p>
                </div>
              </div>
              <p className="text-xs text-on-surface-variant leading-relaxed mb-3">안녕하세요! 이번 초판본 배치의 담당 큐레이터입니다. 도서의 상태나 가치에 대해 궁금한 점이 있으신가요?</p>
              <div className="flex gap-2">
                <input className="bg-surface-container text-[10px] border-none rounded-lg flex-grow focus:ring-1 focus:ring-primary py-2 px-3 outline-none text-white text-sm" placeholder="메시지를 입력하세요..." type="text"/>
                <button className="p-2 bg-primary text-on-primary rounded-lg transition-transform active:scale-90"><span className="material-symbols-outlined text-sm">send</span></button>
              </div>
            </div>
          </div>
        )}
        <button onClick={() => setIsChatOpen(!isChatOpen)} className="w-14 h-14 rounded-full bg-primary text-on-primary flex items-center justify-center shadow-[0_0_25px_rgba(143,245,255,0.4)] hover:scale-110 active:scale-95 transition-all">
          <span className="material-symbols-outlined text-2xl" style={{fontVariationSettings: "'FILL' 1"}}>{isChatOpen ? 'close' : 'chat'}</span>
        </button>
      </div>

      {/* BottomNavBar (Mobile Only) */}
      <nav className="fixed bottom-0 w-full z-50 flex justify-around items-center px-6 pb-6 pt-3 md:hidden bg-[#0e0e0e]/80 backdrop-blur-2xl rounded-t-[2rem] border-t border-[#8ff5ff]/10 shadow-[0_-10px_40px_rgba(143,245,255,0.1)]">
        <button className="flex flex-col items-center justify-center bg-[#8ff5ff] text-[#0e0e0e] rounded-2xl p-2 shadow-[0_0_20px_rgba(143,245,255,0.6)] scale-110 transition-transform duration-200">
          <span className="material-symbols-outlined">explore</span>
          <span className="font-headline text-[10px] font-bold uppercase mt-1">탐색</span>
        </button>
        <button className="flex flex-col items-center justify-center text-[#adaaaa] p-2 hover:text-[#8ff5ff] transition-transform">
          <span className="material-symbols-outlined">add_shopping_cart</span>
          <span className="font-headline text-[10px] font-bold uppercase mt-1">참여</span>
        </button>
        <button className="flex flex-col items-center justify-center text-[#adaaaa] p-2 hover:text-[#8ff5ff] transition-transform">
          <span className="material-symbols-outlined">chat_bubble</span>
          <span className="font-headline text-[10px] font-bold uppercase mt-1">메시지</span>
        </button>
        <button className="flex flex-col items-center justify-center text-[#adaaaa] p-2 hover:text-[#8ff5ff] transition-transform">
          <span className="material-symbols-outlined">query_stats</span>
          <span className="font-headline text-[10px] font-bold uppercase mt-1">추적</span>
        </button>
      </nav>

      {/* Background Decor */}
      <div className="fixed top-0 left-0 w-full h-full -z-10 pointer-events-none opacity-30">
        <div className="absolute top-[10%] left-[5%] w-[40vw] h-[40vw] bg-primary/10 blur-[150px] rounded-full"></div>
        <div className="absolute bottom-[10%] right-[5%] w-[30vw] h-[30vw] bg-secondary/10 blur-[150px] rounded-full"></div>
      </div>
    </div>
  );
};

export default BuyerHome;
