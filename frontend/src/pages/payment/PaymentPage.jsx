import React from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { CreditCard, Image as ImageIcon, Users } from 'lucide-react';
import Header from '../../components/layout/Header';

//클라이언트키 추가
const TOSS_CLIENT_KEY = 'test_ck_a27c64c44aac6a79696abfe411bda51075f648cc09b41868a0169864ac7c4620';

const PaymentPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const product = location.state?.product;

  if (!product) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col">
        <Header />
        <main className="flex-grow flex flex-col items-center justify-center gap-6 text-center px-6">
          <p className="text-xl font-extrabold text-gray-700">결제할 상품 정보가 없습니다.</p>
          <Link to="/" className="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold rounded-2xl transition">
            홈으로 돌아가기
          </Link>
        </main>
      </div>
    );
  }

  const handlePayment = async () => {
    try {
      // 결제창을 열기 전에 서버가 실제 가격을 확인하고 PENDING 기록을 남긴다.
      const prepareRes = await fetch('http://localhost:8080/api/payment/prepare', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ productId: Number(product.id), buyerName: '유한대학교 학우님' }),
      });
      if (prepareRes.status === 401) {
        alert('로그인이 필요합니다.');
        navigate('/login');
        return;
      }
      if (!prepareRes.ok) {
        const err = await prepareRes.json().catch(() => ({}));
        throw new Error(err.message || '결제 준비에 실패했습니다.');
      }
      const { orderId, amount } = await prepareRes.json();

      const toss = window.TossPayments(TOSS_CLIENT_KEY);
      await toss.requestPayment('카드', {
        amount,
        orderId,
        orderName: product.title,
        successUrl: 'http://localhost:8080/api/payment/success',
        failUrl: 'http://localhost:8080/api/payment/fail',
      });
    } catch (err) {
      if (err.code === 'USER_CANCEL') return;
      alert(`결제 오류: ${err.message}`);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <Header />

      <div className="bg-gradient-to-r from-blue-600 to-indigo-600 py-16 px-6">
        <div className="max-w-7xl mx-auto flex flex-col gap-4">
          <span className="bg-white/20 text-white text-[10px] font-black tracking-widest px-3 py-1.5 rounded-full w-max uppercase">
            Yuhan University Joint Purchase
          </span>
          <h2 className="text-3xl md:text-4xl font-extrabold tracking-tight text-white mt-1">
            결제 <span className="text-yellow-400">진행</span>
          </h2>
          <p className="text-blue-100 text-sm md:text-base font-medium max-w-2xl leading-relaxed mt-1">
            아래 상품 정보를 확인하고 결제를 완료하세요.
          </p>
        </div>
      </div>

      <main className="flex-grow max-w-7xl w-full mx-auto p-6 md:p-8 flex flex-col items-center gap-6">

        <div className="w-full max-w-xl bg-white rounded-[32px] border border-gray-200 shadow-sm overflow-hidden">

          {/* 썸네일 */}
          <div className="w-full h-56 bg-gray-100 relative overflow-hidden flex items-center justify-center">
            {product.thumbnail ? (
              <img
                src={product.thumbnail}
                alt={product.title}
                className="absolute inset-0 w-full h-full object-cover"
              />
            ) : (
              <div className="flex flex-col items-center gap-2 text-gray-400">
                <ImageIcon size={36} />
                <span className="text-xs font-bold">이미지 없음</span>
              </div>
            )}
          </div>

          {/* 상품 정보 */}
          <div className="p-8 flex flex-col gap-6">
            <div className="flex flex-col gap-1">
              <span className="text-[11px] font-extrabold text-gray-400 uppercase tracking-widest">{product.major}</span>
              <h3 className="text-xl font-extrabold text-gray-900 leading-snug">{product.title}</h3>
              <span className="text-sm text-gray-400 font-semibold">{product.author}</span>
            </div>

            <div className="flex flex-col gap-4 pt-4 border-t border-gray-100">
              {/* 가격 */}
              <div className="flex items-end justify-between">
                <div className="flex flex-col">
                  {product.originalPrice && (
                    <span className="text-sm text-gray-400 line-through font-semibold">{Number(product.originalPrice).toLocaleString()}원</span>
                  )}
                  <span className="text-2xl font-black text-blue-600">{Number(product.price).toLocaleString()}원</span>
                </div>
                {product.dDay && (
                  <span className="text-xs font-black px-2.5 py-1 rounded-md bg-red-500 text-white">{product.dDay}</span>
                )}
              </div>

              {/* 인원 현황 */}
              {product.current != null && product.target != null && (
                <div className="flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5 text-sm font-bold text-gray-500">
                      <Users size={15} />
                      <span>참여 인원</span>
                    </div>
                    <span className="text-sm font-extrabold">
                      <span className="text-blue-600">{product.current}</span>
                      <span className="text-gray-400"> / {product.target}명</span>
                    </span>
                  </div>
                  <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-blue-500 rounded-full transition-all duration-500"
                      style={{ width: `${Math.min((product.current / product.target) * 100, 100)}%` }}
                    />
                  </div>
                  <span className="text-xs font-bold text-gray-400 text-right">
                    {product.target - product.current > 0
                      ? `${product.target - product.current}명 더 모이면 출발!`
                      : '모집 완료'}
                  </span>
                </div>
              )}
            </div>

            <button
              onClick={handlePayment}
              className="w-full flex items-center justify-center gap-3 py-4 bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white text-base font-extrabold rounded-2xl transition shadow-md shadow-blue-200"
            >
              <CreditCard size={20} />
              결제하기
            </button>

            <button
              onClick={() => navigate(-1)}
              className="w-full flex items-center justify-center pl-3 py-3 bg-white border border-gray-200 hover:bg-gray-50 text-gray-600 text-sm font-bold rounded-2xl transition"
            >
              돌아가기
            </button>
          </div>
        </div>

      </main>
    </div>
  );
};

export default PaymentPage;
