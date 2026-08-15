import React, { useState, useEffect } from 'react';
import { Package, ChevronRight, Clock } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const MyPageOrders = () => {
  const navigate = useNavigate();
  const [orderList, setOrderList] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch('http://localhost:8080/api/products/participations/me', { credentials: 'include' })
      .then(res => {
        if (res.status === 401) {
          throw new Error('로그인이 필요합니다.');
        }
        if (!res.ok) throw new Error('주문 내역을 불러오는데 실패했습니다.');
        return res.json();
      })
      .then(data => {
        // data는 Participation 객체 배열
        const formatted = data.map(part => {
          const product = part.product || {};
          let statusLabel = '진행중';
          let colorClass = 'text-blue-700 bg-blue-100 border-blue-200';
          
          if (product.status === 'CLOSED_SUCCESS') {
            statusLabel = '모집 성공(정산 중)';
            colorClass = 'text-emerald-700 bg-emerald-100 border-emerald-200';
          } else if (product.status === 'CLOSED_FAIL') {
            statusLabel = '모집 실패(환불)';
            colorClass = 'text-red-700 bg-red-100 border-red-200';
          } else if (product.status === 'OPEN' && product.currentCount >= product.targetCount) {
            statusLabel = '목표 달성';
            colorClass = 'text-emerald-700 bg-emerald-100 border-emerald-200';
          }

          return {
            id: part.id,
            productId: product.id,
            title: product.title || '알 수 없는 상품',
            date: new Date(part.joinDate).toLocaleDateString('ko-KR'),
            status: statusLabel,
            price: (product.price || 0).toLocaleString() + '원',
            color: colorClass
          };
        });
        setOrderList(formatted);
      })
      .catch(err => {
        console.error(err);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return (
    <div className="bg-white rounded-[32px] p-8 border border-gray-200 shadow-sm flex flex-col gap-6">
      <div className="flex items-center gap-3 pb-6 border-b border-gray-100">
        <Package className="text-blue-500" size={24} />
        <h4 className="text-lg font-bold text-gray-900">최근 참여 내역</h4>
      </div>

      <div className="flex flex-col gap-4">
        {loading ? (
          <div className="text-center text-gray-500 py-8 text-sm font-bold">참여 내역을 불러오는 중입니다...</div>
        ) : orderList.length === 0 ? (
          <div className="text-center text-gray-400 py-8 text-sm font-bold">아직 참여한 공동구매 내역이 없습니다.</div>
        ) : (
          orderList.map((order) => (
            <div 
              key={order.id} 
              onClick={() => navigate(`/buyer/products/${order.productId}`)}
              className="flex items-center justify-between p-5 rounded-2xl border border-gray-100 hover:border-blue-200 hover:shadow-sm transition cursor-pointer group"
            >
            <div className="flex flex-col gap-2">
              <span className={`text-[11px] font-black w-max px-2.5 py-1 rounded-md ${order.color}`}>
                {order.status}
              </span>
              <h5 className="text-base font-extrabold text-gray-900 group-hover:text-blue-600 transition">{order.title}</h5>
              <div className="flex items-center gap-2 text-xs font-medium text-gray-400">
                <Clock size={12} />
                <span>결제일: {order.date}</span>
                <span className="text-gray-300">|</span>
                <span className="text-gray-700 font-bold">{order.price}</span>
              </div>
            </div>
            <ChevronRight className="text-gray-300 group-hover:text-blue-500 transition" size={20} />
          </div>
        )))}
      </div>
    </div>
  );
};

export default MyPageOrders;