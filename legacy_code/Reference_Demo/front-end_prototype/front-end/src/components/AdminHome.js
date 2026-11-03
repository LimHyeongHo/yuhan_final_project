import React from 'react';
import { useNavigate } from 'react-router-dom';

const AdminHome = () => {
  const navigate = useNavigate();
  const handleLogout = () => navigate('/login');

  return (
    <div className="bg-surface text-on-surface font-body min-h-screen overflow-x-hidden selection:bg-primary selection:text-on-primary">
      {/* TopNavBar */}
      <nav className="fixed top-0 w-full z-50 flex justify-between items-center px-8 h-16 bg-white/40 backdrop-blur-xl border-b border-outline-variant/10 shadow-[0_4px_30px_rgba(0,0,0,0.05)]">
        <div className="flex items-center gap-12">
          <span className="text-2xl font-black italic text-primary drop-shadow-[0_0_8px_rgba(0,106,113,0.2)] font-headline tracking-tight uppercase">대학생 공동주문 마켓</span>
          <div className="hidden lg:flex items-center gap-8 font-headline tracking-tight text-sm">
            <a className="text-on-surface-variant hover:text-primary transition-colors uppercase font-bold tracking-widest" href="#">대시보드</a>
            <a className="text-on-surface-variant hover:text-primary transition-colors uppercase font-bold tracking-widest" href="#">마켓</a>
            <a className="text-on-surface-variant hover:text-primary transition-colors uppercase font-bold tracking-widest" href="#">주문 내역</a>
            <a className="text-on-surface-variant hover:text-primary transition-colors uppercase font-bold tracking-widest" href="#">커뮤니티</a>
          </div>
        </div>
        <div className="flex items-center gap-6">
          <div className="relative group hidden sm:block">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-sm">search</span>
            <input className="bg-surface-container-highest border-none rounded-full py-2 pl-10 pr-4 text-xs w-64 focus:ring-1 focus:ring-primary outline-none text-on-surface placeholder:text-on-surface-variant/50" placeholder="통합 검색..." type="text"/>
          </div>
          <div className="flex items-center gap-4 text-primary">
            <button className="hover:bg-primary/10 p-2 rounded-full transition-transform active:scale-90"><span className="material-symbols-outlined">notifications</span></button>
            <button className="hover:bg-primary/10 p-1 rounded-full border border-primary/20 transition-transform active:scale-90"><span className="material-symbols-outlined text-3xl">account_circle</span></button>
          </div>
        </div>
      </nav>

      {/* SideNavBar */}
      <aside className="fixed left-0 top-16 h-[calc(100vh-64px)] w-64 bg-white border-r border-outline-variant/10 flex flex-col z-40 hidden lg:flex shadow-[10px_0_30px_rgba(0,0,0,0.05)]">
        <div className="p-6 border-b border-surface-container text-on-surface">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-surface-container-highest flex items-center justify-center text-primary overflow-hidden border border-primary/20">
              <img alt="Admin" className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuCSbEnvKyd3qju-BxGN1o98L8MfIw2-RrrAZit-UX52jw19gI7Gb39Bg1zZ8-nx5Dd5Adeb-viCvSYAdQTJFO1s-oYopjlIHTDKekan_MO5P1ZtanVOzu0nrDwGsPv3tqDu_qItpYFZpM3G2RSrI7wbsrtBnfPxY5i95H16iVB-BuSmbK4UuDSpJp6JZvJjzMqPKg-OhQ6nM11MqdegZCPEh6Jvd1tJtJ3NMszcmXllRM-xFmm8pMUU2KmoHd4KrsOqYG2hBa7qX9W0"/>
            </div>
            <div>
              <p className="font-headline text-sm uppercase tracking-widest text-primary tracking-tighter">시스템 제어 센터</p>
              <p className="text-[10px] text-on-surface-variant uppercase tracking-tighter font-bold">권한: 슈퍼 어드민</p>
            </div>
          </div>
        </div>
        <div className="flex-1 py-6 space-y-1 font-headline text-sm uppercase tracking-widest font-bold">
          <a className="bg-gradient-to-r from-primary/20 to-transparent text-primary border-l-4 border-primary px-6 py-3 flex items-center gap-4" href="#"><span className="material-symbols-outlined text-xl">dashboard</span>대시보드 개요</a>
          <a className="text-on-surface-variant px-6 py-3 flex items-center gap-4 hover:translate-x-2 transition-transform hover:bg-surface-container" href="#"><span className="material-symbols-outlined text-xl">group</span>회원 관리</a>
          <a className="text-on-surface-variant px-6 py-3 flex items-center gap-4 hover:translate-x-2 transition-transform hover:bg-surface-container" href="#"><span className="material-symbols-outlined text-xl">inventory_2</span>재고 현황</a>
          <a className="text-on-surface-variant px-6 py-3 flex items-center gap-4 hover:translate-x-2 transition-transform hover:bg-surface-container" href="#"><span className="material-symbols-outlined text-xl">local_shipping</span>물류 시스템</a>
          <a className="text-on-surface-variant px-6 py-3 flex items-center gap-4 hover:translate-x-2 transition-transform hover:bg-surface-container" href="#"><span className="material-symbols-outlined text-xl">insights</span>통계 분석</a>
        </div>
        <div className="p-6 space-y-4">
          <button 
            onClick={() => navigate('/login')}
            className="w-full py-3 px-4 bg-primary text-on-primary rounded-lg font-bold text-xs uppercase tracking-widest shadow-[0_0_15px_rgba(0,106,113,0.1)] hover:brightness-110 transition-all uppercase"
          >
            보고서 생성
          </button>
          <div className="pt-4 border-t border-surface-container font-headline uppercase text-xs tracking-widest font-bold">
            <a className="text-on-surface-variant py-2 flex items-center gap-4 hover:text-primary transition-colors" href="#"><span className="material-symbols-outlined text-xl">settings</span>설정</a>
            <button onClick={handleLogout} className="text-error/70 py-2 flex items-center gap-4 hover:text-error transition-colors uppercase font-bold"><span className="material-symbols-outlined text-xl">logout</span>로그아웃</button>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="lg:ml-64 pt-16 min-h-screen pb-24 md:pb-8">
        {/* Hero Section */}
        <section className="relative h-[450px] w-full overflow-hidden flex items-center px-12">
          <div className="absolute inset-0 z-0">
            <iframe className="w-full h-full border-none opacity-60 pointer-events-none" src="https://prod.spline.design/dcNTIp6-Bo2-fcQ3/scene.splinecode"></iframe>
          </div>
          <div className="absolute inset-0 bg-gradient-to-r from-surface via-transparent to-transparent z-10 pointer-events-none"></div>
          <div className="relative z-20 max-w-2xl">
            <p className="font-headline text-primary uppercase tracking-[0.4em] text-xs mb-4 font-bold">시스템 통합 관리 v4.0</p>
            <h1 className="font-headline text-6xl md:text-7xl font-black text-on-surface leading-tight tracking-tighter mb-6 uppercase">COLLEGE <br/><span className="text-transparent bg-clip-text bg-gradient-to-r from-primary to-secondary">MARKETPLACE</span></h1>
            <div className="flex gap-4">
              <div className="glass-card bg-white/40 border border-outline-variant/10 p-4 rounded-xl flex items-center gap-4 backdrop-blur-xl shadow-[0_4px_30px_rgba(0,0,0,0.05)]">
                <div className="text-3xl font-bold text-on-surface font-headline">4.2k</div>
                <div className="text-[10px] text-on-surface-variant uppercase leading-tight tracking-widest font-bold">활성 <br/>회원수</div>
              </div>
              <div className="glass-card bg-white/40 border border-outline-variant/10 p-4 rounded-xl flex items-center gap-4 backdrop-blur-xl shadow-[0_4px_30px_rgba(0,0,0,0.05)]">
                <div className="text-3xl font-bold text-secondary font-headline">89%</div>
                <div className="text-[10px] text-on-surface-variant uppercase leading-tight tracking-widest font-bold">성장 <br/>속도</div>
              </div>
            </div>
          </div>
        </section>

        <div className="px-8 -mt-12 relative z-30 grid grid-cols-12 gap-6">
          {/* 공동구매 현황 테이블 */}
          <div className="col-span-12 lg:col-span-8 space-y-6">
            <div className="glass-card bg-white/60 border border-outline-variant/10 rounded-2xl p-8 shadow-2xl backdrop-blur-xl shadow-[0_8px_32px_rgba(0,0,0,0.05)]">
              <div className="flex justify-between items-end mb-8">
                <div>
                  <h2 className="font-headline text-2xl font-bold text-on-surface uppercase tracking-tighter">공동구매 현황 모니터링</h2>
                  <p className="text-on-surface-variant text-sm font-medium">실시간 참여 인원 및 단계별 상태 관리</p>
                </div>
                <span className="px-3 py-1 bg-surface-container-highest text-[10px] text-on-surface-variant rounded-full border border-outline-variant/30 font-bold uppercase">v4.0 ACTIVE</span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left border-separate border-spacing-y-3">
                  <thead>
                    <tr className="text-[10px] text-on-surface-variant uppercase tracking-widest font-bold">
                      <th className="pb-4 px-4">프로젝트</th>
                      <th className="pb-4 px-4">참여 인원</th>
                      <th className="pb-4 px-4">상태</th>
                      <th className="pb-4 px-4 text-right">관리</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr className="bg-surface-container-low hover:bg-surface-container transition-colors group">
                      <td className="px-4 py-4 rounded-l-xl">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 bg-surface-container-highest rounded-lg overflow-hidden flex-shrink-0 border border-outline-variant/10">
                            <img alt="P1" className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuChKJPY9X2gaMT1F3O58axjk3gcayjJMIj6Ka0kci-qEL26JUxuKDMUo7Lmjc9y22XhUZ6wSYalEQKnY9rUtIKC6vGkUCthTe62i6EOVhjjNtO9oxSwhFHBo6YBrN-3PzOtLgAz6KdWxG0UfMGtrGcOA15ynVfSC0K3lHiwLQv9kfhEeBZKj7w3FNFFIcPJodAZVic17ec9KRzY7Ziec22uzhsiWv8Y0BFfCRwa-CaCmx1QRBxNSY3A1SvgnrWxv3NALT9wiSoJOkXK"/>
                          </div>
                          <div><div className="text-sm font-bold text-on-surface uppercase font-headline">인문학 전집</div><div className="text-[10px] text-on-surface-variant font-bold">LIB-001</div></div>
                        </div>
                      </td>
                      <td className="px-4 py-4">
                        <div className="flex -space-x-2">
                          {[1, 2, 3].map(i => (
                            <div key={i} className="w-6 h-6 rounded-full border-2 border-surface bg-surface-container-highest overflow-hidden">
                              <img alt="U" className="w-full h-full object-cover" src={`https://lh3.googleusercontent.com/aida-public/AB6AXuCIA5a-Dt3uMPPeeSaV6Bqt61Ufh8T4gOfwK7fzqVPRRUBfdt0wVUXyZ6mohBHO_G_hbnEVOqcpKMSDXswzHougws2If2w-umDUnM54bM27qiqv1uM-l5ItrCPqv8qhZu6Qi1HdZ9pGvRzKmAKBscke4mfOSPMBhEc9u8wY-ATk_cjtcrQjEo8k9Wjn3l2qCTIqqcnfJFkh6hv6JuOA7d9On5W9lrD1TdrOYnMGofoD14qfVP9Qd9_lEdHz_qGMf2DECvlvX93PDbpk`} />
                            </div>
                          ))}
                          <div className="w-6 h-6 rounded-full border-2 border-surface bg-primary/20 flex items-center justify-center text-[8px] text-primary font-bold">+42</div>
                        </div>
                      </td>
                      <td className="px-4 py-4 text-sm"><span className="px-3 py-1 bg-primary/10 text-primary text-[10px] font-black uppercase rounded-full border border-primary/20">모집 중</span></td>
                      <td className="px-4 py-4 rounded-r-xl text-right"><span className="material-symbols-outlined text-on-surface-variant cursor-pointer hover:text-primary transition-colors">settings</span></td>
                    </tr>
                    <tr className="bg-surface-container-low hover:bg-surface-container transition-colors group">
                      <td className="px-4 py-4 rounded-l-xl font-bold">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 bg-surface-container-highest rounded-lg overflow-hidden border border-outline-variant/10">
                            <img alt="P2" className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBhfHvrY18DclS8g4jwiCcQt1afheVA6KhCAQVgQsJGJnP-ySiYCvoDHwlmClkq2qUlb-d9ThnyBOhVtMmv3HNEd3pmnZC5sQHqY08obL3q31E3Ta-XFX0HdhYoYdw__l7JF_jsHq48iLfqFqhH8ld-K18fyXQuu6kP5eMPMTfTKvCOX6x4c5qybtbYwYWSr6hSxYf_UMZJJSNDc3dYbenAGQ4Gn71s3JmFaRTP2xwIfYxXyVSB2RTQaZ3viPvM5q0Ja52JxE_ZdHzw"/>
                          </div>
                          <div><div className="text-sm font-bold text-on-surface uppercase font-headline">기술 한정판</div><div className="text-[10px] text-on-surface-variant font-bold">TECH-88</div></div>
                        </div>
                      </td>
                      <td className="px-4 py-4">
                        <div className="flex -space-x-2"><div className="w-6 h-6 rounded-full border-2 border-surface bg-secondary/20 flex items-center justify-center text-[8px] text-secondary font-bold">+102</div></div>
                      </td>
                      <td className="px-4 py-4 text-sm"><span className="px-3 py-1 bg-secondary/10 text-secondary text-[10px] font-black uppercase rounded-full border border-secondary/20">발주 완료</span></td>
                      <td className="px-4 py-4 rounded-r-xl text-right"><span className="material-symbols-outlined text-on-surface-variant cursor-pointer hover:text-primary transition-colors">settings</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            {/* 커뮤니티 관리 섹션 */}
            <div className="glass-card bg-white/60 border border-outline-variant/10 rounded-2xl p-8 shadow-2xl backdrop-blur-xl shadow-[0_8px_32px_rgba(0,0,0,0.05)]">
              <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4 mb-8">
                <h2 className="font-headline text-2xl font-bold text-on-surface uppercase tracking-tighter">커뮤니티 관리 데스크</h2>
                <div className="flex items-center bg-surface-container-highest rounded-xl px-4 py-2 border border-outline-variant/10">
                  <span className="material-symbols-outlined text-on-surface-variant text-lg">search</span>
                  <input className="bg-transparent border-none focus:ring-0 text-sm placeholder:text-on-surface-variant/40 w-full sm:w-48 text-on-surface px-2 outline-none" placeholder="회원명 검색..." type="text"/>
                </div>
              </div>
              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 bg-surface-container-low rounded-xl border-l-2 border-transparent hover:border-primary transition-all">
                  <div className="flex items-center gap-4 uppercase">
                    <div className="w-12 h-12 rounded-full overflow-hidden bg-surface-container-highest border border-outline-variant/10">
                      <img alt="M1" className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuDndOoX3DEcPSJG9J3FsZ1mrNXh2OFm3Zvtc_Ne6Ejsk5xfIAaUbmRgYUXfFajQslGtDf_RdnDbqOSSeCJJVp19BQSd5JC8ffhNpCe2vPTt_fhwn9U8A_EhqJIQuc8-lwoQlp4IcEwXZMUwOHbn0zKPxveo_AJWInzfrAyZRpJIt03XEIjJZcYQJUgBQ1wKN1gKIEPdW-TRcr3EfNfDXz2JIMOCleTarP_hpKfo4XRUx-lXUIkXiusx-dPl4ulDI7y3Y2eT-20AfsOd"/>
                    </div>
                    <div><p className="text-sm font-black text-on-surface headline-font tracking-tighter">Alex_Digital</p><p className="text-[10px] text-on-surface-variant font-bold tracking-widest leading-none">등급: 다이아몬드 익스플로러</p></div>
                  </div>
                  <div className="flex items-center gap-2">
                    <button 
                      onClick={() => navigate('/login')}
                      className="px-4 py-2 text-[10px] font-black uppercase bg-error/10 text-error rounded-lg hover:bg-error hover:text-white transition-all"
                    >
                      제한
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* 시스템 제어 사이드 패널 */}
          <div className="col-span-12 lg:col-span-4 space-y-6 uppercase font-bold">
            <div className="glass-card bg-surface-container-high/80 border border-primary/20 rounded-2xl p-8 relative overflow-hidden shadow-[0_8px_32px_rgba(0,0,0,0.05)]">
              <div className="absolute -top-12 -right-12 w-32 h-32 bg-primary/10 blur-[60px] rounded-full"></div>
              <h3 className="font-headline text-xl font-bold text-on-surface mb-2 tracking-tighter">마켓 파라미터 조절</h3>
              <p className="text-[10px] text-on-surface-variant mb-8 tracking-widest">전체 플랫폼 할인율 수정</p>
              <div className="space-y-8">
                <div>
                  <div className="flex justify-between items-center mb-4"><label className="text-[10px] text-primary tracking-[0.2em]">기본 할인율</label><span className="text-lg font-headline font-bold text-on-surface">15%</span></div>
                  <input className="w-full h-2 bg-surface-container-highest rounded-lg appearance-none cursor-pointer accent-primary" type="range"/>
                </div>
                <button 
                  onClick={() => navigate('/login')}
                  className="w-full py-4 bg-surface-container-highest border border-primary/40 text-primary text-[10px] font-black rounded-xl hover:bg-primary hover:text-on-primary transition-all tracking-[0.3em]"
                >
                  설정값 동기화
                </button>
              </div>
            </div>

            <div className="glass-card bg-white/60 border border-outline-variant/10 rounded-2xl p-8 shadow-2xl backdrop-blur-xl shadow-[0_8px_32px_rgba(0,0,0,0.05)]">
              <h3 className="font-headline text-xl font-bold text-on-surface mb-6 tracking-tighter">진행 목표 관리</h3>
              <div className="space-y-6">
                <div>
                  <div className="flex justify-between text-[10px] mb-2 font-bold tracking-widest"><span className="text-on-surface-variant">모집 목표량</span><span className="text-on-surface">72%</span></div>
                  <div className="h-2 bg-surface-container-highest rounded-full overflow-hidden"><div className="h-full w-[72%] bg-gradient-to-r from-primary to-secondary"></div></div>
                </div>
                <div>
                  <div className="flex justify-between text-[10px] mb-2 font-bold tracking-widest"><span className="text-on-surface-variant">물류 처리율</span><span className="text-on-surface">45%</span></div>
                  <div className="h-2 bg-surface-container-highest rounded-full overflow-hidden"><div className="h-full w-[45%] bg-gradient-to-r from-secondary to-primary"></div></div>
                </div>
              </div>
              <div className="mt-8 p-4 rounded-xl bg-primary/5 border border-primary/10">
                <div className="flex items-center gap-3 mb-2 text-primary">
                  <span className="material-symbols-outlined text-sm font-bold">bolt</span>
                  <p className="text-[10px] font-black tracking-widest">시스템 최적화 상태</p>
                </div>
                <p className="text-[11px] text-on-surface-variant tracking-normal font-medium normal-case">플랫폼 캐시 효율 99.8%. 모든 노드 배포가 최적 상태입니다.</p>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* BottomNavBar (Mobile Only) */}
      <nav className="fixed bottom-0 w-full z-50 flex justify-around items-center px-6 pb-6 pt-3 md:hidden bg-white/80 backdrop-blur-2xl border-t border-outline-variant/10 rounded-t-[2rem] shadow-[0_-10px_40px_rgba(0,0,0,0.05)]">
        <button className="flex flex-col items-center justify-center bg-primary text-on-primary rounded-2xl p-2 shadow-[0_0_20px_rgba(0,106,113,0.3)] scale-110 transition-transform">
          <span className="material-symbols-outlined">explore</span>
          <span className="font-headline text-[10px] font-bold uppercase mt-1">탐색</span>
        </button>
        <button className="flex flex-col items-center justify-center text-on-surface-variant p-2 hover:text-primary transition-transform">
          <span className="material-symbols-outlined">add_shopping_cart</span>
          <span className="font-headline text-[10px] font-bold uppercase mt-1">참여</span>
        </button>
        <button className="flex flex-col items-center justify-center text-on-surface-variant p-2 hover:text-primary transition-transform">
          <span className="material-symbols-outlined">chat_bubble</span>
          <span className="font-headline text-[10px] font-bold uppercase mt-1">메시지</span>
        </button>
        <button className="flex flex-col items-center justify-center text-on-surface-variant p-2 hover:text-primary transition-transform">
          <span className="material-symbols-outlined">query_stats</span>
          <span className="font-headline text-[10px] font-bold uppercase mt-1">추적</span>
        </button>
      </nav>
    </div>
  );
};

export default AdminHome;
