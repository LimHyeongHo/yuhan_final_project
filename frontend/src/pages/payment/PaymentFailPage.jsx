import React from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { XCircle } from 'lucide-react';
import V3SiteHeader from '../../components/layout/V3SiteHeader';

const PAYMENT_ERROR_MESSAGES = {
  PAYMENT_INVALID_AMOUNT: '결제 금액이 올바르지 않습니다.',
  PAYMENT_ORDER_NOT_FOUND: '유효하지 않은 주문입니다.',
  PAYMENT_AMOUNT_MISMATCH: '결제 금액이 일치하지 않습니다.',
  PAYMENT_CANCELLED: '결제가 취소되었습니다.',
  PAYMENT_CONFIRM_FAILED: '결제 승인에 실패했습니다. 잠시 후 다시 시도해주세요.',
  PAYMENT_REFUND_FAILED: '환불 처리에 실패했습니다. 잠시 후 다시 시도해주세요.',
  PAYMENT_JOIN_FAILED: '결제는 승인됐지만 참여 확정에 실패해 자동 환불되었습니다. 다시 시도해주세요.',
  PAYMENT_CANCEL_IN_PROGRESS: '환불이 이미 진행 중입니다. 잠시 후 상태를 확인해주세요.',
  PAYMENT_LOOKUP_FAILED: '결제 상태를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.',
};

const PaymentFailPage = () => {
  const [searchParams] = useSearchParams();
  const requestedCode = searchParams.get('code');
  const code = Object.prototype.hasOwnProperty.call(PAYMENT_ERROR_MESSAGES, requestedCode)
    ? requestedCode
    : 'PAYMENT_CONFIRM_FAILED';
  const message = PAYMENT_ERROR_MESSAGES[code];

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <V3SiteHeader />

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
              <span className="text-sm font-extrabold text-red-600 text-right">{message}</span>
            </div>
            <div className="flex justify-between items-start gap-4">
              <span className="text-xs font-bold text-gray-400 shrink-0">오류 코드</span>
              <span className="text-xs font-bold text-gray-500 text-right">{code}</span>
            </div>
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
