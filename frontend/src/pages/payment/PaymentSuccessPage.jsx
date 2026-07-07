import React from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { CheckCircle } from 'lucide-react';
import Header from '../../components/layout/Header';

const PaymentSuccessPage = () => {
  const [searchParams] = useSearchParams();
  const amount = searchParams.get('amount');
  const orderName = searchParams.get('orderName');
  const orderId = searchParams.get('orderId');
  const method = searchParams.get('method');

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <Header />

      <main className="flex-grow flex flex-col items-center justify-center px-6 py-16">
        <div className="w-full max-w-md bg-white rounded-[32px] border border-gray-200 shadow-sm p-10 flex flex-col items-center gap-6 text-center">

          <div className="w-20 h-20 rounded-full bg-emerald-50 flex items-center justify-center">
            <CheckCircle className="text-emerald-500" size={44} strokeWidth={2} />
          </div>

          <div className="flex flex-col gap-1">
            <h2 className="text-2xl font-extrabold text-gray-900">결제 완료!</h2>
            <p className="text-sm text-gray-400 font-semibold">공구 탑승에 성공했습니다.</p>
          </div>

          <div className="w-full bg-gray-50 rounded-2xl border border-gray-100 p-6 flex flex-col gap-3 text-left">
            <Row label="주문명" value={orderName} />
            <Row label="결제 금액" value={amount ? `${Number(amount).toLocaleString()}원` : undefined} />
            <Row label="결제 수단" value={method} />
            <Row label="주문 번호" value={orderId} small />
          </div>

          <Link
            to="/"
            className="w-full py-3.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-extrabold rounded-2xl transition text-center"
          >
            홈으로 돌아가기
          </Link>
        </div>
      </main>
    </div>
  );
};

const Row = ({ label, value, small }) => (
  <div className="flex justify-between items-start gap-4">
    <span className="text-xs font-bold text-gray-400 shrink-0">{label}</span>
    <span className={`font-extrabold text-gray-900 text-right break-all ${small ? 'text-xs' : 'text-sm'}`}>
      {value ?? '-'}
    </span>
  </div>
);

export default PaymentSuccessPage;
