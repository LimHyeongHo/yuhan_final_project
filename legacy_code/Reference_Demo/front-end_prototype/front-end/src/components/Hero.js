import React from 'react';
import { useNavigate } from 'react-router-dom';

const Hero = () => {
  const navigate = useNavigate();

  return (
    <section className="relative min-h-[921px] flex items-center justify-center overflow-hidden px-8">
      <div className="absolute inset-0 z-0 opacity-60">
        <spline-viewer url="https://prod.spline.design/dcNTIp6-Bo2-fcQ3/scene.splinecode"></spline-viewer>
      </div>
      <div className="relative z-10 text-center max-w-4xl">
        <span className="inline-block py-1 px-3 mb-6 rounded-full bg-primary/10 border border-primary/20 text-primary font-label text-xs tracking-widest uppercase">The Future of Rare Books</span>
        <h1 className="headline text-6xl md:text-8xl font-bold tracking-tighter mb-8 leading-[1.1] uppercase">
          대학생 <span className="text-gradient-primary">공동주문 마켓</span>
        </h1>
        <p className="text-on-surface-variant text-xl md:text-2xl mb-12 font-body max-w-2xl mx-auto leading-relaxed">
          디지털 큐레이션이 제안하는 새로운 독서 경험. <br className="hidden md:block"/> 희귀 중고 도서 공동구매의 새로운 지평을 엽니다.
        </p>
        <div className="flex flex-col md:flex-row items-center justify-center gap-6">
          <button 
            onClick={() => navigate('/login')}
            className="w-full md:w-auto px-10 py-4 bg-primary text-on-primary font-bold rounded-lg neon-glow-primary hover:scale-105 transition-transform font-headline"
          >
            지금 시작하기
          </button>
          <button 
            onClick={() => navigate('/login')}
            className="w-full md:w-auto px-10 py-4 glass-card text-on-surface font-bold rounded-lg hover:bg-surface-variant transition-all font-headline"
          >
            작품 둘러보기
          </button>
        </div>
      </div>
      <div className="absolute bottom-10 left-1/2 -translate-x-1/2 flex flex-col items-center opacity-40">
        <span className="text-xs tracking-widest mb-2 font-label uppercase">Scroll to Explore</span>
        <span className="material-symbols-outlined">expand_more</span>
      </div>
    </section>
  );
};

export default Hero;
