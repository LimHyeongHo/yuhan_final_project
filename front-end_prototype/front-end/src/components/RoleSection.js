import React from 'react';
import { useNavigate } from 'react-router-dom';

const RoleSection = () => {
  const navigate = useNavigate();

  return (
    <section className="py-24 px-8 max-w-7xl mx-auto">
      <div className="mb-16">
        <h2 className="headline text-4xl font-bold mb-4 tracking-tight">Your Identity. <span className="text-primary">Your Role.</span></h2>
        <div className="h-1 w-24 bg-primary"></div>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6 h-auto md:h-[600px]">
        {/* Buyer */}
        <div className="md:col-span-8 group relative overflow-hidden glass-card rounded-xl p-8 flex flex-col justify-end">
          <div className="absolute inset-0 z-0 opacity-40 group-hover:scale-110 transition-transform duration-700 bg-cover bg-center" style={{backgroundImage: "url('https://lh3.googleusercontent.com/aida-public/AB6AXuA5pc1zFiujo3M7scbgJJG-iEZhKQn7hIrHdC3GiytVJwReaUYqO6aL0eeeVC4MgY3rAhQMa2bAJ7ZHOc_7aaeTxf3LHxT3-SR8T8VlVPcBnQnpB40TLB-ofpFlhyh1HormbrvtyzhYO6ft3le_CrYkO5crJKX3jSyNSPNV0y1SI2EajiwYa3i5qKClU6YzTFVKIvwhAMf_Evi7ZIYq0eZOPv1GftX9aPW0ybVf60Sed99HZmoNZoqNFfmZOFEnkskOQLyxwJEs5gNg')"}}></div>
          <div className="absolute inset-0 bg-gradient-to-t from-background via-background/40 to-transparent z-10"></div>
          <div className="relative z-20">
            <span className="text-primary font-bold tracking-widest text-sm font-label mb-2 block uppercase">COLLECTOR</span>
            <h3 className="headline text-4xl font-bold mb-4">구매자</h3>
            <p className="text-on-surface-variant mb-6 max-w-md font-body">합리적인 가격으로 검증된 품질의 희귀본을 소유하세요. 공동구매의 힘으로 가능해집니다.</p>
            <button 
              onClick={() => navigate('/login')}
              className="bg-primary text-on-primary px-8 py-3 rounded-lg font-bold font-headline hover:bg-primary/90 transition-colors"
            >
              참여하기
            </button>
          </div>
        </div>
        {/* Seller */}
        <div className="md:col-span-4 group relative overflow-hidden glass-card rounded-xl p-8 flex flex-col justify-end">
          <div className="absolute inset-0 z-0 opacity-40 group-hover:scale-110 transition-transform duration-700 bg-cover bg-center" style={{backgroundImage: "url('https://lh3.googleusercontent.com/aida-public/AB6AXuDLGVawJwgVdoPPoolvYANthpQBjkKZREPQuNDsH82U7gjxQpQ0vAYH_Lafuh-9tRPHBHRxtQFoyT5fGp21963UShnqRqBgOzTWwdMpwandE1SVrrYW3JCP0wdswuP-rIIBKwSSj8bdyprSkR4uEsadvN3Udr5N-v6M9p8ecf8AZ3kxbWBocuFHhO92i-hetmyjFuTEGxg-ee0QovN3rk_UUQH2fX7638edrZHh6wf6hUm6PZWVH7LQE-D6JId6IrkC_GeffjR_WShn')"}}></div>
          <div className="absolute inset-0 bg-gradient-to-t from-background via-background/40 to-transparent z-10"></div>
          <div className="relative z-20">
            <span className="text-secondary font-bold tracking-widest text-sm font-label mb-2 block uppercase">CURATOR</span>
            <h3 className="headline text-3xl font-bold mb-4">판매자</h3>
            <p className="text-on-surface-variant mb-6 font-body">당신의 소중한 컬렉션을 가치 있게 판매하세요.</p>
            <button 
              onClick={() => navigate('/login')}
              className="w-full bg-surface-container text-on-surface border border-outline-variant px-8 py-3 rounded-lg font-bold font-headline hover:bg-secondary hover:text-on-secondary transition-all"
            >
              등록하기
            </button>
          </div>
        </div>
        {/* Admin */}
        <div className="md:col-span-12 group relative overflow-hidden bg-surface-container rounded-xl p-8 flex flex-col md:flex-row items-center justify-between border border-outline-variant/10 hover:border-primary/30 transition-all">
          <div className="flex items-center gap-6 mb-6 md:mb-0">
            <div className="w-16 h-16 rounded-full bg-tertiary/20 flex items-center justify-center">
              <span className="material-symbols-outlined text-tertiary text-3xl">settings_input_component</span>
            </div>
            <div>
              <h3 className="headline text-2xl font-bold">시스템 관리자</h3>
              <p className="text-on-surface-variant font-body">전체 플랫폼 운영과 신뢰도를 관리합니다.</p>
            </div>
          </div>
          <button 
            onClick={() => navigate('/login')}
            className="bg-transparent text-tertiary border border-tertiary/30 px-10 py-3 rounded-lg font-bold font-headline hover:bg-tertiary hover:text-on-tertiary transition-all"
          >
            관리 도구 접속
          </button>
        </div>
      </div>
    </section>
  );
};

export default RoleSection;
