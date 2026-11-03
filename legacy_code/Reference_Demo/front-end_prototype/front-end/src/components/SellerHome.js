import React from 'react';
import { useNavigate } from 'react-router-dom';

const SellerHome = () => {
  const navigate = useNavigate();
  const handleLogout = () => navigate('/login');

  return (
    <div className="bg-surface text-on-surface font-body min-h-screen selection:bg-primary selection:text-on-primary">
      {/* TopAppBar */}
      <nav className="fixed top-0 w-full z-50 flex justify-between items-center px-8 h-16 bg-white/40 backdrop-blur-xl text-primary font-headline tracking-tight border-b border-outline-variant/10 shadow-[0_4px_30px_rgba(0,0,0,0.05)]">
        <div className="text-2xl font-black italic text-primary drop-shadow-[0_0_8px_rgba(0,106,113,0.2)] uppercase tracking-tighter">대학생 공동주문 마켓</div>
        <div className="hidden md:flex space-x-8 items-center">
          <a className="text-on-surface-variant hover:text-primary transition-colors text-sm font-bold uppercase tracking-widest" href="#">대시보드</a>
          <a className="text-on-surface-variant hover:text-primary transition-colors text-sm font-bold uppercase tracking-widest" href="#">마켓</a>
          <a className="text-on-surface-variant hover:text-primary transition-colors text-sm font-bold uppercase tracking-widest" href="#">주문관리</a>
          <a className="text-on-surface-variant hover:text-primary transition-colors text-sm font-bold uppercase tracking-widest" href="#">커뮤니티</a>
        </div>
        <div className="flex items-center space-x-4">
          <button className="p-2 hover:bg-primary/10 rounded-full transition-all active:scale-90"><span className="material-symbols-outlined">notifications</span></button>
          <button className="p-2 hover:bg-primary/10 rounded-full active:scale-90"><span className="material-symbols-outlined">account_circle</span></button>
        </div>
      </nav>

      {/* SideNavBar */}
      <aside className="fixed left-0 top-16 h-[calc(100vh-64px)] w-64 bg-white border-r border-outline-variant/10 flex flex-col hidden lg:flex shadow-[10px_0_30px_rgba(0,0,0,0.05)] z-40">
        <div className="p-6">
          <div className="flex items-center space-x-3 mb-8">
            <div className="w-10 h-10 rounded-full bg-surface-container-highest overflow-hidden border border-primary/20">
              <img alt="Seller" src="https://lh3.googleusercontent.com/aida-public/AB6AXuCg92TbiVoJCcimNj9L0qxSxN-VuO2WzakH1K8q1Lx1jFpJr3-sHu3iBJOm5yU_-PNJ8diwcztXcezITTijbSkUSzFxYTsH5hQzx2GYRcNnldEiYY7Gvxj4o_D51MvNe0ZUfgdV2QRuKVAMxaWeLYIU9iIA8tW-bY-sv7uOWLwV3k1RM3hVF2o4EkQc4wTP6C3wN8yEw0PDYZx0kg4yjdeqsv-ZGutdsRsafHvxUjk9EwAQ2qKYedG4w4365eU7InDE3MzjQVo02HxH" />
            </div>
            <div>
              <h3 className="text-xs font-bold uppercase tracking-widest font-headline text-on-surface tracking-tighter">판매자 워크스페이스</h3>
              <p className="text-[10px] text-on-surface-variant font-bold uppercase">권한: 전문 큐레이터</p>
            </div>
          </div>
          <nav className="space-y-2 font-headline uppercase text-sm tracking-widest font-bold">
            <a className="flex items-center space-x-4 text-on-surface-variant px-4 py-3 hover:translate-x-2 transition-transform" href="#"><span className="material-symbols-outlined">dashboard</span><span>개요</span></a>
            <a className="flex items-center space-x-4 bg-gradient-to-r from-primary/20 to-transparent text-primary border-l-4 border-primary px-4 py-3" href="#"><span className="material-symbols-outlined">inventory_2</span><span>인벤토리</span></a>
            <a className="flex items-center space-x-4 text-on-surface-variant px-4 py-3 hover:translate-x-2 transition-transform" href="#"><span className="material-symbols-outlined">local_shipping</span><span>물류 현황</span></a>
            <a className="flex items-center space-x-4 text-on-surface-variant px-4 py-3 hover:translate-x-2 transition-transform" href="#"><span className="material-symbols-outlined">insights</span><span>분석 데이터</span></a>
          </nav>
        </div>
        <div className="mt-auto p-6 space-y-4">
          <button 
            onClick={() => navigate('/login')}
            className="w-full py-3 px-4 bg-primary text-on-primary font-bold rounded-lg shadow-[0_0_20px_rgba(0,106,113,0.1)] hover:brightness-110 transition-all text-xs uppercase tracking-widest"
          >
            리포트 생성
          </button>
          <div className="flex flex-col space-y-2 border-t border-outline-variant/20 pt-4">
            <a className="flex items-center space-x-3 text-on-surface-variant text-xs hover:text-primary transition-colors font-bold uppercase tracking-widest" href="#"><span className="material-symbols-outlined text-sm">settings</span><span>설정</span></a>
            <button onClick={handleLogout} className="flex items-center space-x-3 text-error/70 text-xs hover:text-error transition-colors uppercase font-bold tracking-widest"><span className="material-symbols-outlined text-sm">logout</span><span>로그아웃</span></button>
          </div>
        </div>
      </aside>

      {/* Main Content Canvas */}
      <main className="lg:ml-64 pt-24 pb-32 px-6 md:px-12 max-w-7xl mx-auto">
        <header className="mb-12 flex flex-col md:flex-row md:items-end justify-between gap-6 uppercase">
          <div><h1 className="font-headline text-5xl md:text-6xl font-black tracking-tighter mb-2 text-on-surface">판매자<br/><span className="text-primary drop-shadow-[0_0_15px_rgba(0,106,113,0.2)]">대시보드</span></h1><p className="text-on-surface-variant max-w-md font-medium text-sm">중고 도서 매물을 등록하고 공동구매 참여자들과 실시간으로 소통하세요.</p></div>
          <div className="flex space-x-4">
            <div className="bg-surface-container-high p-4 rounded-xl border border-primary/10 flex flex-col items-center min-w-[120px]"><span className="text-primary font-headline text-2xl font-bold">24</span><span className="text-[10px] uppercase tracking-widest text-on-surface-variant font-bold">판매중인 도서</span></div>
            <div className="bg-surface-container-high p-4 rounded-xl border border-secondary/10 flex flex-col items-center min-w-[120px]"><span className="text-secondary font-headline text-2xl font-bold">1.2k</span><span className="text-[10px] uppercase tracking-widest text-on-surface-variant font-bold">구매 대기자</span></div>
          </div>
        </header>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* 도서 등록 폼 */}
          <section className="lg:col-span-4 bg-surface-container rounded-3xl p-8 border border-outline-variant/10 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 blur-3xl rounded-full"></div>
            <h2 className="font-headline text-xl font-bold mb-8 flex items-center gap-2 text-on-surface"><span className="material-symbols-outlined text-primary">book</span>도서 등록</h2>
            <form className="space-y-6">
              <div className="space-y-1"><label className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant px-1">도서명</label><input className="w-full bg-surface-container-highest border-none rounded-xl px-4 py-4 text-on-surface outline-none text-sm placeholder:text-on-surface/30" placeholder="예: 어둠 속의 자객 (초판본)" type="text"/></div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1"><label className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant px-1">판매 가격 (₩)</label><input className="w-full bg-surface-container-highest border-none rounded-xl px-4 py-4 text-on-surface outline-none text-sm placeholder:text-on-surface/30" placeholder="45,000" type="number"/></div>
                <div className="space-y-1"><label className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant px-1">목표 수량</label><input className="w-full bg-surface-container-highest border-none rounded-xl px-4 py-4 text-on-surface outline-none text-sm placeholder:text-on-surface/30" placeholder="1" type="number"/></div>
              </div>
              <div className="space-y-1"><label className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant px-1">카테고리</label>
                <select className="w-full bg-surface-container-highest border-none rounded-xl px-4 py-4 text-on-surface outline-none text-sm appearance-none">
                  <option>SF/판타지</option><option>기술/과학</option><option>인문/철학</option><option>희귀본/수집용</option>
                </select>
              </div>
              <button 
                onClick={() => navigate('/login')}
                className="w-full mt-4 py-4 bg-primary text-on-primary font-black rounded-xl hover:shadow-[0_0_30px_rgba(0,106,113,0.2)] hover:scale-[1.02] active:scale-[0.98] transition-all uppercase tracking-widest text-xs"
              >
                도서 등록 완료
              </button>
            </form>
          </section>

          {/* 에디터 섹션 */}
          <section className="lg:col-span-8 bg-surface-container rounded-3xl p-8 border border-outline-variant/10">
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4 uppercase font-bold">
              <h2 className="font-headline text-xl font-bold flex items-center gap-2 text-on-surface"><span className="material-symbols-outlined text-secondary">edit_note</span>상세 정보 에디터</h2>
              <div className="flex bg-surface-container-highest p-1 rounded-lg">
                <button className="px-4 py-1.5 text-[10px] rounded-md bg-surface text-primary">내용</button>
                <button className="px-4 py-1.5 text-[10px] rounded-md text-on-surface-variant hover:text-on-surface">설명</button>
                <button className="px-4 py-1.5 text-[10px] rounded-md text-on-surface-variant hover:text-on-surface">미리보기</button>
              </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div className="space-y-6">
                <div className="space-y-2"><label className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">도서 설명 및 상태 정보</label><textarea className="w-full bg-surface-container-highest border-none rounded-2xl p-4 text-on-surface h-48 outline-none resize-none text-xs placeholder:text-on-surface/30" placeholder="도서의 희귀성, 보관 상태를 기록하세요..."></textarea></div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1"><label className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">경매 시작일</label><input className="w-full bg-surface-container-highest border-none rounded-xl px-4 py-3 text-on-surface text-xs outline-none" type="date"/></div>
                  <div className="space-y-1"><label className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">경매 종료일</label><input className="w-full bg-surface-container-highest border-none rounded-xl px-4 py-3 text-on-surface text-xs outline-none" type="date"/></div>
                </div>
              </div>
              <div className="space-y-6">
                <label className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant block">도서 표지 및 상태 사진</label>
                <div className="aspect-video w-full rounded-2xl border-2 border-dashed border-outline-variant flex flex-col items-center justify-center gap-2 bg-surface-container-low hover:bg-surface-container-high transition-colors cursor-pointer group">
                  <span className="material-symbols-outlined text-4xl text-outline-variant group-hover:text-primary transition-colors">auto_stories</span>
                  <p className="text-xs text-on-surface-variant font-bold">고해상도 사진 업로드</p>
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <div className="aspect-square bg-surface-container-highest rounded-xl overflow-hidden border border-outline-variant/10 group relative">
                    <img alt="preview" className="w-full h-full object-cover opacity-60 group-hover:opacity-100 transition-opacity" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBjLWX0vDxS-xTDQMSRmWbGN1YF4Jgq9GZmzDwBqelTXt-dSB7kBQo-ik2oo0SRD77XX1g54JfqdK1iGTF48kkJN9gk7O8QhVwGzTeS8bl9Eh_WMxmfx3-DlaH060vLMpyDuhtFe642S0jcqXvg4QIgMiEG2TDtrscuaZbiPz8J3RUD-eCr0ZHKbWSyiap4INfx4JjDSIiIpbnpllxBeVRH5XXtlGjcxiN_WWZg-_GZ66JXaXatr_tZlkDcv-UKqhwndRIeE9GGZ8ku"/>
                  </div>
                  <div className="aspect-square bg-surface-container-highest rounded-xl border border-dashed border-outline-variant flex items-center justify-center hover:bg-surface-container-high transition-colors cursor-pointer"><span className="material-symbols-outlined text-on-surface-variant">add</span></div>
                </div>
              </div>
            </div>
          </section>

          {/* 판매 달성도 섹션 */}
          <section className="lg:col-span-5 bg-surface-container rounded-3xl p-8 border border-outline-variant/10 flex flex-col">
            <h2 className="font-headline text-xl font-bold mb-6 flex items-center gap-2 text-on-surface"><span className="material-symbols-outlined text-primary">monitoring</span>판매 달성도</h2>
            <div className="flex-1 space-y-6">
              <div className="flex justify-between items-end">
                <div><p className="text-4xl font-headline font-black text-on-surface">12<span className="text-primary text-xl">/15</span></p><p className="text-[10px] uppercase font-bold text-on-surface-variant tracking-widest">누적 입찰 수</p></div>
                <div className="text-right"><p className="text-secondary font-headline font-bold text-lg">80%</p><p className="text-[10px] uppercase font-bold text-on-surface-variant tracking-widest">목표 달성</p></div>
              </div>
              <div className="h-4 w-full bg-surface-container-highest rounded-full overflow-hidden"><div className="h-full bg-gradient-to-r from-primary to-secondary rounded-full shadow-[0_0_15px_rgba(0,106,113,0.2)]" style={{width: '80%'}}></div></div>
              <div className="space-y-3 pt-4">
                <div className="flex items-center gap-3 p-3 bg-surface-container-low rounded-xl border border-primary/10"><span className="material-symbols-outlined text-primary text-sm" style={{fontVariationSettings: "'FILL' 1"}}>check_circle</span><span className="text-[10px] font-bold uppercase text-on-surface tracking-wider">에스크로 시스템 활성화</span></div>
                <div className="flex items-center gap-3 p-3 bg-surface-container-low rounded-xl border border-primary/10"><span className="material-symbols-outlined text-primary text-sm" style={{fontVariationSettings: "'FILL' 1"}}>check_circle</span><span className="text-[10px] font-bold uppercase text-on-surface tracking-wider">안심 택배 서비스 연동</span></div>
                <div className="flex items-center gap-3 p-3 bg-surface-container-low rounded-xl border border-outline-variant/30 opacity-50"><span className="material-symbols-outlined text-on-surface-variant text-sm">pending</span><span className="text-[10px] font-bold uppercase text-on-surface-variant tracking-wider">최종 검수 확인 대기</span></div>
              </div>
            </div>
            <div className="mt-8 pt-6 border-t border-outline-variant/10 uppercase">
              <button className="w-full py-4 rounded-xl border-2 border-primary/20 text-primary font-black opacity-50 cursor-not-allowed flex items-center justify-center gap-2 text-xs tracking-widest"><span className="material-symbols-outlined">lock</span>판매 확정 및 발송 승인</button>
            </div>
          </section>

          {/* 구매자 문의 채팅 위젯 */}
          <section className="lg:col-span-7 bg-surface-container rounded-3xl border border-outline-variant/10 overflow-hidden flex flex-col min-h-[450px]">
            <div className="p-6 border-b border-outline-variant/10 bg-surface-container-high flex justify-between items-center"><h2 className="font-headline text-xl font-bold text-on-surface flex items-center gap-2">구매자 문의 <span className="w-2 h-2 bg-primary rounded-full animate-pulse"></span></h2><button className="text-[10px] font-bold text-primary uppercase tracking-widest">모두 읽음</button></div>
            <div className="flex-1 flex overflow-hidden">
              <div className="w-[35%] min-w-[140px] border-r border-outline-variant/10 overflow-y-auto bg-surface-container-low">
                <div className="p-4 bg-primary/10 border-l-4 border-primary"><div className="flex justify-between items-start mb-1 text-on-surface text-[10px] font-bold"><span>김민준</span><span>2분 전</span></div><p className="text-[10px] text-on-surface-variant line-clamp-2">책 안쪽에 형광펜 낙서가 심한가요?</p></div>
                <div className="p-4 border-b border-outline-variant/10 hover:bg-surface-container-high cursor-pointer transition-colors"><div className="flex justify-between items-start mb-1 text-on-surface text-[10px] font-bold"><span>이지혜</span><span>14분 전</span></div><p className="text-[10px] text-on-surface-variant line-clamp-1">배송지 변경 가능한가요?</p></div>
              </div>
              <div className="flex-1 flex flex-col relative overflow-hidden">
                <div className="flex-1 p-6 space-y-4 overflow-y-auto">
                  <div className="flex gap-3">
                    <div className="w-8 h-8 rounded-lg bg-surface-container-highest flex-shrink-0 overflow-hidden"><img alt="avatar" className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuD1Y1eM663MlvjtaUHz6cbz9FdA5py0BId8Qx8A_i3wJwUoCosYBpyr_FyZ1niGJOEF300Lqi_d04r4ie19PBTIrbO4zOpDvp1xnnUqUkw0jbprZYMw1wbeR1Day4mTs1aala0wfPie1k5bs7liLO10Wo0yz4r6r3Ka7JodpCaLhmEqDkJLzse5hzPw2_oap4i34nPvY55yPKO_LOnXpsvnm0lNHv6sc8mBx9nGpFvA0LJqQVNZP3qtLxyoate0fhpCauZPb_V1znWj"/></div>
                    <div className="bg-surface-container-highest p-3 rounded-2xl rounded-tl-none max-w-[80%] text-xs text-on-surface leading-relaxed">안녕하세요! 도서 책등 부분에 갈라짐이 있는지 알고 싶습니다.</div>
                  </div>
                  <div className="flex gap-3 flex-row-reverse">
                    <div className="w-8 h-8 rounded-lg bg-primary/20 flex-shrink-0 flex items-center justify-center border border-primary/30 text-primary"><span className="material-symbols-outlined text-sm">auto_stories</span></div>
                    <div className="bg-primary/10 p-3 rounded-2xl rounded-tr-none max-w-[80%] border border-primary/10 text-xs text-on-surface leading-relaxed">반갑습니다. 해당 도서는 암소 보관되어 상태가 매우 좋습니다. 상세 컷을 추가했으니 확인 부탁드려요!</div>
                  </div>
                </div>
                <div className="p-4 bg-surface-container-high border-t border-outline-variant/10 relative">
                  <input className="w-full bg-surface border-none rounded-xl py-3 pl-4 pr-12 text-xs text-on-surface outline-none focus:ring-1 focus:ring-primary" placeholder="답변을 입력하세요..." type="text"/>
                  <button className="absolute right-6 top-1/2 -translate-y-1/2 text-primary active:scale-90 transition-transform"><span className="material-symbols-outlined text-xl">send</span></button>
                </div>
              </div>
            </div>
          </section>
        </div>
      </main>

      {/* BottomNavBar (Mobile Only) */}
      <nav className="fixed bottom-0 w-full z-50 flex justify-around items-center px-6 pb-6 pt-3 md:hidden bg-white/80 backdrop-blur-2xl rounded-t-[2rem] border-t border-outline-variant/10 shadow-[0_-10px_40px_rgba(0,0,0,0.05)]">
        <button className="flex flex-col items-center justify-center text-on-surface-variant p-2 hover:text-primary transition-transform scale-110">
          <span className="material-symbols-outlined">explore</span>
          <span className="font-headline text-[10px] font-bold uppercase mt-1">둘러보기</span>
        </button>
        <button className="flex flex-col items-center justify-center text-on-surface-variant p-2 hover:text-primary transition-transform scale-110">
          <span className="material-symbols-outlined">library_add</span>
          <span className="font-headline text-[10px] font-bold uppercase mt-1">등록</span>
        </button>
        <button className="flex flex-col items-center justify-center text-on-surface-variant p-2 hover:text-primary transition-transform scale-110">
          <span className="material-symbols-outlined">forum</span>
          <span className="font-headline text-[10px] font-bold uppercase mt-1">문의</span>
        </button>
        <button className="flex flex-col items-center justify-center bg-primary text-on-primary rounded-2xl p-2 shadow-[0_0_20px_rgba(0,106,113,0.3)] transition-transform scale-110">
          <span className="material-symbols-outlined">analytics</span>
          <span className="font-headline text-[10px] font-bold uppercase mt-1">관리</span>
        </button>
      </nav>
    </div>
  );
};

export default SellerHome;
