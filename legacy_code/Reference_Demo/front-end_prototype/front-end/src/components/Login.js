import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

const Login = () => {
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();

  const handleLogin = (e) => {
    e.preventDefault();

    if (userId === 'admin1' && password === 'admin1') {
      navigate('/buyer');
    } else if (userId === 'admin2' && password === 'admin2') {
      navigate('/seller');
    } else if (userId === 'admin3' && password === 'admin3') {
      navigate('/admin');
    } else {
      alert('아이디 또는 비밀번호가 일치하지 않습니다.\n(테스트 계정: admin1, admin2, admin3)');
    }
  };

  return (
    <main className="min-h-screen relative flex items-center justify-center px-4 pt-20 overflow-hidden bg-background">
      {/* Background Atmospheric Element */}
      <div className="absolute inset-0 z-0">
        <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] bg-primary/10 blur-[120px] rounded-full"></div>
        <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] bg-secondary/10 blur-[120px] rounded-full"></div>
        <img 
          className="w-full h-full object-cover opacity-20 mix-blend-overlay" 
          src="https://lh3.googleusercontent.com/aida-public/AB6AXuDcz5XHxo7VCUuaMvSbNpJP6pR0MzC90FQvChAogRglNe_xxlrQDfBTx4R4-cFGmXXCz3LzZ4NqfvBACI2cnyBqT-9m2NRxmT2yn7EFgswg3z8XU4CQot0El4jGZDfbZRGVMeMkIlolLJ3-BB7AkcDiYonB9Baqh9HVCy26rcASvXUWrRhohwH0mOURZDIdM6MhehT8LSzR5YzBftVcEHkwaGsaA3vMbCNr1f5rvjlGHItmaXCN_HQZgcg22yl_TxXlgIFFrYiOA7Up" 
          alt="Atmosphere"
        />
      </div>

      {/* Login Container */}
      <div className="relative z-10 w-full max-w-md">
        <div className="glass-panel border border-outline-variant/15 rounded-xl p-8 md:p-10 shadow-[0_20px_50px_rgba(0,0,0,0.05)] bg-white/60 backdrop-blur-xl">
          {/* Header Section */}
          <div className="mb-10 text-center">
            <span className="text-secondary font-headline text-[10px] uppercase tracking-[0.3em] mb-2 block">Secure Access</span>
            <h1 className="text-3xl font-headline font-bold text-on-surface tracking-tight">로그인</h1>
            <p className="text-on-surface-variant text-sm mt-2">테스트 계정: admin1(구매자), admin2(판매자), admin3(관리자)</p>
          </div>

          {/* Form Section */}
          <form className="space-y-6" onSubmit={handleLogin}>
            <div className="space-y-2">
              <label className="block text-[10px] uppercase tracking-widest text-on-surface-variant font-bold ml-1">이메일 또는 사용자 이름</label>
              <div className="relative group">
                <input 
                  className="w-full bg-surface-container border-none rounded-lg py-4 px-5 text-on-surface placeholder:text-outline focus:ring-0 focus:outline-none transition-all duration-300 border-b-2 border-transparent focus:border-primary outline-none" 
                  placeholder="admin1" 
                  type="text"
                  value={userId}
                  onChange={(e) => setUserId(e.target.value)}
                />
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between items-center ml-1">
                <label className="text-[10px] uppercase tracking-widest text-on-surface-variant font-bold">비밀번호</label>
              </div>
              <div className="relative group">
                <input 
                  className="w-full bg-surface-container border-none rounded-lg py-4 px-5 text-on-surface placeholder:text-outline focus:ring-0 focus:outline-none transition-all duration-300 border-b-2 border-transparent focus:border-primary outline-none" 
                  placeholder="••••••••" 
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
            </div>
            <div className="pt-2">
              <button className="w-full bg-primary text-on-primary font-headline font-bold py-4 rounded-lg shadow-[0_0_20px_rgba(0,106,113,0.1)] hover:shadow-[0_0_30px_rgba(0,106,113,0.2)] transition-all duration-300 transform active:scale-[0.98]" type="submit">
                로그인
              </button>
            </div>
          </form>

          {/* Divider */}
          <div className="relative my-8 flex items-center">
            <div className="flex-grow border-t border-outline-variant/15"></div>
            <span className="mx-4 text-[10px] uppercase tracking-[0.2em] text-outline font-bold">OR CONNECT WITH</span>
            <div className="flex-grow border-t border-outline-variant/15"></div>
          </div>

          {/* Social Logins */}
          <div className="grid grid-cols-2 gap-4">
            <button className="flex items-center justify-center gap-3 bg-white hover:bg-surface-container transition-colors py-3 rounded-lg border border-outline-variant/15 group">
              <img alt="Google" className="w-5 h-5 opacity-70 group-hover:opacity-100 transition-opacity" src="https://lh3.googleusercontent.com/aida-public/AB6AXuAeL4HPtZ2n0ZWWQdFlAGpo8kw0bFoSOu4lNZ9hYiE1qTeQqIpTA4UH54CwGBH6DuNq7k7DYWrl96vBXCRWDQZxZxfjc3h-rs08K5rI2CN6YemA9mzUOc5sxNBLXsYgjAJueI-jwxMecZku5mqWJP3-UpeJfyUyan3eS3i24tYTrRQ6kpBEy7e_fj0toRGC3j66FHTYxubnOOorHWUu0FtojrqAPTbtw2LCiBzeCmGqkYWIPbxGIDtkUrDVkp2viaYUt_mHyJD-c07x" />
              <span className="text-xs font-bold uppercase tracking-wider">Google</span>
            </button>
            <button className="flex items-center justify-center gap-3 bg-white hover:bg-surface-container transition-colors py-3 rounded-lg border border-outline-variant/15 group">
              <span className="material-symbols-outlined text-tertiary">account_balance_wallet</span>
              <span className="text-xs font-bold uppercase tracking-wider">Wallet</span>
            </button>
          </div>

          {/* Footer Link */}
          <p className="mt-10 text-center text-sm text-on-surface-variant">
            계정이 없으신가요? 
            <Link className="text-primary font-bold hover:underline ml-1" to="/signup">회원가입</Link>
          </p>
        </div>

        {/* Bottom decorative links */}
        <div className="mt-8 flex justify-center items-center gap-8 text-[10px] uppercase tracking-widest text-outline">
          <Link className="hover:text-on-surface transition-colors" to="#">이용약관</Link>
          <span className="w-1 h-1 bg-outline-variant rounded-full"></span>
          <Link className="hover:text-on-surface transition-colors" to="#">개인정보처리방침</Link>
          <span className="w-1 h-1 bg-outline-variant rounded-full"></span>
          <Link className="hover:text-on-surface transition-colors" to="#">고객지원</Link>
        </div>
      </div>

      {/* Asymmetric Detail Decor */}
      <div className="hidden lg:block absolute right-20 top-1/2 -translate-y-1/2 w-64 h-96 opacity-40">
        <div className="absolute inset-0 border-r border-b border-primary/20 translate-x-4 translate-y-4"></div>
        <img 
          className="w-full h-full object-cover grayscale brightness-50 contrast-125" 
          src="https://lh3.googleusercontent.com/aida-public/AB6AXuBXIXqopjCB3u8IEycdxcrqVb8YBQE9xOC6-qDOIo36lpvZxwNS8CuIko-f-PAMzCxjdh-GO0Zdl6aax3quN2AkBCJzRwZG-ANz5HQum2|nvV4FcltqzMyIhlq9h_v-ORLL--6N--71ymhhBqe-M_nU3P0jBNgm3nGZjwUzJcm2DYHJGFvS-PhLXlkRT-JnREMHZ74u20Nvg2KAV4KkJ9ciNhMjW4gZR3O36LROeHag4oyGNnzk0W47zOgKO0L2jx9JJioirgg-ip0" 
          alt="Decor"
        />
        <div className="absolute -bottom-4 -left-4 font-headline text-5xl font-extrabold text-primary/10 select-none">
          0101
        </div>
      </div>
    </main>
  );
};

export default Login;
