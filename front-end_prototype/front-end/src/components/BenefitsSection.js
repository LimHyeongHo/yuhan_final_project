import React from 'react';

const BenefitsSection = () => {
  return (
    <section className="py-24 bg-surface-container-low relative">
      <div className="absolute top-0 left-0 w-full h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent"></div>
      <div className="max-w-7xl mx-auto px-8">
        <div className="text-center mb-20">
          <h2 className="headline text-4xl md:text-5xl font-bold mb-6 tracking-tight">The <span className="text-gradient-primary">Advantage</span></h2>
          <p className="text-on-surface-variant max-w-xl mx-auto font-body font-medium">대학생 공동주문 마켓이 제안하는 독보적인 세 가지 혜택</p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Price Benefit */}
          <div className="glass-card p-10 rounded-2xl hover:translate-y-[-8px] transition-transform duration-500">
            <div className="mb-8 flex items-center justify-center w-14 h-14 rounded-xl bg-primary/10 border border-primary/20">
              <span className="material-symbols-outlined text-primary text-3xl">trending_down</span>
            </div>
            <h4 className="headline text-2xl font-bold mb-4">혁신적인 가격</h4>
            <p className="text-on-surface-variant leading-relaxed font-body">공동구매 엔진을 통해 개인 거래보다 최대 40% 저렴한 가격으로 고가 희귀본을 획득할 수 있습니다.</p>
          </div>
          {/* Rare Content Benefit */}
          <div className="glass-card p-10 rounded-2xl hover:translate-y-[-8px] transition-transform duration-500 bg-surface-container-high/50">
            <div className="mb-8 flex items-center justify-center w-14 h-14 rounded-xl bg-secondary/10 border border-secondary/20">
              <span className="material-symbols-outlined text-secondary text-3xl">diamond</span>
            </div>
            <h4 className="headline text-2xl font-bold mb-4">희귀본 우선 확보</h4>
            <p className="text-on-surface-variant leading-relaxed font-body">전 세계 네트워크를 활용해 시장에 나오지 않은 절판본과 리미티드 에디션을 가장 먼저 소싱합니다.</p>
          </div>
          {/* Quality Benefit */}
          <div className="glass-card p-10 rounded-2xl hover:translate-y-[-8px] transition-transform duration-500">
            <div className="mb-8 flex items-center justify-center w-14 h-14 rounded-xl bg-tertiary/10 border border-tertiary/20">
              <span className="material-symbols-outlined text-tertiary text-3xl">verified</span>
            </div>
            <h4 className="headline text-2xl font-bold mb-4">엄격한 품질 검증</h4>
            <p className="text-on-surface-variant leading-relaxed font-body">모든 도서는 12단계의 정밀 검수 프로세스를 거쳐 디지털 인증서와 함께 안전하게 배송됩니다.</p>
          </div>
        </div>
      </div>
    </section>
  );
};

export default BenefitsSection;
