import React from 'react';
import { Link } from 'react-router-dom';

const SignUp = () => {
  return (
    <main className="min-h-screen pt-20 flex items-center justify-center relative overflow-hidden bg-background">
      {/* Background 3D Placeholder/Visual Elements */}
      <div className="absolute inset-0 z-0 opacity-40 pointer-events-none">
        <div className="absolute top-1/4 -left-20 w-96 h-96 bg-primary/10 rounded-full blur-[120px]"></div>
        <div className="absolute bottom-1/4 -right-20 w-96 h-96 bg-secondary/10 rounded-full blur-[120px]"></div>
        <div className="absolute inset-0" style={{ backgroundImage: 'radial-gradient(circle at 50% 50%, #1a1919 0%, transparent 80%)' }}></div>
        <div className="w-full h-full flex items-center justify-center">
          <div className="w-[800px] h-[800px] border border-primary/5 rounded-full animate-pulse flex items-center justify-center">
            <div className="w-[600px] h-[600px] border border-secondary/5 rounded-full"></div>
          </div>
        </div>
      </div>

      {/* Auth Container */}
      <section className="relative z-10 w-full max-w-2xl px-6 py-12">
        <div className="glass-panel border border-outline-variant/10 rounded-xl overflow-hidden shadow-2xl bg-white/60 backdrop-blur-xl">
          {/* Auth Header */}
          <div className="p-8 md:p-12 text-center">
            <h1 className="text-4xl md:text-5xl font-headline font-bold tracking-tight text-on-surface mb-4">시작하기</h1>
            <p className="text-on-surface-variant font-body">디지털 큐레이터 서비스에 가입하여 새로운 가치를 발견하세요.</p>
          </div>

          {/* Role Selection Interface */}
          <div className="px-8 md:px-12 pb-8">
            <label className="block text-xs uppercase tracking-[0.2em] font-headline font-bold text-primary mb-6">역할 선택</label>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <button className="group p-4 bg-primary/5 border-2 border-primary rounded-lg transition-all active:scale-95 text-left">
                <span className="material-symbols-outlined text-primary mb-2">shopping_bag</span>
                <div className="font-headline font-bold text-on-surface">구매자</div>
                <div className="text-[10px] text-on-surface-variant leading-tight mt-1">유니크한 아이템을 탐색하고 구매합니다.</div>
              </button>
              <button className="group p-4 bg-surface-container hover:bg-surface-container-high transition-all border border-outline-variant/10 rounded-lg active:scale-95 text-left">
                <span className="material-symbols-outlined text-on-surface-variant group-hover:text-secondary transition-colors mb-2">store</span>
                <div className="font-headline font-bold text-on-surface-variant group-hover:text-on-surface">판매자</div>
                <div className="text-[10px] text-on-surface-variant leading-tight mt-1">자신의 가치를 시장에 선보이고 관리합니다.</div>
              </button>
              <button className="group p-4 bg-surface-container hover:bg-surface-container-high transition-all border border-outline-variant/10 rounded-lg active:scale-95 text-left">
                <span className="material-symbols-outlined text-on-surface-variant group-hover:text-tertiary transition-colors mb-2">admin_panel_settings</span>
                <div className="font-headline font-bold text-on-surface-variant group-hover:text-on-surface">관리자</div>
                <div className="text-[10px] text-on-surface-variant leading-tight mt-1">시스템 운영 및 커뮤니티를 모니터링합니다.</div>
              </button>
            </div>
          </div>

          {/* Registration Form */}
          <form className="px-8 md:px-12 space-y-6">
            <div className="space-y-4">
              <div className="relative group">
                <input className="w-full bg-surface-container border-none rounded-lg px-5 py-4 text-on-surface placeholder:text-outline focus:ring-0 transition-all font-body outline-none" placeholder="사용자 이름" type="text" />
                <div className="absolute bottom-0 left-0 w-0 h-[2px] bg-primary transition-all duration-300 group-focus-within:w-full shadow-[0_0_8px_rgba(0,106,113,0.3)]"></div>
              </div>
              <div className="relative group">
                <input className="w-full bg-surface-container border-none rounded-lg px-5 py-4 text-on-surface placeholder:text-outline focus:ring-0 transition-all font-body outline-none" placeholder="이메일 주소" type="email" />
                <div className="absolute bottom-0 left-0 w-0 h-[2px] bg-primary transition-all duration-300 group-focus-within:w-full shadow-[0_0_8px_rgba(0,106,113,0.3)]"></div>
              </div>
              <div className="relative group">
                <input className="w-full bg-surface-container border-none rounded-lg px-5 py-4 text-on-surface placeholder:text-outline focus:ring-0 transition-all font-body outline-none" placeholder="비밀번호" type="password" />
                <div className="absolute bottom-0 left-0 w-0 h-[2px] bg-primary transition-all duration-300 group-focus-within:w-full shadow-[0_0_8px_rgba(0,106,113,0.3)]"></div>
              </div>
            </div>
            <button className="w-full bg-primary text-on-primary font-headline font-bold py-4 rounded-lg shadow-[0_0_20px_rgba(0,106,113,0.1)] hover:shadow-[0_0_30px_rgba(0,106,113,0.2)] transition-all active:scale-[0.98]" type="submit">
              계정 생성하기
            </button>
          </form>

          {/* Social Authentication */}
          <div className="px-8 md:px-12 py-10">
            <div className="flex items-center gap-4 mb-8">
              <div className="h-[1px] flex-1 bg-outline-variant/20"></div>
              <span className="text-[10px] uppercase tracking-widest text-on-surface-variant font-headline">또는 다음으로 가입</span>
              <div className="h-[1px] flex-1 bg-outline-variant/20"></div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <button className="flex items-center justify-center gap-3 bg-white hover:bg-surface-container border border-outline-variant/10 py-3 rounded-lg transition-all group">
                <img alt="Google Logo" className="w-5 h-5 opacity-80 group-hover:opacity-100" src="https://lh3.googleusercontent.com/aida-public/AB6AXuA-jmK7Jm-3FC2MF7ia8AvuWd0vUY_z9j_xBqgS3w1T_sVLXo8GYHqzGygPwWHawSEZdSSkRVuP2ssmKOhlwVXlGWJb_hlfabcIXqqKavbJcMzhlJ7drL_RhMuf86-0skjKX__AtCxDAbSLzQoLcFjAn9nb2kZam3eQuK2qIyl-QrOK7MYf3DxVJ5WlUFJmOQRgzEBTvUZllEV27vNITwPX2kBqexOG_wY-WkpT98qnLcT26uu_XZuL-mSBANDg4Sf364ZN8O4LoBbH" />
                <span className="text-sm font-medium text-on-surface-variant group-hover:text-on-surface font-body">Google</span>
              </button>
              <button className="flex items-center justify-center gap-3 bg-white hover:bg-surface-container border border-outline-variant/10 py-3 rounded-lg transition-all group">
                <span className="material-symbols-outlined text-on-surface-variant group-hover:text-on-surface">account_balance</span>
                <span className="text-sm font-medium text-on-surface-variant group-hover:text-on-surface font-body">Wallet</span>
              </button>
            </div>
            <div className="mt-8 text-center">
              <p className="text-on-surface-variant text-sm font-body">
                이미 계정이 있으신가요? <Link className="text-secondary font-bold hover:underline transition-all" to="/login">로그인</Link>
              </p>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
};

export default SignUp;
