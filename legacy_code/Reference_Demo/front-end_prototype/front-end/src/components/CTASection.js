import React from 'react';
import { Link, useNavigate } from 'react-router-dom';

const CTASection = () => {
  const navigate = useNavigate();
  return (
    <section className="py-24 px-8">
      <div className="max-w-5xl mx-auto rounded-3xl overflow-hidden relative p-12 md:p-20 flex flex-col items-center text-center">
        <div className="absolute inset-0 bg-primary/5 backdrop-blur-3xl z-0"></div>
        <div className="absolute inset-0 border border-primary/10 rounded-3xl z-0"></div>
        <div className="relative z-10">
          <h2 className="headline text-4xl md:text-6xl font-bold mb-8 leading-tight">지식의 소유, <br className="md:hidden"/> 그 이상의 가치.</h2>
          <p className="text-on-surface-variant text-lg mb-12 max-w-2xl mx-auto font-body">지금 바로 대학생 공동주문 마켓의 첫 번째 공동구매 라운드에 참여하고, 당신만의 지식 생태계를 구축하세요.</p>
          <div className="flex flex-col sm:flex-row gap-4">
            <Link to="/login" className="px-12 py-4 bg-primary text-on-primary font-bold rounded-lg neon-glow-primary hover:scale-105 transition-transform font-headline">무료 가입하기</Link>
            <button 
              onClick={() => navigate('/login')}
              className="px-12 py-4 glass-card text-on-surface font-bold rounded-lg hover:bg-surface-variant transition-all font-headline"
            >
              문의하기
            </button>
          </div>
        </div>
      </div>
    </section>
  );
};

export default CTASection;
