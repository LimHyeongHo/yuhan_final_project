import React, { useState, useEffect } from 'react';
import { Bookmark, ShoppingBag } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const MyPageScrap = () => {
  const [scrapList, setScrapList] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    fetch('http://localhost:8080/api/products/scraps/me', { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error('스크랩 목록을 불러올 수 없습니다.');
        return res.json();
      })
      .then(data => {
        const formattedScraps = data.map(scrap => ({
          id: scrap.product.productId,
          title: scrap.product.title,
          author: scrap.product.author || '저자 미상',
          currentCount: scrap.product.currentCount,
          targetCount: scrap.product.targetCount
        }));
        setScrapList(formattedScraps);
      })
      .catch(err => console.error(err));
  }, []);

  return (
    <div className="bg-white rounded-[32px] p-8 border border-gray-200 shadow-sm flex flex-col gap-6">
      <div className="flex items-center justify-between pb-6 border-b border-gray-100">
        <div className="flex items-center gap-3">
          <Bookmark className="text-blue-500" size={24} />
          <h4 className="text-lg font-bold text-gray-900">스크랩한 공구</h4>
        </div>
        <span className="text-sm font-bold text-gray-500">총 {scrapList.length}건</span>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {scrapList.map((item) => (
          <div key={item.id} className="p-5 rounded-2xl border border-gray-100 flex flex-col gap-4 hover:border-blue-300 transition">
            <div className="flex flex-col gap-1">
              <h5 className="text-sm font-extrabold text-gray-900 truncate">{item.title}</h5>
              <span className="text-xs text-gray-400 font-medium">{item.author}</span>
            </div>
            
            {/* 진행률 게이지 */}
            <div className="flex flex-col gap-1.5">
              <div className="flex justify-between text-[11px] font-bold">
                <span className="text-blue-600">모집 현황</span>
                <span className="text-gray-600">{item.currentCount} / {item.targetCount}명</span>
              </div>
              <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-blue-500 rounded-full" 
                  style={{ width: `${(item.currentCount / item.targetCount) * 100}%` }}
                ></div>
              </div>
            </div>

            <button 
              onClick={() => navigate(`/buyer/products/${item.id}`)}
              className="w-full py-2 bg-gray-50 hover:bg-blue-50 hover:text-blue-600 text-gray-600 text-xs font-bold rounded-xl transition flex items-center justify-center gap-2 mt-2"
            >
              <ShoppingBag size={14} /> 상세 보기
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default MyPageScrap;