import React from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { XCircle } from 'lucide-react';
import Header from '../../components/layout/Header';

const PaymentFailPage = () => {
  const [searchParams] = useSearchParams();
  const message = searchParams.get('message');
  const code = searchParams.get('code');

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <Header />

      <main className="flex-grow flex flex-col items-center justify-center px-6 py-16">
        <div className="w-full max-w-md bg-white rounded-[32px] border border-gray-200 shadow-sm p-10 flex flex-col items-center gap-6 text-center">

          <div className="w-20 h-20 rounded-full bg-red-50 flex items-center justify-center">
            <XCircle className="text-red-500" size={44} strokeWidth={2} />
          </div>

          <div className="flex flex-col gap-1">
            <h2 className="text-2xl font-extrabold text-gray-900">결제 실패</h2>
            <p className="text-sm text-gray-400 font-semibold">결제 중 문제가 발생했습니다.</p>
          </div>

          <div className="w-full bg-gray-50 rounded-2xl border border-gray-100 p-6 flex flex-col gap-3 text-left">
            <div className="flex justify-between items-start gap-4">
              <span className="text-xs font-bold text-gray-400 shrink-0">오류 메시지</span>
              <span className="text-sm font-extrabold text-red-600 text-right">{message ?? '-'}</span>
            </div>
            {code && (
              <div className="flex justify-between items-start gap-4">
                <span className="text-xs font-bold text-gray-400 shrink-0">오류 코드</span>
                <span className="text-xs font-bold text-gray-500 text-right">{code}</span>
              </div>
            )}
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

export default PaymentFailPage;
