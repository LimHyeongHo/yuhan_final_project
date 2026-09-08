// [UI-RQ-006][feature/ui-fixes] 이용 가이드 페이지 — 구매자/판매자 핵심 흐름 안내
import React from 'react';
import { Link } from 'react-router-dom';
import { UserPlus, ShieldCheck, Users, CreditCard, Store, ArrowRight } from 'lucide-react';
import Header from '../../components/layout/Header';

const buyerSteps = [
  { icon: UserPlus, title: '1. 회원가입', desc: '이메일과 비밀번호로 가입하고 구매자 계정을 만듭니다.' },
  { icon: ShieldCheck, title: '2. 본인인증 및 기기 인증서 발급', desc: '로그인 시 휴대폰 본인인증을 진행하고, 기기 인증서를 발급받으면 안전 로그인을 사용할 수 있습니다.' },
  { icon: Users, title: '3. 공동구매 참여', desc: '원하는 전공책 공동구매를 찾아 "공동구매 참여하기(N빵 탑승)" 버튼으로 참여합니다.' },
  { icon: CreditCard, title: '4. 결제', desc: '참여 후 안내에 따라 결제를 완료하면 공동구매 확정 대상에 포함됩니다.' },
];

const sellerSteps = [
  { icon: UserPlus, title: '1. 회원가입 및 판매자 신청', desc: '회원가입 후 판매자 권한을 신청하고 승인을 받습니다.' },
  { icon: Store, title: '2. 상품(공동구매) 등록', desc: '판매자 대시보드 → 물품 등록에서 도서/학과물품 정보와 목표 인원, 가격을 입력해 공동구매를 개설합니다.' },
  { icon: Users, title: '3. 참여 현황 관리', desc: '판매 현황에서 참여 인원과 진행 상태를 확인하고, 구매자 문의는 채팅으로 응대합니다.' },
  { icon: CreditCard, title: '4. 정산', desc: '공동구매가 성사되면 마이페이지 정산 관리에서 계좌 등록 후 출금을 신청합니다.' },
];

const StepCard = ({ icon: Icon, title, desc }) => (
  <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-5 flex gap-4">
    <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
      <Icon size={20} />
    </div>
    <div className="flex flex-col gap-1">
      <h4 className="text-sm font-extrabold text-gray-900">{title}</h4>
      <p className="text-xs text-gray-500 leading-relaxed">{desc}</p>
    </div>
  </div>
);

const GuidePage = () => {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <Header />

      <main className="flex-grow max-w-4xl w-full mx-auto p-6 md:p-8 flex flex-col gap-10">
        <div className="flex flex-col gap-2 text-center pt-4">
          <span className="text-xs font-black text-blue-600 uppercase tracking-widest">Getting Started</span>
          <h1 className="text-2xl md:text-3xl font-extrabold text-gray-900">YU-BOOK 이용 가이드</h1>
          <p className="text-sm text-gray-500">전공책 공동구매, 이렇게 이용하세요.</p>
        </div>

        <section className="flex flex-col gap-4">
          <h2 className="text-lg font-extrabold text-gray-900 border-b border-gray-100 pb-3">구매자 이용 흐름</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {buyerSteps.map((step) => (
              <StepCard key={step.title} {...step} />
            ))}
          </div>
          <Link to="/buyer/products" className="self-start flex items-center gap-1 text-sm font-bold text-blue-600 hover:underline mt-1">
            공구 찾으러 가기 <ArrowRight size={14} />
          </Link>
        </section>

        <section className="flex flex-col gap-4">
          <h2 className="text-lg font-extrabold text-gray-900 border-b border-gray-100 pb-3">판매자 이용 흐름</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {sellerSteps.map((step) => (
              <StepCard key={step.title} {...step} />
            ))}
          </div>
          <Link to="/seller/products" className="self-start flex items-center gap-1 text-sm font-bold text-blue-600 hover:underline mt-1">
            상품 등록하러 가기 <ArrowRight size={14} />
          </Link>
        </section>
      </main>
    </div>
  );
};

export default GuidePage;
