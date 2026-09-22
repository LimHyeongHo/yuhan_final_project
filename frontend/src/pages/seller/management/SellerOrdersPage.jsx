import React, { useState, useEffect } from 'react';
import { Package, Users, Calendar, ArrowRight, User, ShoppingCart, Loader } from 'lucide-react';
import V3SiteHeader from '../../../components/layout/V3SiteHeader';

const SellerOrdersPage = () => {
  const [orders, setOrders] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetch('http://localhost:8080/api/products/seller/orders', {
      method: 'GET',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
    })
      .then(res => {
        if (!res.ok) throw new Error('주문 내역을 불러오는데 실패했습니다.');
        return res.json();
      })
      .then(data => {
        setOrders(data);
        setIsLoading(false);
      })
      .catch(err => {
        console.error(err);
        setIsLoading(false);
      });
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col text-gray-900">
      <V3SiteHeader />
      <div className="flex-grow max-w-7xl w-full mx-auto p-6 flex flex-col gap-8">
        
        <header className="bg-white rounded-3xl p-8 shadow-sm flex flex-col items-start gap-4 border border-gray-100">
          <div className="flex flex-col md:flex-row md:items-center gap-4">
            <h1 className="text-3xl font-extrabold text-gray-900 tracking-tight flex items-center gap-3 whitespace-nowrap">
              <Users className="text-blue-600" size={32} />
              주문/구매자 관리
            </h1>
            <div className="bg-blue-50 px-4 py-3 rounded-2xl flex items-center gap-3 whitespace-nowrap">
              <ShoppingCart className="text-blue-700" size={24} />
              <p className="text-sm text-blue-700 font-semibold">
                총 결제자 수 <strong className="ml-1 text-xl font-black text-gray-900">{orders.length}명</strong>
              </p>
            </div>
          </div>
          <p className="text-gray-500 font-medium">내 상품을 결제한 사람들의 명단을 확인하고 전달 상태를 체크해보세요.</p>
        </header>

        {isLoading ? (
          <div className="flex justify-center items-center py-20">
            <Loader className="animate-spin text-blue-600" size={48} />
          </div>
        ) : orders.length === 0 ? (
          <div className="bg-white rounded-3xl p-16 shadow-sm border border-gray-100 text-center flex flex-col items-center gap-4">
            <Package size={64} className="text-gray-200" />
            <p className="text-gray-500 font-medium text-lg">아직 결제된 주문 내역이 없습니다.</p>
          </div>
        ) : (
          <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-center border-collapse">
                <thead>
                  <tr className="bg-gray-50 text-gray-500 text-sm font-semibold uppercase tracking-wider border-b border-gray-200">
                    <th className="px-6 py-5 text-center">상품명 (Product)</th>
                    <th className="px-6 py-5 text-center">닉네임</th>
                    <th className="px-6 py-5 text-center">결제 금액</th>
                    <th className="px-6 py-5 text-center">공동구매 상태</th>
                    <th className="px-6 py-5 text-center">결제 일시</th>
                    <th className="px-6 py-5 text-center">전달 확인</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {orders.map((order, idx) => (
                    <tr key={order.participationId || idx} className="hover:bg-blue-50/30 transition-colors">
                      <td className="px-6 py-5">
                        <p className="font-bold text-gray-900 truncate max-w-[250px] mx-auto">{order.productTitle}</p>
                      </td>
                      <td className="px-6 py-5">
                        <div className="flex items-center justify-center gap-2">
                          <div className="bg-gray-100 p-1.5 rounded-full"><User size={14} className="text-gray-500"/></div>
                          <span className="font-medium text-gray-700">{order.buyerNickname}</span>
                        </div>
                      </td>
                      <td className="px-6 py-5">
                        <span className="font-bold text-gray-900">{order.productPrice?.toLocaleString()}원</span>
                      </td>
                      <td className="px-6 py-5">
                        <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                          order.productStatus === 'SUCCESS' ? 'bg-green-100 text-green-700' :
                          order.productStatus === 'FAIL' ? 'bg-red-100 text-red-700' :
                          'bg-blue-100 text-blue-700'
                        }`}>
                          {order.productStatus === 'OPEN' ? '진행중' : order.productStatus === 'SUCCESS' ? '성사완료' : '실패(환불)'}
                        </span>
                      </td>
                      <td className="px-6 py-5">
                        <div className="flex items-center justify-center gap-2 text-gray-500 text-sm">
                          <Calendar size={14} />
                          {order.joinDate ? order.joinDate.split('T')[0] : '날짜 없음'}
                        </div>
                      </td>
                      <td className="px-6 py-5 text-center">
                        <button 
                          className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-sm font-bold rounded-xl transition"
                          onClick={() => alert('실제 배송/전달 처리를 위한 기능은 추후 확장 가능합니다.')}
                        >
                          전달 완료
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default SellerOrdersPage;
